package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.dto.DrumcamBilheteDto;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.service.DrumcamService;
import br.com.nord_tool_backend.websocket.CanalSala;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Relé do Drumcam pela internet. O celular-câmera (app NordTool em modo câmera) e o painel de controle (Repertório →
 * Drumcam) abrem o mesmo WebSocket com um bilhete de uso único. O servidor não entende o conteúdo: repassa os pedidos
 * de um controle para uma câmera da mesma pessoa e as respostas de volta para quem pediu. Os vídeos não passam por
 * aqui (vão direto da câmera para o YouTube); só os comandos, o estado, a prévia e as miniaturas.
 * Tudo vive em memória: num reinício, a câmera e o painel se reconectam sozinhos.
 */
@Slf4j
@Service
public class DrumcamServiceImpl implements DrumcamService {

    static final String CAMERA = "camera";
    static final String CONTROLE = "controle";
    /** Maior mensagem aceita (a prévia JPEG em base64 com folga). */
    static final int MAX_MENSAGEM = 1_400_000;
    static final int MAX_POR_SEGUNDO = 60;
    static final int MAX_CAMERAS_POR_PESSOA = 5;
    static final int MAX_CONTROLES_POR_PESSOA = 10;
    static final int MAX_BILHETES = 2_000;
    static final Duration VALIDADE_BILHETE = Duration.ofSeconds(60);
    private static final String TIPO = "t";
    private static final String ID = "id";

    private static final class Bilhete {
        private final Long idUsuario;
        private final String papel;
        private final String nome;
        private final Instant expiraEm;

        private Bilhete(Long idUsuario, String papel, String nome, Instant expiraEm) {
            this.idUsuario = idUsuario;
            this.papel = papel;
            this.nome = nome;
            this.expiraEm = expiraEm;
        }
    }

    private static final class Conexao {
        private final CanalSala canal;
        private final Long idUsuario;
        private final String papel;
        private final String nome;
        private long janela;
        private int mensagens;

        private Conexao(CanalSala canal, Long idUsuario, String papel, String nome) {
            this.canal = canal;
            this.idUsuario = idUsuario;
            this.papel = papel;
            this.nome = nome;
        }
    }

    private final AutorizacaoService autorizacao;
    private final ObjectMapper mapper;
    private final Clock clock;
    private final SecureRandom aleatorio;
    private final Map<String, Bilhete> bilhetes = new HashMap<>();
    private final Map<String, Conexao> conexoes = new HashMap<>();

    @Autowired
    public DrumcamServiceImpl(AutorizacaoService autorizacao, ObjectMapper mapper, Clock clock) {
        this(autorizacao, mapper, clock, new SecureRandom());
    }

    DrumcamServiceImpl(AutorizacaoService autorizacao, ObjectMapper mapper, Clock clock, SecureRandom aleatorio) {
        this.autorizacao = autorizacao;
        this.mapper = mapper;
        this.clock = clock;
        this.aleatorio = aleatorio;
    }

    @Override
    public synchronized DrumcamBilheteDto bilhete(String papel, String nome) {
        if (!CAMERA.equals(papel) && !CONTROLE.equals(papel)) throw new EntradaInvalidaException("Papel inválido.");
        Long eu = autorizacao.exigir(Modulo.REPERTORIO, Acao.LEITURA).getId();
        limpar();
        if (bilhetes.size() >= MAX_BILHETES) throw new EntradaInvalidaException("Muitas conexões abrindo agora. Tente de novo em instantes.");
        String limpo = nomeLimpo(nome);
        byte[] bytes = new byte[24];
        aleatorio.nextBytes(bytes);
        String codigo = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        bilhetes.put(codigo, new Bilhete(eu, papel, limpo, clock.instant().plus(VALIDADE_BILHETE)));
        return new DrumcamBilheteDto(codigo, papel);
    }

    @Override
    public synchronized void conectar(String bilhete, CanalSala canal) {
        Bilhete b = bilhete == null ? null : bilhetes.remove(bilhete);
        if (b == null || clock.instant().isAfter(b.expiraEm)) {
            canal.fechar("Bilhete inválido ou vencido.");
            return;
        }
        long iguais = conexoes.values().stream().filter(c -> c.idUsuario.equals(b.idUsuario) && c.papel.equals(b.papel)).count();
        int limite = CAMERA.equals(b.papel) ? MAX_CAMERAS_POR_PESSOA : MAX_CONTROLES_POR_PESSOA;
        if (iguais >= limite) {
            canal.fechar(CAMERA.equals(b.papel) ? "Limite de câmeras conectadas." : "Limite de painéis abertos.");
            return;
        }
        Conexao nova = new Conexao(canal, b.idUsuario, b.papel, b.nome);
        conexoes.put(canal.id(), nova);
        ObjectNode ok = mapper.createObjectNode().put(TIPO, "ok").put("papel", b.papel).put(ID, canal.id());
        canal.enviar(ok.toString());
        if (CAMERA.equals(b.papel)) avisarCameras(b.idUsuario);
        else canal.enviar(listaDeCameras(b.idUsuario).toString());
    }

