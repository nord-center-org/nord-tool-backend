package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.dto.MarketingConexaoDto;
import br.com.nord_tool_backend.dto.MarketingContaDto;
import br.com.nord_tool_backend.dto.MarketingContaRegistroDto;
import br.com.nord_tool_backend.dto.MarketingMidiaDto;
import br.com.nord_tool_backend.dto.MarketingMidiaPublicaDto;
import br.com.nord_tool_backend.dto.MarketingPainelDto;
import br.com.nord_tool_backend.dto.MarketingPostDto;
import br.com.nord_tool_backend.dto.MarketingPublicacaoDto;
import br.com.nord_tool_backend.dto.MarketingRedeDto;
import br.com.nord_tool_backend.exception.ArquivoMuitoGrandeException;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.exception.NaoEncontradoException;
import br.com.nord_tool_backend.exception.NegocioException;
import br.com.nord_tool_backend.form.MarketingConectarForm;
import br.com.nord_tool_backend.form.MarketingMidiaForm;
import br.com.nord_tool_backend.form.MarketingPublicarForm;
import br.com.nord_tool_backend.repository.MarketingRepository;
import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.service.MarketingService;
import br.com.nord_tool_backend.service.marketing.FalhaRedeException;
import br.com.nord_tool_backend.service.marketing.MarketingCofre;
import br.com.nord_tool_backend.service.marketing.PainelRede;
import br.com.nord_tool_backend.service.marketing.PostRede;
import br.com.nord_tool_backend.service.marketing.RedeSocial;
import br.com.nord_tool_backend.service.marketing.RedeSocialProvider;
import br.com.nord_tool_backend.service.marketing.ResultadoPublicacao;
import br.com.nord_tool_backend.service.marketing.TokensRede;
import br.com.nord_tool_backend.service.marketing.VideoParaPublicar;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Marketing (v1): a pessoa conecta as próprias contas do YouTube, do Instagram e do TikTok pelo login oficial de cada
 * rede, publica um vídeo em várias delas de uma vez e acompanha seguidores e views num painel só.
 * <p>
 * O vídeo chega em partes (o limite de upload da API continua o mesmo) num arquivo temporário do servidor, é
 * publicado em segundo plano em cada conta e apagado ao terminar (ou depois de 2 horas). Os tokens das redes ficam
 * cifrados no banco e são renovados sozinhos antes de vencer.
 */
@Service
@Slf4j
public class MarketingServiceImpl implements MarketingService {

    static final long TAMANHO_PARTE = 8L * 1024 * 1024;
    static final long MAX_VIDEO = 500L * 1024 * 1024;
    static final long VIDA_MIDIA_MS = 2L * 60 * 60 * 1000;
    static final long VALIDADE_ESTADO_MS = 10L * 60 * 1000;
    static final long CACHE_PAINEL_MS = 5L * 60 * 1000;
    static final int LIMITE_PUBLICACOES = 50;
    static final String CAMINHO_VOLTA = "/gestao/marketing/conectar";
    static final String CAMINHO_MIDIA = "/api/v1/nord-tool/publico/marketing/midia/";
    private static final Set<String> TIPOS = new LinkedHashSet<>(Arrays.asList("video/mp4", "video/quicktime", "video/webm"));

    /** Vídeo sendo recebido/publicado. */
    static final class Midia {
        final Long idUsuario;
        final Path arquivo;
        final long tamanho;
        final String tipo;
        final String codigoPublico;
        final long criadaEm;
        long recebido;
        int proximaParte;
        final AtomicInteger publicando = new AtomicInteger();

        Midia(Long idUsuario, Path arquivo, long tamanho, String tipo, String codigoPublico, long criadaEm) {
            this.idUsuario = idUsuario;
            this.arquivo = arquivo;
            this.tamanho = tamanho;
            this.tipo = tipo;
            this.codigoPublico = codigoPublico;
            this.criadaEm = criadaEm;
        }

        boolean completa() {
            return recebido == tamanho;
        }
    }

    private static final class PainelEmCache {
        final long em;
        final List<MarketingPainelDto> painel;

        PainelEmCache(long em, List<MarketingPainelDto> painel) {
            this.em = em;
            this.painel = painel;
        }
    }

    private final MarketingRepository repository;
    private final AutorizacaoService autorizacao;
    private final MarketingCofre cofre;
    private final Map<RedeSocial, RedeSocialProvider> provedores = new EnumMap<>(RedeSocial.class);
    private final Set<String> origens;
    private final String urlApp;
    private final String urlApi;
    private final Executor executor;
    private final Clock relogio;
    private final Path pasta;
    private final Map<String, Midia> midias = new ConcurrentHashMap<>();
    private final Map<Long, PainelEmCache> paineis = new ConcurrentHashMap<>();

