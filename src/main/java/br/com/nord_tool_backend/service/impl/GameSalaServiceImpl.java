package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.dto.AvatarDto;
import br.com.nord_tool_backend.dto.GameSalaDto;
import br.com.nord_tool_backend.exception.ConflitoException;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.exception.NaoEncontradoException;
import br.com.nord_tool_backend.repository.AvatarRepository;
import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.service.GameSalaService;
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
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Salas do multiplayer dos Games: a tela grande hospeda (vaga 0) e roda o jogo; até 7 celulares entram pelo código
 * e viram controles. O servidor só repassa mensagens: entradas dos celulares para quem hospeda e avisos de quem
 * hospeda para os celulares. As salas vivem em memória (somem num reinício), e cada conexão abre com um bilhete de
 * uso único emitido para quem está logado, porque o navegador não manda o token no WebSocket.
 */
@Slf4j
@Service
public class GameSalaServiceImpl implements GameSalaService {

    static final int MAX_VAGAS = 8;
    static final int MAX_SALAS = 500;
    static final int MAX_MENSAGEM = 2048;
    static final int MAX_POR_SEGUNDO = 90;
    static final Duration VALIDADE_BILHETE = Duration.ofSeconds(60);
    static final Duration SALA_OCIOSA = Duration.ofMinutes(10);
    static final Duration SALA_MAXIMA = Duration.ofHours(6);
    static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    static final int TAMANHO_CODIGO = 4;
    private static final Pattern JOGO = Pattern.compile("^[a-z0-9-]{1,40}$");
    private static final Pattern CODIGO = Pattern.compile("^[A-Z0-9]{4}$");
    private static final String TIPO = "t";
    private static final String DADOS = "d";
    private static final String VAGA = "vaga";

    private static final class Vaga {
        private final Long idUsuario;
        private final String nome;
        private final String avatar;
        private CanalSala canal;
        private long janela;
        private int mensagens;

        private Vaga(Long idUsuario, String nome, String avatar) {
            this.idUsuario = idUsuario;
            this.nome = nome;
            this.avatar = avatar;
        }
    }

    private static final class Sala {
        private final String codigo;
        private final String jogo;
        private final Instant criadaEm;
        private final Map<Integer, Vaga> vagas = new TreeMap<>();
        private Instant ultimaAtividade;

        private Sala(String codigo, String jogo, Instant agora) {
            this.codigo = codigo;
            this.jogo = jogo;
            this.criadaEm = agora;
            this.ultimaAtividade = agora;
        }

        private Vaga anfitriao() {
            return vagas.get(0);
        }
    }

    private static final class Bilhete {
        private final String codigo;
        private final int vaga;
        private final Instant expiraEm;

        private Bilhete(String codigo, int vaga, Instant expiraEm) {
            this.codigo = codigo;
            this.vaga = vaga;
            this.expiraEm = expiraEm;
        }
    }

    private static final class Ligacao {
        private final String codigo;
        private final int vaga;

        private Ligacao(String codigo, int vaga) {
            this.codigo = codigo;
            this.vaga = vaga;
        }
    }

    private final Map<String, Sala> salas = new HashMap<>();
    private final Map<String, Bilhete> bilhetes = new HashMap<>();
    private final Map<String, Ligacao> ligacoes = new HashMap<>();
    private final AutorizacaoService autorizacao;
    private final AvatarRepository avatares;
    private final ObjectMapper mapper;
    private final Clock clock;
    private final SecureRandom aleatorio;

    @Autowired
    public GameSalaServiceImpl(AutorizacaoService autorizacao, AvatarRepository avatares, ObjectMapper mapper, Clock clock) {
        this(autorizacao, avatares, mapper, clock, new SecureRandom());
    }

    GameSalaServiceImpl(AutorizacaoService autorizacao, AvatarRepository avatares, ObjectMapper mapper, Clock clock, SecureRandom aleatorio) {
        this.autorizacao = autorizacao;
        this.avatares = avatares;
        this.mapper = mapper;
        this.clock = clock;
        this.aleatorio = aleatorio;
    }

    @Override
    public synchronized GameSalaDto criar(String jogo) {
        Long eu = autorizacao.exigir(Modulo.GAMES, Acao.LEITURA).getId();
        if (jogo == null || !JOGO.matcher(jogo).matches()) throw new EntradaInvalidaException("Jogo inválido");
        // Uma sala hospedada por pessoa: abrir outra fecha a anterior.
        new ArrayList<>(salas.values()).stream()
                .filter(s -> s.anfitriao() != null && eu.equals(s.anfitriao().idUsuario))
                .forEach(s -> fecharSala(s, "Quem hospeda abriu outra sala"));
        if (salas.size() >= MAX_SALAS) throw new ConflitoException("Muitas salas abertas agora. Tente de novo em instantes.");
        Sala sala = new Sala(novoCodigo(), jogo, clock.instant());
        sala.vagas.put(0, pessoa(eu));
        salas.put(sala.codigo, sala);
        return ingresso(sala, 0);
    }

