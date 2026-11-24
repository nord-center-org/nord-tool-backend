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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
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
 * Desde a v3 também passa o "sinal" do WebRTC (oferta, resposta e candidatos), para a imagem ao vivo ir direto da
 * câmera para o painel, e a câmera pode trocar o próprio nome e dizer o tipo (app ou câmera ao vivo).
 * Tudo vive em memória: num reinício, a câmera e o painel se reconectam sozinhos.
 */
@Slf4j
@Service
public class DrumcamServiceImpl implements DrumcamService {

    static final String CAMERA = "camera";
    static final String CONTROLE = "controle";
    /** Maior mensagem aceita (a prévia JPEG em base64 com folga). */
    static final int MAX_MENSAGEM = 1_400_000;
    static final int MAX_POR_SEGUNDO = 120;
    static final int MAX_CAMERAS_POR_PESSOA = 9;
    /** Maior "sinal" do WebRTC (uma oferta SDP tem poucos KB). */
    static final int MAX_SINAL = 64_000;
    static final String TODOS = "*";
    static final int MAX_CONTROLES_POR_PESSOA = 10;
    static final int MAX_BILHETES = 2_000;
    static final Duration VALIDADE_BILHETE = Duration.ofSeconds(60);
    /**
     * Passe de retomada da câmera: vale 24 horas e chega novo a cada conexão. Com ele a câmera volta ao relé depois de
     * um deploy (o servidor reinicia e derruba todas as conexões) mesmo se a sessão de login do aparelho já venceu.
     */
    static final Duration VALIDADE_RETOMADA = Duration.ofHours(24);
    static final String PREFIXO_RETOMADA = "r1.";
    private static final SecureRandom ALEATORIO_CHAVE = new SecureRandom();
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
        private String nome;
        private String tipo = "app";
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
    private final byte[] chaveRetomada;

    @Autowired
    public DrumcamServiceImpl(AutorizacaoService autorizacao, ObjectMapper mapper, Clock clock,
                              @Value("${nord-tool.security.jwt-keys:}") String jwtKeys,
                              @Value("${nord-tool.security.jwt-secret:}") String jwtSecret) {
        this(autorizacao, mapper, clock, new SecureRandom(), chave(jwtKeys, jwtSecret));
    }

    DrumcamServiceImpl(AutorizacaoService autorizacao, ObjectMapper mapper, Clock clock, SecureRandom aleatorio, byte[] chaveRetomada) {
        this.autorizacao = autorizacao;
        this.mapper = mapper;
        this.clock = clock;
        this.aleatorio = aleatorio;
        this.chaveRetomada = chaveRetomada.clone();
    }

    /**
     * Chave do passe de retomada, derivada das chaves JWT do ambiente (sobrevive a deploys). Sem chave JWT (só no perfil
     * local), uma aleatória: o passe vale até reiniciar.
     */
    static byte[] chave(String jwtKeys, String jwtSecret) {
        String base = (jwtKeys == null ? "" : jwtKeys) + "|" + (jwtSecret == null ? "" : jwtSecret);
        byte[] b;
        if (base.length() > 1) {
            b = base.getBytes(StandardCharsets.UTF_8);
        } else {
            b = new byte[32];
            ALEATORIO_CHAVE.nextBytes(b);
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(b, "HmacSHA256"));
            return mac.doFinal("nordtool-drumcam-retomada".getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Sem HMAC-SHA256 na JVM", e);
        }
    }