    @Autowired
    public MarketingServiceImpl(MarketingRepository repository, AutorizacaoService autorizacao, MarketingCofre cofre,
                                List<RedeSocialProvider> provedores,
                                @Value("${nord-tool.cors.allowed-origins:}") String[] origens,
                                @Value("${nord-tool.app-url:}") String urlApp,
                                @Value("${nord-tool.marketing.url-api:}") String urlApi) {
        this(repository, autorizacao, cofre, provedores, origens, urlApp, urlApi, Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "nordtool-marketing");
            t.setDaemon(true);
            return t;
        }), Clock.systemDefaultZone(), pastaTemporaria());
    }

    MarketingServiceImpl(MarketingRepository repository, AutorizacaoService autorizacao, MarketingCofre cofre,
                         List<RedeSocialProvider> provedores, String[] origens, String urlApp, String urlApi, Executor executor,
                         Clock relogio, Path pasta) {
        this.repository = repository;
        this.autorizacao = autorizacao;
        this.cofre = cofre;
        for (RedeSocialProvider p : provedores) this.provedores.put(p.rede(), p);
        this.origens = new LinkedHashSet<>();
        if (origens != null) {
            for (String o : origens) {
                if (o != null && !o.isBlank()) this.origens.add(semBarra(o.trim()));
            }
        }
        this.urlApp = urlApp == null ? "" : semBarra(urlApp.trim());
        this.urlApi = urlApi == null ? "" : semBarra(urlApi.trim());
        this.executor = executor;
        this.relogio = relogio;
        this.pasta = pasta;
    }

    private static Path pastaTemporaria() {
        try {
            return Files.createTempDirectory("nordtool-marketing");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String semBarra(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    private long agora() {
        return relogio.millis();
    }

    private LocalDateTime agoraData() {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(agora()), ZoneId.systemDefault());
    }

    // ------------------------------------------------------------------ contas

    @Override
    public List<MarketingRedeDto> redes() {
        Long eu = autorizacao.exigir(Modulo.MARKETING, Acao.LEITURA).getId();
        List<MarketingContaRegistroDto> contas = repository.contas(eu);
        List<MarketingRedeDto> redes = new ArrayList<>();
        for (RedeSocial r : RedeSocial.values()) {
            RedeSocialProvider p = provedores.get(r);
            List<MarketingContaDto> daRede = new ArrayList<>();
            for (MarketingContaRegistroDto c : contas) {
                if (r.name().equals(c.getCdRede())) daRede.add(publica(c));
            }
            redes.add(new MarketingRedeDto(r.name(), r.getNome(), p != null && p.configurada(), daRede));
        }
        return redes;
    }

    private static MarketingContaDto publica(MarketingContaRegistroDto c) {
        MarketingContaDto d = new MarketingContaDto();
        d.setId(c.getId());
        d.setCdRede(c.getCdRede());
        d.setTxNome(c.getTxNome());
        d.setTxFoto(c.getTxFoto());
        d.setDhCriacao(c.getDhCriacao());
        return d;
    }

    private RedeSocialProvider provedor(String rede) {
        RedeSocial r = RedeSocial.de(rede).orElseThrow(() -> new EntradaInvalidaException("Rede inválida"));
        RedeSocialProvider p = provedores.get(r);
        if (p == null || !p.configurada()) {
            throw new NegocioException("O " + r.getNome() + " ainda não foi configurado no servidor (faltam as chaves do app da rede).");
        }
        return p;
    }

    /** Endereço de volta do login: o do frontend que pediu, se for uma origem permitida; senão o NORD_APP_URL. */
    private String volta(String origem) {
        String o = origem == null ? "" : semBarra(origem.trim());
        if (!o.isEmpty() && origens.contains(o)) return o + CAMINHO_VOLTA;
        if (!urlApp.isEmpty()) return urlApp + CAMINHO_VOLTA;
        throw new EntradaInvalidaException("Endereço do NordTool não permitido para conectar contas.");
    }

    @Override
    public MarketingConexaoDto conexao(String rede, String origem) {
        Long eu = autorizacao.exigir(Modulo.MARKETING, Acao.ESCRITA).getId();
        RedeSocialProvider p = provedor(rede);
        String estado = cofre.assinarEstado(eu, p.rede(), agora() + VALIDADE_ESTADO_MS);
        return new MarketingConexaoDto(p.urlAutorizacao(volta(origem), estado));
    }

    @Override
    public MarketingContaDto conectar(MarketingConectarForm form, String origem) {
        Long eu = autorizacao.exigir(Modulo.MARKETING, Acao.ESCRITA).getId();
        RedeSocialProvider p = provedor(form.getCdRede());
        if (!cofre.estadoValido(form.getTxEstado(), eu, p.rede(), agora())) {
            throw new EntradaInvalidaException("O login da rede venceu ou não é desta conta. Tente conectar de novo.");
        }
        TokensRede t;
        try {
            t = p.conectar(form.getTxCodigo(), volta(origem));
        } catch (FalhaRedeException e) {
            throw new NegocioException(e.getMessage());
        }
        if (t.getIdExterno() == null || t.getIdExterno().isBlank()) throw new NegocioException(p.rede().getNome() + " não informou a conta.");
        String nome = corta(t.getNome() == null || t.getNome().isBlank() ? p.rede().getNome() : t.getNome(), 200);
        String foto = t.getFoto() == null || t.getFoto().length() > 1000 ? null : t.getFoto();
        Long id = repository.salvarConta(eu, p.rede().name(), corta(t.getIdExterno(), 100), nome, foto, cofre.cifrar(t.getTokenAcesso()),
                cofre.cifrar(t.getTokenRenovacao()), t.getExpiraEm());
        paineis.remove(eu);
        MarketingContaDto d = new MarketingContaDto();
        d.setId(id);
        d.setCdRede(p.rede().name());
        d.setTxNome(nome);
        d.setTxFoto(foto);
        d.setDhCriacao(agoraData());
        return d;
    }

    private static String corta(String s, int max) {
        return s.length() > max ? s.substring(0, max) : s;
    }

    @Override
    public void desconectar(Long idConta) {
        Long eu = autorizacao.exigir(Modulo.MARKETING, Acao.ESCRITA).getId();
        if (!repository.excluirConta(eu, idConta)) throw new NaoEncontradoException("Conta não encontrada");
        paineis.remove(eu);
    }

    /** Token de acesso decifrado, renovado antes de vencer (Instagram: com 7 dias de folga; os outros: 5 minutos). */
    String acesso(MarketingContaRegistroDto c, RedeSocialProvider p) {
        String token = cofre.decifrar(c.getTxTokenAcesso());
        if (token == null) throw new FalhaRedeException("O acesso a " + c.getTxNome() + " não vale mais. Conecte a conta de novo.");
        LocalDateTime vence = c.getDhExpiraAcesso();
        long folgaMin = p.rede() == RedeSocial.INSTAGRAM ? 7L * 24 * 60 : 5;
        if (vence == null || vence.isAfter(agoraData().plusMinutes(folgaMin))) return token;
        TokensRede novo = p.renovar(new TokensRede(token, cofre.decifrar(c.getTxTokenRenovacao()), vence, c.getTxIdExterno(),
                c.getTxNome(), c.getTxFoto()));
        repository.atualizarTokens(c.getId(), cofre.cifrar(novo.getTokenAcesso()), cofre.cifrar(novo.getTokenRenovacao()), novo.getExpiraEm());
        c.setTxTokenAcesso(cofre.cifrar(novo.getTokenAcesso()));
        c.setDhExpiraAcesso(novo.getExpiraEm());
        return novo.getTokenAcesso();
    }

    // ------------------------------------------------------------------ vídeo em partes

    private void limparMidias() {
        long limite = agora() - VIDA_MIDIA_MS;
        midias.entrySet().removeIf(e -> {
            if (e.getValue().criadaEm >= limite) return false;
            apagar(e.getValue());
            return true;
        });
    }

    private static void apagar(Midia m) {
        try {
            Files.deleteIfExists(m.arquivo);
        } catch (IOException e) {
            log.warn("Não deu para apagar o vídeo temporário do Marketing");
        }
    }

    @Override
    public MarketingMidiaDto iniciarMidia(MarketingMidiaForm form) {
        Long eu = autorizacao.exigir(Modulo.MARKETING, Acao.ESCRITA).getId();
        limparMidias();
        String tipo = form.getTxTipo() == null ? "" : form.getTxTipo().trim().toLowerCase(java.util.Locale.ROOT);
        if (!TIPOS.contains(tipo)) throw new EntradaInvalidaException("Envie um vídeo MP4, MOV ou WebM.");
        if (form.getNrTamanho() <= 0) throw new EntradaInvalidaException("Vídeo vazio.");
        if (form.getNrTamanho() > MAX_VIDEO) throw new ArquivoMuitoGrandeException("O vídeo pode ter até 500 MB.");
        long minhas = midias.values().stream().filter(m -> m.idUsuario.equals(eu)).count();
        if (minhas >= 3) throw new NegocioException("Espere os vídeos anteriores terminarem de publicar.");
        String id = cofre.codigoAleatorio();
        Path arquivo = pasta.resolve(id.replaceAll("[^A-Za-z0-9_-]", "") + ".video");
        try {
            Files.deleteIfExists(arquivo);
            Files.createFile(arquivo);
        } catch (IOException e) {
            throw new NegocioException("Não deu para receber o vídeo agora. Tente de novo.");
        }
        midias.put(id, new Midia(eu, arquivo, form.getNrTamanho(), tipo, cofre.codigoAleatorio(), agora()));
        int partes = (int) ((form.getNrTamanho() + TAMANHO_PARTE - 1) / TAMANHO_PARTE);
        return new MarketingMidiaDto(id, TAMANHO_PARTE, partes);
    }

    private Midia minha(String idMidia, Long eu) {
        Midia m = idMidia == null ? null : midias.get(idMidia);
        if (m == null || !m.idUsuario.equals(eu)) throw new NaoEncontradoException("Vídeo não encontrado. Envie de novo.");
        return m;
    }

    @Override
    public void receberParte(String idMidia, int indice, byte[] dados) {
        Long eu = autorizacao.exigir(Modulo.MARKETING, Acao.ESCRITA).getId();
        Midia m = minha(idMidia, eu);
        synchronized (m) {
            if (indice != m.proximaParte) throw new EntradaInvalidaException("Parte fora de ordem: envie a parte " + m.proximaParte + ".");
            long esperado = Math.min(TAMANHO_PARTE, m.tamanho - m.recebido);
            if (dados == null || dados.length != esperado) {
                throw new EntradaInvalidaException("Parte com tamanho errado (esperado " + esperado + " bytes).");
            }
            try (OutputStream out = Files.newOutputStream(m.arquivo, StandardOpenOption.APPEND)) {
                out.write(dados);
            } catch (IOException e) {
                throw new NegocioException("Não deu para gravar o vídeo. Tente de novo.");
            }
            m.recebido += dados.length;
            m.proximaParte++;
        }
    }

    // ------------------------------------------------------------------ publicar

    @Override
    public List<MarketingPublicacaoDto> publicar(MarketingPublicarForm form, String baseApi) {
        Long eu = autorizacao.exigir(Modulo.MARKETING, Acao.ESCRITA).getId();
        Midia m = minha(form.getIdMidia(), eu);
        if (!m.completa()) throw new EntradaInvalidaException("O vídeo ainda não terminou de subir.");
        String titulo = form.getTxTitulo() == null ? "" : form.getTxTitulo().trim();
        if (titulo.isEmpty()) throw new EntradaInvalidaException("Informe o título.");
        String legenda = form.getTxLegenda() == null ? "" : form.getTxLegenda().trim();
        List<MarketingContaRegistroDto> escolhidas = new ArrayList<>();
        for (Long idConta : new LinkedHashSet<>(form.getIdsContas())) {
            MarketingContaRegistroDto c = repository.conta(eu, idConta).orElseThrow(() -> new NaoEncontradoException("Conta não encontrada"));
            RedeSocial r = RedeSocial.de(c.getCdRede()).orElseThrow(() -> new NaoEncontradoException("Conta não encontrada"));
            if (r == RedeSocial.INSTAGRAM && "video/webm".equals(m.tipo)) {
                throw new EntradaInvalidaException("O Instagram não aceita WebM: envie MP4 ou MOV.");
            }
            escolhidas.add(c);
        }
        String base = urlApi.isEmpty() ? semBarra(baseApi == null ? "" : baseApi) : urlApi;
        String urlPublica = base.isEmpty() ? null : base + CAMINHO_MIDIA + m.codigoPublico;
        VideoParaPublicar video = new VideoParaPublicar(m.arquivo, m.tamanho, m.tipo, corta(titulo, 150), corta(legenda, 2200),
                form.isInPublico(), urlPublica);
        List<Long> ids = new ArrayList<>();
        m.publicando.addAndGet(escolhidas.size());
        for (MarketingContaRegistroDto c : escolhidas) {
            Long idPub = repository.criarPublicacao(eu, c.getId(), video.getTitulo(), legenda.isEmpty() ? null : video.getLegenda(),
                    form.isInPublico());
            ids.add(idPub);
            executor.execute(() -> publicarEm(c, idPub, video, form.getIdMidia(), m));
        }
        paineis.remove(eu);
        List<MarketingPublicacaoDto> todas = repository.publicacoes(eu, LIMITE_PUBLICACOES);
        List<MarketingPublicacaoDto> estas = new ArrayList<>();
        for (MarketingPublicacaoDto p : todas) {
            if (ids.contains(p.getId())) estas.add(p);
        }
        return estas;
    }

    private void publicarEm(MarketingContaRegistroDto c, Long idPub, VideoParaPublicar video, String idMidia, Midia m) {
        try {
            RedeSocialProvider p = provedores.get(RedeSocial.de(c.getCdRede()).orElseThrow(IllegalStateException::new));
            if (p == null || !p.configurada()) throw new FalhaRedeException("Esta rede não está configurada no servidor.");
            ResultadoPublicacao r = p.publicar(acesso(c, p), c.getTxIdExterno(), video);
            repository.concluirPublicacao(idPub, r.isPublico() ? "PUBLICADO" : "PRIVADO", r.getIdExterno(), r.getLink(), r.getAviso());
        } catch (FalhaRedeException e) {
            repository.concluirPublicacao(idPub, "ERRO", null, null, corta(e.getMessage(), 1000));
        } catch (RuntimeException e) {
            log.warn("Falha inesperada ao publicar no Marketing: {}", e.getClass().getSimpleName());
            repository.concluirPublicacao(idPub, "ERRO", null, null, "Falha inesperada ao publicar. Tente de novo.");
        } finally {
            if (m.publicando.decrementAndGet() <= 0) {
                midias.remove(idMidia);
                apagar(m);
            }
        }
    }

    @Override
    public List<MarketingPublicacaoDto> publicacoes() {
        Long eu = autorizacao.exigir(Modulo.MARKETING, Acao.LEITURA).getId();
        repository.marcarPresas(eu);
        return repository.publicacoes(eu, LIMITE_PUBLICACOES);
    }

    // ------------------------------------------------------------------ painel

    @Override
    public List<MarketingPainelDto> painel(boolean atualizar) {
        Long eu = autorizacao.exigir(Modulo.MARKETING, Acao.LEITURA).getId();
        PainelEmCache cache = paineis.get(eu);
        if (!atualizar && cache != null && agora() - cache.em < CACHE_PAINEL_MS) return cache.painel;
        List<MarketingPainelDto> painel = new ArrayList<>();
        for (MarketingContaRegistroDto c : repository.contas(eu)) {
            MarketingPainelDto d = new MarketingPainelDto();
            d.setIdConta(c.getId());
            d.setCdRede(c.getCdRede());
            d.setTxConta(c.getTxNome());
            d.setTxFoto(c.getTxFoto());
            d.setPosts(Collections.emptyList());
            Optional<RedeSocial> r = RedeSocial.de(c.getCdRede());
            RedeSocialProvider p = r.map(provedores::get).orElse(null);
            if (p == null || !p.configurada()) {
                d.setTxErro("Esta rede não está configurada no servidor.");
            } else {
                try {
                    PainelRede pr = p.painel(acesso(c, p), c.getTxIdExterno());
                    d.setNrSeguidores(pr.getSeguidores());
                    d.setNrViewsTotais(pr.getViewsTotais());
                    List<MarketingPostDto> posts = new ArrayList<>();
                    for (PostRede post : pr.getPosts()) {
                        posts.add(new MarketingPostDto(post.getId(), post.getTitulo(), post.getMiniatura(), post.getLink(), post.getData(),
                                post.getViews(), post.getCurtidas(), post.getComentarios()));
                    }
                    d.setPosts(posts);
                } catch (FalhaRedeException e) {
                    d.setTxErro(e.getMessage());
                }
            }
            painel.add(d);
        }
        paineis.put(eu, new PainelEmCache(agora(), painel));
        return painel;
    }

    // ------------------------------------------------------------------ link público do vídeo

    @Override
    public Optional<MarketingMidiaPublicaDto> midiaPublica(String codigo) {
        if (codigo == null || codigo.length() < 20) return Optional.empty();
        long limite = agora() - VIDA_MIDIA_MS;
        for (Midia m : midias.values()) {
            if (java.security.MessageDigest.isEqual(m.codigoPublico.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    codigo.getBytes(java.nio.charset.StandardCharsets.UTF_8)) && m.completa() && m.criadaEm >= limite) {
                return Optional.of(new MarketingMidiaPublicaDto(m.arquivo, m.tipo, m.tamanho));
            }
        }
        return Optional.empty();
    }
}