    @Override
    public synchronized GameSalaDto entrar(String codigo) {
        Long eu = autorizacao.exigir(Modulo.GAMES, Acao.LEITURA).getId();
        Sala sala = sala(codigo);
        Integer minha = sala.vagas.entrySet().stream().filter(e -> eu.equals(e.getValue().idUsuario)).map(Map.Entry::getKey).findFirst().orElse(null);
        if (minha == null) {
            for (int v = 1; v < MAX_VAGAS && minha == null; v++) {
                if (!sala.vagas.containsKey(v)) minha = v;
            }
            if (minha == null) throw new ConflitoException("Sala cheia: já tem " + MAX_VAGAS + " jogadores");
            sala.vagas.put(minha, pessoa(eu));
            sala.ultimaAtividade = clock.instant();
            enviarJogadores(sala);
        }
        return ingresso(sala, minha);
    }

    @Override
    public synchronized void conectar(String bilhete, CanalSala canal) {
        Bilhete b = bilhete == null ? null : bilhetes.remove(bilhete);
        Sala sala = b == null ? null : salas.get(b.codigo);
        Vaga vaga = sala == null ? null : sala.vagas.get(b.vaga);
        if (vaga == null || clock.instant().isAfter(b.expiraEm)) {
            canal.fechar("Bilhete inválido ou vencido");
            return;
        }
        if (vaga.canal != null) {
            ligacoes.remove(vaga.canal.id());
            vaga.canal.fechar("Conectado em outro aparelho");
        }
        vaga.canal = canal;
        ligacoes.put(canal.id(), new Ligacao(sala.codigo, b.vaga));
        sala.ultimaAtividade = clock.instant();
        ObjectNode boasVindas = mapper.createObjectNode().put(TIPO, "sala").put("codigo", sala.codigo).put("jogo", sala.jogo)
                .put(VAGA, b.vaga).put("anfitriao", b.vaga == 0);
        boasVindas.set("jogadores", jogadores(sala));
        canal.enviar(boasVindas.toString());
        enviarJogadores(sala);
    }

    @Override
    public synchronized void receber(CanalSala canal, String mensagem) {
        Ligacao l = ligacoes.get(canal.id());
        Sala sala = l == null ? null : salas.get(l.codigo);
        Vaga vaga = sala == null ? null : sala.vagas.get(l.vaga);
        if (vaga == null || mensagem == null) return;
        if (mensagem.length() > MAX_MENSAGEM) {
            canal.enviar(erro("Mensagem grande demais"));
            return;
        }
        if (!dentroDoLimite(vaga)) return;
        JsonNode no;
        try {
            no = mapper.readTree(mensagem);
        } catch (JsonProcessingException e) {
            canal.enviar(erro("Mensagem inválida"));
            return;
        }
        if (no == null || !no.isObject()) {
            canal.enviar(erro("Mensagem inválida"));
            return;
        }
        sala.ultimaAtividade = clock.instant();
        String tipo = no.path(TIPO).asText("");
        if ("ping".equals(tipo)) {
            canal.enviar(mapper.createObjectNode().put(TIPO, "pong").toString());
        } else if (l.vaga == 0) {
            doAnfitriao(sala, tipo, no);
        } else {
            doControle(sala, l.vaga, tipo, no);
        }
    }

    private void doAnfitriao(Sala sala, String tipo, JsonNode no) {
        switch (tipo) {
            case "todos":
                ObjectNode todos = mapper.createObjectNode().put(TIPO, "host");
                todos.set(DADOS, no.path(DADOS));
                sala.vagas.forEach((v, vaga) -> {
                    if (v != 0 && vaga.canal != null) vaga.canal.enviar(todos.toString());
                });
                break;
            case "para":
                Vaga alvo = sala.vagas.get(no.path(VAGA).asInt(-1));
                if (alvo != null && alvo != sala.anfitriao() && alvo.canal != null) {
                    ObjectNode para = mapper.createObjectNode().put(TIPO, "host");
                    para.set(DADOS, no.path(DADOS));
                    alvo.canal.enviar(para.toString());
                }
                break;
            case "fechar":
                fecharSala(sala, "Quem hospeda fechou a sala");
                break;
            default:
                enviarErro(sala.anfitriao(), "Tipo de mensagem desconhecido");
        }
    }

    private void doControle(Sala sala, int vaga, String tipo, JsonNode no) {
        if ("entrada".equals(tipo)) {
            Vaga anfitriao = sala.anfitriao();
            if (anfitriao != null && anfitriao.canal != null) {
                ObjectNode entrada = mapper.createObjectNode().put(TIPO, "entrada").put(VAGA, vaga);
                entrada.set(DADOS, no.path(DADOS));
                anfitriao.canal.enviar(entrada.toString());
            }
        } else if ("sair".equals(tipo)) {
            Vaga saindo = sala.vagas.remove(vaga);
            ligacoes.remove(saindo.canal.id());
            saindo.canal.fechar("Saiu da sala");
            enviarJogadores(sala);
        } else {
            enviarErro(sala.vagas.get(vaga), "Tipo de mensagem desconhecido");
        }
    }