    @Override
    public synchronized void receber(CanalSala canal, String mensagem) {
        Conexao c = conexoes.get(canal.id());
        if (c == null) return;
        if (mensagem == null || mensagem.length() > MAX_MENSAGEM) {
            canal.fechar("Mensagem grande demais.");
            return;
        }
        if (!dentroDoLimite(c)) return;
        JsonNode m;
        try {
            m = mapper.readTree(mensagem);
        } catch (JsonProcessingException e) {
            return;
        }
        if (m == null || !m.isObject()) return;
        String tipo = m.path(TIPO).asText("");
        if (CONTROLE.equals(c.papel) && "req".equals(tipo)) repassarPedido(c, (ObjectNode) m);
        else if (CAMERA.equals(c.papel) && "resp".equals(tipo)) repassarResposta(c, (ObjectNode) m);
        // "ping" e o resto: só mantêm a conexão viva.
    }

    /** Controle → câmera: a câmera tem de ser da mesma pessoa; se sumiu, o controle recebe o erro na hora. */
    private void repassarPedido(Conexao de, ObjectNode m) {
        String idCamera = m.path(CAMERA).asText("");
        Conexao camera = conexoes.get(idCamera);
        if (camera == null || !CAMERA.equals(camera.papel) || !camera.idUsuario.equals(de.idUsuario)) {
            ObjectNode erro = mapper.createObjectNode().put(TIPO, "resp").put("s", 503)
                .put("erro", "A câmera não está conectada.");
            erro.set(ID, m.path(ID));
            de.canal.enviar(erro.toString());
            return;
        }
        m.remove(CAMERA);
        m.put("de", de.canal.id());
        camera.canal.enviar(m.toString());
    }

    /** Câmera → controle: só para um controle da mesma pessoa. */
    private void repassarResposta(Conexao camera, ObjectNode m) {
        Conexao para = conexoes.get(m.path("para").asText(""));
        if (para == null || !CONTROLE.equals(para.papel) || !para.idUsuario.equals(camera.idUsuario)) return;
        m.remove("para");
        m.put(CAMERA, camera.canal.id());
        para.canal.enviar(m.toString());
    }

    @Override
    public synchronized void desconectar(CanalSala canal) {
        Conexao c = conexoes.remove(canal.id());
        if (c != null && CAMERA.equals(c.papel)) avisarCameras(c.idUsuario);
    }

    @Override
    @Scheduled(fixedDelay = 60_000L)
    public synchronized void limpar() {
        Instant agora = clock.instant();
        bilhetes.values().removeIf(b -> agora.isAfter(b.expiraEm));
    }

    private void avisarCameras(Long idUsuario) {
        String lista = listaDeCameras(idUsuario).toString();
        for (Conexao c : new ArrayList<>(conexoes.values())) {
            if (c.idUsuario.equals(idUsuario) && CONTROLE.equals(c.papel)) c.canal.enviar(lista);
        }
    }

    private ObjectNode listaDeCameras(Long idUsuario) {
        ObjectNode msg = mapper.createObjectNode().put(TIPO, "cameras");
        ArrayNode lista = msg.putArray("lista");
        List<Conexao> minhas = new ArrayList<>();
        for (Conexao c : conexoes.values()) if (c.idUsuario.equals(idUsuario) && CAMERA.equals(c.papel)) minhas.add(c);
        minhas.sort((a, b) -> a.nome.compareToIgnoreCase(b.nome));
        for (Conexao c : minhas) lista.addObject().put(ID, c.canal.id()).put("nome", c.nome);
        return msg;
    }

    private boolean dentroDoLimite(Conexao c) {
        long segundo = clock.millis() / 1000;
        if (segundo != c.janela) {
            c.janela = segundo;
            c.mensagens = 0;
        }
        return ++c.mensagens <= MAX_POR_SEGUNDO;
    }

    private static String nomeLimpo(String nome) {
        String n = nome == null ? "" : nome.replaceAll("[\\p{Cntrl}<>]", "").trim();
        if (n.isEmpty()) return "Drumcam";
        return n.length() > 60 ? n.substring(0, 60) : n;
    }
}