    private byte[] assinar(String corpo) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(chaveRetomada, "HmacSHA256"));
            return mac.doFinal(corpo.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Sem HMAC-SHA256 na JVM", e);
        }
    }

    /** Passe de retomada (só para câmeras): "r1.usuario.validade.nonce.assinatura". */
    String passeDeRetomada(Long idUsuario) {
        byte[] nonce = new byte[12];
        aleatorio.nextBytes(nonce);
        Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
        String corpo = PREFIXO_RETOMADA + idUsuario + "." + clock.instant().plus(VALIDADE_RETOMADA).toEpochMilli() + "." + b64.encodeToString(nonce);
        return corpo + "." + b64.encodeToString(assinar(corpo));
    }

    /** Dono do passe, se a assinatura confere e ainda vale; nulo se não. */
    Long donoDaRetomada(String passe) {
        int ponto = passe.lastIndexOf('.');
        if (ponto <= PREFIXO_RETOMADA.length()) return null;
        String corpo = passe.substring(0, ponto);
        byte[] assinatura;
        try {
            assinatura = Base64.getUrlDecoder().decode(passe.substring(ponto + 1));
        } catch (IllegalArgumentException e) {
            return null;
        }
        if (!MessageDigest.isEqual(assinar(corpo), assinatura)) return null;
        String[] partes = corpo.substring(PREFIXO_RETOMADA.length()).split("\\.");
        if (partes.length != 3) return null;
        try {
            long validade = Long.parseLong(partes[1]);
            return clock.instant().toEpochMilli() > validade ? null : Long.valueOf(partes[0]);
        } catch (NumberFormatException e) {
            return null;
        }
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
        Bilhete b;
        if (bilhete != null && bilhete.startsWith(PREFIXO_RETOMADA)) {
            // Câmera voltando depois de o servidor reiniciar: o passe assinado vale no lugar do bilhete.
            Long dono = donoDaRetomada(bilhete);
            b = dono == null ? null : new Bilhete(dono, CAMERA, "Câmera", clock.instant().plus(VALIDADE_BILHETE));
        } else {
            b = bilhete == null ? null : bilhetes.remove(bilhete);
        }
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
        if (CAMERA.equals(b.papel)) ok.put("retomar", passeDeRetomada(b.idUsuario));
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
        else if ("sinal".equals(tipo) && mensagem.length() <= MAX_SINAL) repassarSinal(c, (ObjectNode) m);
        else if (CAMERA.equals(c.papel) && "info".equals(tipo)) atualizarInfo(c, m);
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

    /**
     * Sinal do WebRTC. Controle → câmera ({@code camera}) e câmera → controle ({@code para}, ou "*" para todos os
     * painéis da pessoa). Sempre só entre conexões da mesma pessoa; quem recebe vê de onde veio.
     */
    private void repassarSinal(Conexao de, ObjectNode m) {
        if (CONTROLE.equals(de.papel)) {
            Conexao camera = conexoes.get(m.path(CAMERA).asText(""));
            if (camera == null || !CAMERA.equals(camera.papel) || !camera.idUsuario.equals(de.idUsuario)) return;
            m.remove(CAMERA);
            m.put("de", de.canal.id());
            camera.canal.enviar(m.toString());
            return;
        }
        String para = m.path("para").asText("");
        m.remove("para");
        m.put(CAMERA, de.canal.id());
        String texto = m.toString();
        for (Conexao c : new ArrayList<>(conexoes.values())) {
            boolean alvo = TODOS.equals(para) || c.canal.id().equals(para);
            if (alvo && CONTROLE.equals(c.papel) && c.idUsuario.equals(de.idUsuario)) c.canal.enviar(texto);
        }
    }

    /** A câmera troca o nome (renomear pelo painel) e diz o tipo; os painéis recebem a lista nova. */
    private void atualizarInfo(Conexao camera, JsonNode m) {
        if (m.hasNonNull("nome")) camera.nome = nomeLimpo(m.path("nome").asText(""));
        String tipo = m.path("tipo").asText("");
        if ("app".equals(tipo) || "aovivo".equals(tipo)) camera.tipo = tipo;
        avisarCameras(camera.idUsuario);
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
        for (Conexao c : minhas) lista.addObject().put(ID, c.canal.id()).put("nome", c.nome).put("tipo", c.tipo);
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