    @Override
    public synchronized void desconectar(CanalSala canal) {
        Ligacao l = ligacoes.remove(canal.id());
        Sala sala = l == null ? null : salas.get(l.codigo);
        Vaga vaga = sala == null ? null : sala.vagas.get(l.vaga);
        if (vaga == null || vaga.canal != canal) return;
        // A vaga fica guardada: quem cair volta para ela pelo mesmo código.
        vaga.canal = null;
        sala.ultimaAtividade = clock.instant();
        enviarJogadores(sala);
    }

    @Override
    @Scheduled(fixedDelay = 60_000)
    public synchronized void limpar() {
        Instant agora = clock.instant();
        bilhetes.values().removeIf(b -> agora.isAfter(b.expiraEm));
        for (Sala s : new ArrayList<>(salas.values())) {
            boolean vazia = s.vagas.values().stream().noneMatch(v -> v.canal != null);
            if (agora.isAfter(s.criadaEm.plus(SALA_MAXIMA)) || vazia && agora.isAfter(s.ultimaAtividade.plus(SALA_OCIOSA))) {
                fecharSala(s, "Sala encerrada por tempo");
            }
        }
    }

    private void fecharSala(Sala sala, String motivo) {
        salas.remove(sala.codigo);
        bilhetes.values().removeIf(b -> b.codigo.equals(sala.codigo));
        String aviso = mapper.createObjectNode().put(TIPO, "fechada").put("motivo", motivo).toString();
        for (Vaga v : sala.vagas.values()) {
            if (v.canal != null) {
                ligacoes.remove(v.canal.id());
                v.canal.enviar(aviso);
                v.canal.fechar(motivo);
                v.canal = null;
            }
        }
        log.info("Sala de games {} fechada: {}", sala.codigo, motivo);
    }

    private boolean dentroDoLimite(Vaga vaga) {
        long segundo = clock.millis() / 1000;
        if (vaga.janela != segundo) {
            vaga.janela = segundo;
            vaga.mensagens = 0;
        }
        vaga.mensagens++;
        return vaga.mensagens <= MAX_POR_SEGUNDO;
    }

    private Sala sala(String codigo) {
        String c = codigo == null ? "" : codigo.trim().toUpperCase(Locale.ROOT);
        if (!CODIGO.matcher(c).matches()) throw new EntradaInvalidaException("Código da sala inválido");
        Sala sala = salas.get(c);
        if (sala == null) throw new NaoEncontradoException("Sala não encontrada. Confira o código na tela grande.");
        return sala;
    }

    private Vaga pessoa(Long idUsuario) {
        AvatarDto a = avatares.buscar(idUsuario).orElse(null);
        String nome = a == null || a.getNmNome() == null ? "Jogador" : a.getNmNome();
        return new Vaga(idUsuario, nome, a == null ? null : a.getDsConfig());
    }

    private GameSalaDto ingresso(Sala sala, int vaga) {
        String bilhete = novoBilhete();
        bilhetes.put(bilhete, new Bilhete(sala.codigo, vaga, clock.instant().plus(VALIDADE_BILHETE)));
        return new GameSalaDto(sala.codigo, sala.jogo, vaga, vaga == 0, bilhete);
    }

    private ArrayNode jogadores(Sala sala) {
        ArrayNode lista = mapper.createArrayNode();
        sala.vagas.forEach((v, vaga) -> lista.addObject().put(VAGA, v).put("idUsuario", vaga.idUsuario).put("nome", vaga.nome)
                .put("avatar", vaga.avatar).put("conectado", vaga.canal != null));
        return lista;
    }

    private void enviarJogadores(Sala sala) {
        ObjectNode msg = mapper.createObjectNode().put(TIPO, "jogadores");
        msg.set("jogadores", jogadores(sala));
        String texto = msg.toString();
        sala.vagas.values().forEach(v -> {
            if (v.canal != null) v.canal.enviar(texto);
        });
    }

    private void enviarErro(Vaga vaga, String mensagem) {
        if (vaga != null && vaga.canal != null) vaga.canal.enviar(erro(mensagem));
    }

    private String erro(String mensagem) {
        return mapper.createObjectNode().put(TIPO, "erro").put("m", mensagem).toString();
    }

    private String novoCodigo() {
        String codigo;
        do {
            StringBuilder sb = new StringBuilder(TAMANHO_CODIGO);
            for (int i = 0; i < TAMANHO_CODIGO; i++) sb.append(ALFABETO.charAt(aleatorio.nextInt(ALFABETO.length())));
            codigo = sb.toString();
        } while (salas.containsKey(codigo));
        return codigo;
    }

    private String novoBilhete() {
        byte[] bytes = new byte[32];
        aleatorio.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Para os testes: quantas salas estão abertas. */
    synchronized int salasAbertas() {
        return salas.size();
    }

    /** Para os testes: os bilhetes ainda válidos. */
    synchronized List<String> bilhetesGuardados() {
        return new ArrayList<>(bilhetes.keySet());
    }
}
