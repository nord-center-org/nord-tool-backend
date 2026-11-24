package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.dto.MarketingConexaoDto;
import br.com.nord_tool_backend.dto.MarketingContaDto;
import br.com.nord_tool_backend.dto.MarketingContaRegistroDto;
import br.com.nord_tool_backend.dto.MarketingMidiaDto;
import br.com.nord_tool_backend.dto.MarketingMidiaPublicaDto;
import br.com.nord_tool_backend.dto.MarketingPainelDto;
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
import br.com.nord_tool_backend.security.UsuarioAutenticado;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.service.marketing.FalhaRedeException;
import br.com.nord_tool_backend.service.marketing.InstagramProvider;
import br.com.nord_tool_backend.service.marketing.MarketingCofre;
import br.com.nord_tool_backend.service.marketing.PainelRede;
import br.com.nord_tool_backend.service.marketing.PostRede;
import br.com.nord_tool_backend.service.marketing.RedeSocial;
import br.com.nord_tool_backend.service.marketing.RedeSocialProvider;
import br.com.nord_tool_backend.service.marketing.ResultadoPublicacao;
import br.com.nord_tool_backend.service.marketing.TiktokProvider;
import br.com.nord_tool_backend.service.marketing.TokensRede;
import br.com.nord_tool_backend.service.marketing.VideoParaPublicar;
import br.com.nord_tool_backend.service.marketing.YoutubeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MarketingServiceImplTest {

    private static final Long EU = 7L;
    private static final String ORIGEM = "https://app.nordtool.test";
    private static final Instant AGORA = Instant.parse("2026-11-24T12:00:00Z");

    /** Rede de mentira, para testar o serviço sem HTTP. */
    static class RedeFalsa implements RedeSocialProvider {
        final RedeSocial rede;
        boolean configurada = true;
        TokensRede aoConectar = new TokensRede("acesso-1", "renova-1", null, "ext-1", "Canal Teste", "https://foto/1.jpg");
        TokensRede aoRenovar;
        RuntimeException falha;
        ResultadoPublicacao resultado = new ResultadoPublicacao("post-1", "https://rede/post-1", true, null);
        PainelRede painel = new PainelRede(10L, 500L, Collections.singletonList(
                new PostRede("p1", "Vídeo", "https://mini", "https://link", "2026-11-20", 300L, 20L, 3L)));
        final List<String> tokensUsados = new ArrayList<>();
        VideoParaPublicar ultimoVideo;
        int renovacoes;

        RedeFalsa(RedeSocial rede) {
            this.rede = rede;
        }

        @Override
        public RedeSocial rede() {
            return rede;
        }

        @Override
        public boolean configurada() {
            return configurada;
        }

        @Override
        public String urlAutorizacao(String redirectUri, String state) {
            return "https://login.rede/" + rede.name() + "?redirect=" + redirectUri + "&state=" + state;
        }

        @Override
        public TokensRede conectar(String codigo, String redirectUri) {
            if (falha != null) throw falha;
            return aoConectar;
        }

        @Override
        public TokensRede renovar(TokensRede atual) {
            renovacoes++;
            return aoRenovar;
        }

        @Override
        public PainelRede painel(String tokenAcesso, String idExterno) {
            tokensUsados.add(tokenAcesso);
            if (falha != null) throw falha;
            return painel;
        }

        @Override
        public ResultadoPublicacao publicar(String tokenAcesso, String idExterno, VideoParaPublicar video) {
            tokensUsados.add(tokenAcesso);
            ultimoVideo = video;
            assertTrue(Files.exists(video.getArquivo()), "o vídeo existe enquanto publica");
            if (falha != null) throw falha;
            return resultado;
        }
    }

    @TempDir
    Path pasta;

    private MarketingRepository repository;
    private AutorizacaoService autorizacao;
    private MarketingCofre cofre;
    private RedeFalsa youtube;
    private RedeFalsa instagram;
    private RedeFalsa tiktok;
    private MarketingServiceImpl service;
    private long relogioMs = AGORA.toEpochMilli();

    private final Clock relogio = new Clock() {
        @Override
        public ZoneId getZone() {
            return ZoneId.systemDefault();
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(relogioMs);
        }
    };

    @BeforeEach
    void setUp() {
        repository = mock(MarketingRepository.class);
        autorizacao = mock(AutorizacaoService.class);
        when(autorizacao.exigir(any(), any())).thenReturn(new UsuarioAutenticado(EU, "eu@obra.com", "USUARIO",
                Collections.emptyMap(), 0, Instant.now()));
        cofre = new MarketingCofre(new byte[32]);
        youtube = new RedeFalsa(RedeSocial.YOUTUBE);
        instagram = new RedeFalsa(RedeSocial.INSTAGRAM);
        tiktok = new RedeFalsa(RedeSocial.TIKTOK);
        tiktok.configurada = false;
        service = novo(new String[]{ORIGEM + "/", " "}, "");
    }

    private MarketingServiceImpl novo(String[] origens, String urlApp) {
        return new MarketingServiceImpl(repository, autorizacao, cofre, Arrays.asList(youtube, instagram, tiktok), origens, urlApp,
                "", Runnable::run, relogio, pasta);
    }

    private LocalDateTime agoraData() {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(relogioMs), ZoneId.systemDefault());
    }

    private MarketingContaRegistroDto conta(Long id, RedeSocial rede, String token, LocalDateTime expira) {
        MarketingContaRegistroDto c = new MarketingContaRegistroDto();
        c.setId(id);
        c.setIdUsuario(EU);
        c.setCdRede(rede.name());
        c.setTxIdExterno("ext-" + id);
        c.setTxNome("Conta " + id);
        c.setTxFoto("https://foto/" + id);
        c.setTxTokenAcesso(cofre.cifrar(token));
        c.setTxTokenRenovacao(cofre.cifrar("renova-" + id));
        c.setDhExpiraAcesso(expira);
        c.setDhCriacao(agoraData());
        return c;
    }

    // ------------------------------------------------------------------ contas

    @Test
    void redesMostraAsTresComConfiguracaoEContasSemTokens() {
        when(repository.contas(EU)).thenReturn(Arrays.asList(conta(1L, RedeSocial.YOUTUBE, "a", null),
                conta(2L, RedeSocial.INSTAGRAM, "b", null)));
        List<MarketingRedeDto> redes = service.redes();
        assertEquals(3, redes.size());
        assertEquals("YOUTUBE", redes.get(0).getCdRede());
        assertEquals("YouTube", redes.get(0).getNmRede());
        assertTrue(redes.get(0).isInConfigurada());
        assertFalse(redes.get(2).isInConfigurada());
        assertEquals(1, redes.get(0).getContas().size());
        assertEquals("Conta 1", redes.get(0).getContas().get(0).getTxNome());
        assertTrue(redes.get(2).getContas().isEmpty());
        verify(autorizacao).exigir(Modulo.MARKETING, Acao.LEITURA);
    }

    @Test
    void conexaoDevolveLoginDaRedeComStateAssinadoEVoltaNaOrigemPermitida() {
        MarketingConexaoDto c = service.conexao("youtube", ORIGEM);
        assertTrue(c.getTxUrl().startsWith("https://login.rede/YOUTUBE?redirect=" + ORIGEM + "/gestao/marketing/conectar&state="));
        String state = c.getTxUrl().substring(c.getTxUrl().indexOf("state=") + 6);
        assertTrue(cofre.estadoValido(state, EU, RedeSocial.YOUTUBE, relogioMs));
        assertFalse(cofre.estadoValido(state, 8L, RedeSocial.YOUTUBE, relogioMs));
        assertFalse(cofre.estadoValido(state, EU, RedeSocial.TIKTOK, relogioMs));
        assertFalse(cofre.estadoValido(state, EU, RedeSocial.YOUTUBE, relogioMs + 11 * 60_000));
        verify(autorizacao).exigir(Modulo.MARKETING, Acao.ESCRITA);
    }

    @Test
    void conexaoRecusaRedeInvalidaNaoConfiguradaEOrigemDesconhecida() {
        assertThrows(EntradaInvalidaException.class, () -> service.conexao("orkut", ORIGEM));
        assertThrows(EntradaInvalidaException.class, () -> service.conexao(null, ORIGEM));
        NegocioException e = assertThrows(NegocioException.class, () -> service.conexao("TIKTOK", ORIGEM));
        assertTrue(e.getMessage().contains("TikTok"));
        assertThrows(EntradaInvalidaException.class, () -> service.conexao("YOUTUBE", "https://malicioso.test"));
        // Sem origem permitida, usa o NORD_APP_URL.
        MarketingServiceImpl comApp = novo(null, "https://nordtool.test/");
        assertTrue(comApp.conexao("YOUTUBE", "https://malicioso.test").getTxUrl().contains("redirect=https://nordtool.test/gestao/marketing/conectar"));
    }

    private MarketingConectarForm form(String rede, String state) {
        MarketingConectarForm f = new MarketingConectarForm();
        f.setCdRede(rede);
        f.setTxCodigo("codigo-da-rede");
        f.setTxEstado(state);
        return f;
    }

    @Test
    void conectarGuardaTokensCifradosEDevolveAContaSemTokens() {
        String state = cofre.assinarEstado(EU, RedeSocial.YOUTUBE, relogioMs + 60_000);
        when(repository.salvarConta(eq(EU), eq("YOUTUBE"), eq("ext-1"), eq("Canal Teste"), eq("https://foto/1.jpg"), anyString(),
                anyString(), isNull())).thenAnswer(inv -> {
                    String acesso = inv.getArgument(5);
                    assertNotEquals("acesso-1", acesso);
                    assertEquals("acesso-1", cofre.decifrar(acesso));
                    assertEquals("renova-1", cofre.decifrar(inv.getArgument(6)));
                    return 99L;
                });
        MarketingContaDto c = service.conectar(form("YOUTUBE", state), ORIGEM);
        assertEquals(99L, c.getId());
        assertEquals("YOUTUBE", c.getCdRede());
        assertEquals("Canal Teste", c.getTxNome());
    }

    @Test
    void conectarCortaNomeLongoIgnoraFotoGrandeEUsaONomeDaRede() {
        StringBuilder longo = new StringBuilder();
        for (int i = 0; i < 300; i++) longo.append('x');
        youtube.aoConectar = new TokensRede("a", null, null, "ext-9", longo.toString(), "https://" + longo + longo + longo + longo);
        String state = cofre.assinarEstado(EU, RedeSocial.YOUTUBE, relogioMs + 60_000);
        when(repository.salvarConta(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(1L);
        MarketingContaDto c = service.conectar(form("YOUTUBE", state), ORIGEM);
        assertEquals(200, c.getTxNome().length());
        assertNull(c.getTxFoto());
        youtube.aoConectar = new TokensRede("a", null, null, "ext-9", " ", null);
        assertEquals("YouTube", service.conectar(form("YOUTUBE", cofre.assinarEstado(EU, RedeSocial.YOUTUBE, relogioMs + 60_000)), ORIGEM).getTxNome());
    }

    @Test
    void conectarRecusaStateInvalidoFalhaDaRedeEContaSemId() {
        assertThrows(EntradaInvalidaException.class, () -> service.conectar(form("YOUTUBE", "forjado.1"), ORIGEM));
        String deOutro = cofre.assinarEstado(8L, RedeSocial.YOUTUBE, relogioMs + 60_000);
        assertThrows(EntradaInvalidaException.class, () -> service.conectar(form("YOUTUBE", deOutro), ORIGEM));
        youtube.falha = new FalhaRedeException("YouTube recusou: invalid_grant");
        NegocioException e = assertThrows(NegocioException.class,
                () -> service.conectar(form("YOUTUBE", cofre.assinarEstado(EU, RedeSocial.YOUTUBE, relogioMs + 60_000)), ORIGEM));
        assertEquals("YouTube recusou: invalid_grant", e.getMessage());
        youtube.falha = null;
        youtube.aoConectar = new TokensRede("a", null, null, "", "x", null);
        assertThrows(NegocioException.class,
                () -> service.conectar(form("YOUTUBE", cofre.assinarEstado(EU, RedeSocial.YOUTUBE, relogioMs + 60_000)), ORIGEM));
        verify(repository, never()).salvarConta(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void desconectarSoAPropriaConta() {
        when(repository.excluirConta(EU, 5L)).thenReturn(true);
        service.desconectar(5L);
        assertThrows(NaoEncontradoException.class, () -> service.desconectar(6L));
    }

    // ------------------------------------------------------------------ vídeo e publicação

    private MarketingMidiaForm midia(long tamanho, String tipo) {
        MarketingMidiaForm f = new MarketingMidiaForm();
        f.setTxNome("video.mp4");
        f.setNrTamanho(tamanho);
        f.setTxTipo(tipo);
        return f;
    }

    private String enviar(byte[] video, String tipo) {
        MarketingMidiaDto m = service.iniciarMidia(midia(video.length, tipo));
        int partes = (int) ((video.length + m.getNrTamanhoParte() - 1) / m.getNrTamanhoParte());
        assertEquals(partes, m.getNrPartes());
        for (int i = 0; i < partes; i++) {
            int ini = (int) (i * m.getNrTamanhoParte());
            int fim = (int) Math.min(video.length, ini + m.getNrTamanhoParte());
            service.receberParte(m.getId(), i, Arrays.copyOfRange(video, ini, fim));
        }
        return m.getId();
    }

    private MarketingPublicarForm publicar(String idMidia, Long... contas) {
        MarketingPublicarForm f = new MarketingPublicarForm();
        f.setIdMidia(idMidia);
        f.setTxTitulo("  Meu vídeo  ");
        f.setTxLegenda(" legenda #nordtool ");
        f.setIdsContas(Arrays.asList(contas));
        return f;
    }

    @Test
    void iniciarMidiaValidaTipoTamanhoELimiteDeEnviosAbertos() {
        assertThrows(EntradaInvalidaException.class, () -> service.iniciarMidia(midia(10, "image/png")));
        assertThrows(EntradaInvalidaException.class, () -> service.iniciarMidia(midia(10, null)));
        assertThrows(EntradaInvalidaException.class, () -> service.iniciarMidia(midia(0, "video/mp4")));
        assertThrows(ArquivoMuitoGrandeException.class, () -> service.iniciarMidia(midia(MarketingServiceImpl.MAX_VIDEO + 1, "video/mp4")));
        service.iniciarMidia(midia(10, "VIDEO/MP4"));
        service.iniciarMidia(midia(10, "video/quicktime"));
        service.iniciarMidia(midia(10, "video/webm"));
        assertThrows(NegocioException.class, () -> service.iniciarMidia(midia(10, "video/mp4")));
        // Duas horas depois os vídeos esquecidos são apagados e libera de novo.
        relogioMs += MarketingServiceImpl.VIDA_MIDIA_MS + 1;
        assertNotNull(service.iniciarMidia(midia(10, "video/mp4")));
    }

    @Test
    void partesSoEmOrdemComOTamanhoCertoESoDoDono() {
        MarketingMidiaDto m = service.iniciarMidia(midia(10, "video/mp4"));
        assertThrows(EntradaInvalidaException.class, () -> service.receberParte(m.getId(), 1, new byte[10]));
        assertThrows(EntradaInvalidaException.class, () -> service.receberParte(m.getId(), 0, new byte[9]));
        assertThrows(EntradaInvalidaException.class, () -> service.receberParte(m.getId(), 0, null));
        assertThrows(NaoEncontradoException.class, () -> service.receberParte("outro", 0, new byte[10]));
        assertThrows(NaoEncontradoException.class, () -> service.receberParte(null, 0, new byte[10]));
        when(autorizacao.exigir(any(), any())).thenReturn(new UsuarioAutenticado(8L, "outro@obra.com", "USUARIO",
                Collections.emptyMap(), 0, Instant.now()));
        assertThrows(NaoEncontradoException.class, () -> service.receberParte(m.getId(), 0, new byte[10]));
    }

    @Test
    void publicaEmCadaContaComOTokenDecifradoEApagaOVideoNoFim() throws Exception {
        byte[] video = new byte[(int) (MarketingServiceImpl.TAMANHO_PARTE + 1234)];
        video[0] = 1;
        video[video.length - 1] = 9;
        String id = enviar(video, "video/mp4");
        when(repository.conta(EU, 1L)).thenReturn(Optional.of(conta(1L, RedeSocial.YOUTUBE, "tok-yt", null)));
        when(repository.conta(EU, 2L)).thenReturn(Optional.of(conta(2L, RedeSocial.INSTAGRAM, "tok-ig", agoraData().plusDays(30))));
        when(repository.criarPublicacao(eq(EU), anyLong(), eq("Meu vídeo"), eq("legenda #nordtool"), eq(true))).thenReturn(11L, 12L);
        MarketingPublicacaoDto p11 = new MarketingPublicacaoDto();
        p11.setId(11L);
        MarketingPublicacaoDto p99 = new MarketingPublicacaoDto();
        p99.setId(99L);
        when(repository.publicacoes(EU, MarketingServiceImpl.LIMITE_PUBLICACOES)).thenReturn(Arrays.asList(p11, p99));
        instagram.resultado = new ResultadoPublicacao("ig-1", "https://instagram.com/p/1", false, "ficou privado");

        List<MarketingPublicacaoDto> r = service.publicar(publicar(id, 1L, 2L, 1L), "https://api.nordtool.test/");

        assertEquals(1, r.size());
        assertEquals(Collections.singletonList("tok-yt"), youtube.tokensUsados);
        assertEquals(Collections.singletonList("tok-ig"), instagram.tokensUsados);
        VideoParaPublicar v = youtube.ultimoVideo;
        assertEquals(video.length, v.getTamanho());
        assertEquals("Meu vídeo", v.getTitulo());
        assertTrue(v.getUrlPublica().startsWith("https://api.nordtool.test/api/v1/nord-tool/publico/marketing/midia/"));
        verify(repository).concluirPublicacao(11L, "PUBLICADO", "post-1", "https://rede/post-1", null);
        verify(repository).concluirPublicacao(12L, "PRIVADO", "ig-1", "https://instagram.com/p/1", "ficou privado");
        assertFalse(Files.exists(v.getArquivo()), "o vídeo é apagado depois de publicar em todas");
        assertThrows(NaoEncontradoException.class, () -> service.publicar(publicar(id, 1L), "x"));
    }

    @Test
    void oVideoChegaInteiroEFicaNoLinkPublicoEnquantoPublica() throws Exception {
        byte[] video = "conteudo do video".getBytes(StandardCharsets.UTF_8);
        String id = enviar(video, "video/mp4");
        when(repository.conta(EU, 1L)).thenReturn(Optional.of(conta(1L, RedeSocial.INSTAGRAM, "tok", null)));
        when(repository.criarPublicacao(any(), any(), any(), any(), anyBoolean())).thenReturn(1L);
        final byte[][] lido = new byte[1][];
        final Optional<?>[] publico = new Optional<?>[1];
        instagram.resultado = new ResultadoPublicacao("x", null, true, null) {
            @Override
            public String getIdExterno() {
                try {
                    String url = instagram.ultimoVideo.getUrlPublica();
                    String codigo = url.substring(url.lastIndexOf('/') + 1);
                    Optional<MarketingMidiaPublicaDto> m = service.midiaPublica(codigo);
                    publico[0] = m;
                    lido[0] = Files.readAllBytes(m.get().getArquivo());
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
                return "x";
            }
        };
        service.publicar(publicar(id, 1L), "https://api.nordtool.test");
        assertTrue(publico[0].isPresent());
        assertEquals("conteudo do video", new String(lido[0], StandardCharsets.UTF_8));
        assertFalse(service.midiaPublica("curto").isPresent());
        assertFalse(service.midiaPublica(null).isPresent());
        assertFalse(service.midiaPublica("codigo-que-nao-existe-com-tamanho-grande").isPresent());
        // Sem endereço da API, o Instagram recebe a URL nula (e a rede acusa o erro).
        instagram.resultado = new ResultadoPublicacao("y", null, true, null);
        service.publicar(publicar(enviar(video, "video/mp4"), 1L), "");
        assertNull(instagram.ultimoVideo.getUrlPublica());
    }

    @Test
    void publicarValidaVideoCompletoTituloContasEWebmNoInstagram() {
        MarketingMidiaDto m = service.iniciarMidia(midia(10, "video/webm"));
        assertThrows(EntradaInvalidaException.class, () -> service.publicar(publicar(m.getId(), 1L), ""));
        service.receberParte(m.getId(), 0, new byte[10]);
        MarketingPublicarForm semTitulo = publicar(m.getId(), 1L);
        semTitulo.setTxTitulo("  ");
        assertThrows(EntradaInvalidaException.class, () -> service.publicar(semTitulo, ""));
        semTitulo.setTxTitulo(null);
        assertThrows(EntradaInvalidaException.class, () -> service.publicar(semTitulo, ""));
        when(repository.conta(EU, 1L)).thenReturn(Optional.empty());
        assertThrows(NaoEncontradoException.class, () -> service.publicar(publicar(m.getId(), 1L), ""));
        MarketingContaRegistroDto estranha = conta(3L, RedeSocial.YOUTUBE, "t", null);
        estranha.setCdRede("ORKUT");
        when(repository.conta(EU, 3L)).thenReturn(Optional.of(estranha));
        assertThrows(NaoEncontradoException.class, () -> service.publicar(publicar(m.getId(), 3L), ""));
        when(repository.conta(EU, 2L)).thenReturn(Optional.of(conta(2L, RedeSocial.INSTAGRAM, "t", null)));
        EntradaInvalidaException e = assertThrows(EntradaInvalidaException.class, () -> service.publicar(publicar(m.getId(), 2L), ""));
        assertTrue(e.getMessage().contains("WebM"));
        verify(repository, never()).criarPublicacao(any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void falhaDaRedeTokenInvalidoRedeDesconfiguradaEErroInesperadoViramErroNaPublicacao() {
        byte[] video = new byte[4];
        when(repository.criarPublicacao(any(), any(), any(), any(), anyBoolean())).thenReturn(1L, 2L, 3L, 4L);
        when(repository.conta(EU, 1L)).thenReturn(Optional.of(conta(1L, RedeSocial.YOUTUBE, "t", null)));
        youtube.falha = new FalhaRedeException("YouTube recusou: quota");
        MarketingPublicarForm f = publicar(enviar(video, "video/mp4"), 1L);
        f.setTxLegenda(null);
        service.publicar(f, "");
        verify(repository).criarPublicacao(EU, 1L, "Meu vídeo", null, true);
        verify(repository).concluirPublicacao(1L, "ERRO", null, null, "YouTube recusou: quota");

        youtube.falha = new IllegalStateException("bug");
        service.publicar(publicar(enviar(video, "video/mp4"), 1L), "");
        verify(repository).concluirPublicacao(2L, "ERRO", null, null, "Falha inesperada ao publicar. Tente de novo.");

        MarketingContaRegistroDto quebrada = conta(1L, RedeSocial.YOUTUBE, "t", null);
        quebrada.setTxTokenAcesso("nao-cifrado");
        when(repository.conta(EU, 1L)).thenReturn(Optional.of(quebrada));
        youtube.falha = null;
        service.publicar(publicar(enviar(video, "video/mp4"), 1L), "");
        verify(repository).concluirPublicacao(eq(3L), eq("ERRO"), isNull(), isNull(), org.mockito.ArgumentMatchers.contains("Conecte a conta de novo"));

        when(repository.conta(EU, 5L)).thenReturn(Optional.of(conta(5L, RedeSocial.TIKTOK, "t", null)));
        service.publicar(publicar(enviar(video, "video/mp4"), 5L), "");
        verify(repository).concluirPublicacao(4L, "ERRO", null, null, "Esta rede não está configurada no servidor.");
    }

    @Test
    void renovaOTokenAntesDeVencerEGuardaONovoCifrado() {
        when(repository.contas(EU)).thenReturn(Arrays.asList(conta(1L, RedeSocial.YOUTUBE, "velho", agoraData().plusMinutes(2)),
                conta(2L, RedeSocial.INSTAGRAM, "ig-velho", agoraData().plusDays(3)),
                conta(3L, RedeSocial.INSTAGRAM, "ig-ok", agoraData().plusDays(30))));
        youtube.aoRenovar = new TokensRede("novo", "renova-novo", agoraData().plusHours(1), "ext-1", "Conta 1", null);
        instagram.aoRenovar = new TokensRede("ig-novo", null, agoraData().plusDays(60), "ext-2", "Conta 2", null);
        service.painel(false);
        assertEquals(Collections.singletonList("novo"), youtube.tokensUsados);
        assertEquals(Arrays.asList("ig-novo", "ig-ok"), instagram.tokensUsados);
        assertEquals(1, youtube.renovacoes);
        assertEquals(1, instagram.renovacoes);
        verify(repository).atualizarTokens(eq(1L), any(), any(), eq(agoraData().plusHours(1)));
        verify(repository).atualizarTokens(eq(2L), any(), isNull(), eq(agoraData().plusDays(60)));
    }

    @Test
    void painelJuntaAsContasGuardaPorCincoMinutosEMostraOErroDeCadaUma() {
        MarketingContaRegistroDto estranha = conta(4L, RedeSocial.YOUTUBE, "t", null);
        estranha.setCdRede("ORKUT");
        when(repository.contas(EU)).thenReturn(Arrays.asList(conta(1L, RedeSocial.YOUTUBE, "a", null),
                conta(2L, RedeSocial.INSTAGRAM, "b", null), conta(3L, RedeSocial.TIKTOK, "c", null), estranha));
        instagram.falha = new FalhaRedeException("Instagram recusou: token");
        List<MarketingPainelDto> p = service.painel(false);
        assertEquals(4, p.size());
        assertEquals(10L, p.get(0).getNrSeguidores());
        assertEquals(500L, p.get(0).getNrViewsTotais());
        assertEquals(300L, p.get(0).getPosts().get(0).getNrViews());
        assertEquals("Vídeo", p.get(0).getPosts().get(0).getTxTitulo());
        assertNull(p.get(0).getTxErro());
        assertEquals("Instagram recusou: token", p.get(1).getTxErro());
        assertEquals("Esta rede não está configurada no servidor.", p.get(2).getTxErro());
        assertEquals("Esta rede não está configurada no servidor.", p.get(3).getTxErro());
        assertTrue(service.painel(false) == p, "dentro de 5 minutos vem do cache");
        assertFalse(service.painel(true) == p, "atualizar busca de novo");
        relogioMs += MarketingServiceImpl.CACHE_PAINEL_MS + 1;
        service.painel(false);
        verify(repository, times(3)).contas(EU);
    }

    @Test
    void publicacoesMarcaAsPresasAntesDeListar() {
        when(repository.publicacoes(EU, 50)).thenReturn(Collections.emptyList());
        assertTrue(service.publicacoes().isEmpty());
        verify(repository).marcarPresas(EU);
    }

    @Test
    void cofreCifraComIvAleatorioERecusaTextoAdulterado() {
        String a = cofre.cifrar("segredo");
        String b = cofre.cifrar("segredo");
        assertNotEquals(a, b);
        assertEquals("segredo", cofre.decifrar(a));
        assertNull(cofre.cifrar(null));
        assertNull(cofre.decifrar(null));
        assertNull(cofre.decifrar("AAAA"));
        assertNull(cofre.decifrar("@@@"));
        assertNull(new MarketingCofre(new byte[]{1, 2, 3}).decifrar(a), "outra chave não decifra");
        assertFalse(cofre.estadoValido(null, EU, RedeSocial.YOUTUBE, 0));
        assertFalse(cofre.estadoValido("semponto", EU, RedeSocial.YOUTUBE, 0));
        assertFalse(cofre.estadoValido("a.@@@", EU, RedeSocial.YOUTUBE, 0));
        assertFalse(cofre.estadoValido("1.YOUTUBE.x.y.zz", EU, RedeSocial.YOUTUBE, 0));
        assertTrue(new MarketingCofre("", "", "segredo-jwt").decifrar(new MarketingCofre("", "", "segredo-jwt").cifrar("x")).equals("x"));
        assertThrows(IllegalStateException.class, () -> new MarketingCofre("AAAA", "", ""));
        assertNotNull(new MarketingCofre(java.util.Base64.getEncoder().encodeToString(new byte[32]), "", "").cifrar("x"));
        assertNotNull(new MarketingCofre("", "", "").cifrar("x"));
    }

    @Test
    void redeSocialPeloNome() {
        assertEquals(Optional.of(RedeSocial.TIKTOK), RedeSocial.de(" tiktok "));
        assertFalse(RedeSocial.de("x").isPresent());
        assertFalse(RedeSocial.de(null).isPresent());
    }

    // ------------------------------------------------------------------ provedores com as APIs simuladas

    private static VideoParaPublicar videoDe(Path arquivo, long tamanho, boolean publico) {
        return new VideoParaPublicar(arquivo, tamanho, "video/mp4", "Título", "Legenda", publico, "https://api/midia/abc");
    }

    @Nested
    class Youtube {
        RestTemplate http = new RestTemplate();
        MockRestServiceServer api = MockRestServiceServer.bindTo(http).build();
        List<String> envios = new ArrayList<>();
        YoutubeProvider yt = new YoutubeProvider(http, " id ", " segredo ") {
            @Override
            protected RespostaEnvio enviarTrecho(String url, String metodo, Map<String, String> cab, Path arquivo, long inicio, long tamanho) {
                envios.add(metodo + " " + url + " " + inicio + "+" + tamanho + " " + cab.get("Content-Type"));
                return new RespostaEnvio(200, "{\"id\":\"vid1\",\"status\":{\"privacyStatus\":\"private\"}}", Optional.empty());
            }
        };

        @Test
        void conectaRenovaPainelEPublica() throws Exception {
            assertTrue(yt.configurada());
            assertFalse(new YoutubeProvider(http, null, null).configurada());
            assertEquals(RedeSocial.YOUTUBE, yt.rede());
            String url = yt.urlAutorizacao("https://app/marketing/conectar", "st");
            assertTrue(url.contains("client_id=id") && url.contains("access_type=offline") && url.contains("state=st"));

            api.verify();
            api.reset();
            api.expect(requestTo("https://oauth2.googleapis.com/token")).andExpect(method(HttpMethod.POST))
                    .andExpect(content().string(containsString("grant_type=authorization_code")))
                    .andRespond(withSuccess("{\"access_token\":\"A\",\"refresh_token\":\"R\",\"expires_in\":3600}", MediaType.APPLICATION_JSON));
            api.expect(requestTo(startsWith("https://www.googleapis.com/youtube/v3/channels"))).andExpect(header("Authorization", "Bearer A"))
                    .andRespond(withSuccess("{\"items\":[{\"id\":\"UC1\",\"snippet\":{\"title\":\"Meu canal\",\"thumbnails\":{\"default\":{\"url\":\"f\"}}}}]}",
                            MediaType.APPLICATION_JSON));
            TokensRede t = yt.conectar("cod", "https://app/marketing/conectar");
            assertEquals("A", t.getTokenAcesso());
            assertEquals("R", t.getTokenRenovacao());
            assertEquals("UC1", t.getIdExterno());
            assertEquals("Meu canal", t.getNome());
            assertNotNull(t.getExpiraEm());

            api.verify();
            api.reset();
            api.expect(requestTo("https://oauth2.googleapis.com/token")).andExpect(content().string(containsString("grant_type=refresh_token")))
                    .andRespond(withSuccess("{\"access_token\":\"A2\",\"expires_in\":3600}", MediaType.APPLICATION_JSON));
            TokensRede r = yt.renovar(t);
            assertEquals("A2", r.getTokenAcesso());
            assertEquals("R", r.getTokenRenovacao());
            assertThrows(FalhaRedeException.class, () -> yt.renovar(new TokensRede("a", null, null, "x", "x", null)));

            api.verify();
            api.reset();
            api.expect(requestTo(startsWith("https://www.googleapis.com/youtube/v3/channels")))
                    .andRespond(withSuccess("{\"items\":[{\"id\":\"UC1\",\"statistics\":{\"subscriberCount\":\"42\",\"viewCount\":\"1000\"},"
                            + "\"contentDetails\":{\"relatedPlaylists\":{\"uploads\":\"UU1\"}}}]}", MediaType.APPLICATION_JSON));
            api.expect(requestTo(startsWith("https://www.googleapis.com/youtube/v3/playlistItems")))
                    .andRespond(withSuccess("{\"items\":[{\"contentDetails\":{\"videoId\":\"v1\"}},{\"contentDetails\":{}}]}", MediaType.APPLICATION_JSON));
            api.expect(requestTo(startsWith("https://www.googleapis.com/youtube/v3/videos?part=snippet,statistics&id=v1")))
                    .andRespond(withSuccess("{\"items\":[{\"id\":\"v1\",\"snippet\":{\"title\":\"T\",\"publishedAt\":\"2026\"},"
                            + "\"statistics\":{\"viewCount\":\"77\",\"likeCount\":\"5\"}}]}", MediaType.APPLICATION_JSON));
            PainelRede p = yt.painel("A2", "UC1");
            assertEquals(42L, p.getSeguidores());
            assertEquals(1000L, p.getViewsTotais());
            assertEquals(77L, p.getPosts().get(0).getViews());
            assertNull(p.getPosts().get(0).getComentarios());
            assertEquals("https://youtu.be/v1", p.getPosts().get(0).getLink());

            HttpHeaders local = new HttpHeaders();
            local.setLocation(java.net.URI.create("https://upload.google/sessao"));
            api.verify();
            api.reset();
            api.expect(requestTo(YoutubeProviderUpload.URL)).andExpect(method(HttpMethod.POST))
                    .andExpect(header("X-Upload-Content-Length", "5"))
                    .andExpect(content().string(containsString("\"privacyStatus\":\"public\"")))
                    .andRespond(withSuccess().headers(local));
            Path f = Files.write(pasta.resolve("v.mp4"), new byte[5]);
            ResultadoPublicacao res = yt.publicar("A2", "UC1", videoDe(f, 5, true));
            assertEquals("vid1", res.getIdExterno());
            assertFalse(res.isPublico());
            assertTrue(res.getAviso().contains("privado"));
            assertEquals(Collections.singletonList("PUT https://upload.google/sessao 0+5 video/mp4"), envios);
            api.verify();
        }

        @Test
        void errosDaApiViramMensagemSemTokenECanalInexistente() {
            api.expect(requestTo("https://oauth2.googleapis.com/token"))
                    .andRespond(withStatus(HttpStatus.BAD_REQUEST).body("{\"error\":\"invalid_grant\",\"error_description\":\"Bad Request\"}")
                            .contentType(MediaType.APPLICATION_JSON));
            FalhaRedeException e = assertThrows(FalhaRedeException.class, () -> yt.conectar("c", "r"));
            assertEquals("YouTube recusou: Bad Request", e.getMessage());
            api.verify();
            api.reset();
            api.expect(requestTo("https://oauth2.googleapis.com/token")).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
            assertThrows(FalhaRedeException.class, () -> yt.conectar("c", "r"));
            api.verify();
            api.reset();
            api.expect(requestTo("https://oauth2.googleapis.com/token")).andRespond(withSuccess("{\"access_token\":\"A\"}", MediaType.APPLICATION_JSON));
            api.expect(requestTo(startsWith("https://www.googleapis.com/youtube/v3/channels"))).andRespond(withSuccess("{\"items\":[]}", MediaType.APPLICATION_JSON));
            assertTrue(assertThrows(FalhaRedeException.class, () -> yt.conectar("c", "r")).getMessage().contains("canal"));
            api.verify();
            api.reset();
            api.expect(requestTo(YoutubeProviderUpload.URL)).andRespond(withStatus(HttpStatus.FORBIDDEN)
                    .body("{\"error\":{\"message\":\"quotaExceeded\"}}").contentType(MediaType.APPLICATION_JSON));
            assertEquals("YouTube recusou: quotaExceeded", assertThrows(FalhaRedeException.class,
                    () -> yt.publicar("A", "UC1", videoDe(pasta, 1, true))).getMessage());
            api.verify();
            api.reset();
            api.expect(requestTo(YoutubeProviderUpload.URL)).andRespond(withSuccess());
            assertThrows(FalhaRedeException.class, () -> yt.publicar("A", "UC1", videoDe(pasta, 1, true)));
            api.verify();
        }
    }

    /** URL de envio do YouTube (a mesma do provedor). */
    static final class YoutubeProviderUpload {
        static final String URL = "https://www.googleapis.com/upload/youtube/v3/videos?uploadType=resumable&part=snippet,status";
    }

    @Nested
    class Instagram {
        RestTemplate http = new RestTemplate();
        MockRestServiceServer api = MockRestServiceServer.bindTo(http).build();
        int pausas;
        InstagramProvider ig = new InstagramProvider(http, "app", "seg", "v24.0") {
            @Override
            protected void esperar(long ms) {
                pausas++;
            }
        };

        @Test
        void conectaComTokenLongoPainelComViewsEPublicaReel() {
            assertTrue(ig.configurada());
            assertFalse(new InstagramProvider(http, "", "", "").configurada());
            assertTrue(ig.urlAutorizacao("https://app/v", "st").contains("instagram_business_content_publish"));
            api.verify();
            api.reset();
            api.expect(requestTo("https://api.instagram.com/oauth/access_token"))
                    .andRespond(withSuccess("{\"data\":[{\"access_token\":\"curto\",\"user_id\":\"1\"}]}", MediaType.APPLICATION_JSON));
            api.expect(requestTo(startsWith("https://graph.instagram.com/access_token?grant_type=ig_exchange_token")))
                    .andRespond(withSuccess("{\"access_token\":\"longo\",\"expires_in\":5184000}", MediaType.APPLICATION_JSON));
            api.expect(requestTo(startsWith("https://graph.instagram.com/v24.0/me?fields=user_id")))
                    .andRespond(withSuccess("{\"user_id\":\"178\",\"username\":\"nordtool\"}", MediaType.APPLICATION_JSON));
            TokensRede t = ig.conectar("c", "r");
            assertEquals("longo", t.getTokenAcesso());
            assertEquals("178", t.getIdExterno());
            assertEquals("@nordtool", t.getNome());

            api.verify();
            api.reset();
            api.expect(requestTo(startsWith("https://graph.instagram.com/refresh_access_token")))
                    .andRespond(withSuccess("{\"access_token\":\"renovado\",\"expires_in\":5184000}", MediaType.APPLICATION_JSON));
            assertEquals("renovado", ig.renovar(t).getTokenAcesso());
            api.verify();
            api.reset();
            api.expect(requestTo(startsWith("https://graph.instagram.com/refresh_access_token"))).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
            assertThrows(FalhaRedeException.class, () -> ig.renovar(t));

            api.verify();
            api.reset();
            api.expect(requestTo(startsWith("https://graph.instagram.com/v24.0/me?fields=followers_count")))
                    .andRespond(withSuccess("{\"followers_count\":120}", MediaType.APPLICATION_JSON));
            api.expect(requestTo(startsWith("https://graph.instagram.com/v24.0/me/media")))
                    .andRespond(withSuccess("{\"data\":[{\"id\":\"m1\",\"caption\":\"Primeiro reel\",\"media_type\":\"VIDEO\",\"thumbnail_url\":\"t\","
                            + "\"permalink\":\"https://instagram.com/reel/1\",\"like_count\":4},{\"id\":\"m2\",\"media_type\":\"IMAGE\",\"media_url\":\"img\"}]}",
                            MediaType.APPLICATION_JSON));
            api.expect(requestTo(startsWith("https://graph.instagram.com/v24.0/m1/insights?metric=views")))
                    .andRespond(withSuccess("{\"data\":[{\"name\":\"views\",\"values\":[{\"value\":350}]}]}", MediaType.APPLICATION_JSON));
            api.expect(requestTo(startsWith("https://graph.instagram.com/v24.0/m2/insights?metric=views")))
                    .andRespond(withStatus(HttpStatus.BAD_REQUEST));
            PainelRede p = ig.painel("tok", "178");
            assertEquals(120L, p.getSeguidores());
            assertEquals(350L, p.getViewsTotais());
            assertEquals(350L, p.getPosts().get(0).getViews());
            assertNull(p.getPosts().get(1).getViews());
            assertEquals("img", p.getPosts().get(1).getMiniatura());

            api.verify();
            api.reset();
            api.expect(requestTo("https://graph.instagram.com/v24.0/178/media"))
                    .andExpect(content().string(containsString("media_type=REELS")))
                    .andExpect(content().string(containsString("video_url=https%3A%2F%2Fapi%2Fmidia%2Fabc")))
                    .andRespond(withSuccess("{\"id\":\"cont1\"}", MediaType.APPLICATION_JSON));
            api.expect(requestTo(startsWith("https://graph.instagram.com/v24.0/cont1?fields=status_code")))
                    .andRespond(withSuccess("{\"status_code\":\"IN_PROGRESS\"}", MediaType.APPLICATION_JSON));
            api.expect(requestTo(startsWith("https://graph.instagram.com/v24.0/cont1?fields=status_code")))
                    .andRespond(withSuccess("{\"status_code\":\"FINISHED\"}", MediaType.APPLICATION_JSON));
            api.expect(requestTo("https://graph.instagram.com/v24.0/178/media_publish"))
                    .andRespond(withSuccess("{\"id\":\"post9\"}", MediaType.APPLICATION_JSON));
            api.expect(requestTo(startsWith("https://graph.instagram.com/v24.0/post9?fields=permalink")))
                    .andRespond(withSuccess("{\"permalink\":\"https://instagram.com/reel/9\"}", MediaType.APPLICATION_JSON));
            ResultadoPublicacao r = ig.publicar("tok", "178", videoDe(pasta, 5, false));
            assertEquals("post9", r.getIdExterno());
            assertEquals("https://instagram.com/reel/9", r.getLink());
            assertTrue(r.isPublico());
            assertNotNull(r.getAviso());
            assertEquals(1, pausas);
            api.verify();
        }

        @Test
        void erroAoProcessarSemUrlESemConfirmacao() {
            assertThrows(FalhaRedeException.class, () -> ig.publicar("t", "1",
                    new VideoParaPublicar(pasta, 1, "video/mp4", "t", "l", true, null)));
            api.verify();
            api.reset();
            api.expect(requestTo("https://graph.instagram.com/v24.0/1/media")).andRespond(withSuccess("{\"id\":\"c\"}", MediaType.APPLICATION_JSON));
            api.expect(requestTo(startsWith("https://graph.instagram.com/v24.0/c?fields=status_code")))
                    .andRespond(withSuccess("{\"status_code\":\"ERROR\",\"status\":\"formato\"}", MediaType.APPLICATION_JSON));
            assertTrue(assertThrows(FalhaRedeException.class, () -> ig.publicar("t", "1", videoDe(pasta, 1, true))).getMessage().contains("formato"));
            api.verify();
            api.reset();
            api.expect(requestTo("https://graph.instagram.com/v24.0/1/media")).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
            assertThrows(FalhaRedeException.class, () -> ig.publicar("t", "1", videoDe(pasta, 1, true)));
            api.verify();
            api.reset();
            api.expect(requestTo("https://api.instagram.com/oauth/access_token")).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
            assertThrows(FalhaRedeException.class, () -> ig.conectar("c", "r"));
            api.verify();
        }
    }

    @Nested
    class Tiktok {
        RestTemplate http = new RestTemplate();
        MockRestServiceServer api = MockRestServiceServer.bindTo(http).build();
        List<String> envios = new ArrayList<>();
        TiktokProvider tt = new TiktokProvider(http, "chave", "seg") {
            @Override
            protected RespostaEnvio enviarTrecho(String url, String metodo, Map<String, String> cab, Path arquivo, long inicio, long tamanho) {
                envios.add(cab.get("Content-Range") + " " + tamanho);
                return new RespostaEnvio(envios.size() == 1 ? 206 : 201, "", Optional.empty());
            }

            @Override
            protected void esperar(long ms) {
                // sem pausa no teste
            }
        };

        @Test
        void conectaRenovaPainelEPublicaPrivadoQuandoOAppNaoTemAuditoria() {
            assertTrue(tt.configurada());
            assertTrue(tt.urlAutorizacao("https://app/v", "st").contains("client_key=chave"));
            api.verify();
            api.reset();
            api.expect(requestTo("https://open.tiktokapis.com/v2/oauth/token/"))
                    .andRespond(withSuccess("{\"access_token\":\"A\",\"refresh_token\":\"R\",\"expires_in\":86400,\"open_id\":\"o1\"}",
                            MediaType.APPLICATION_JSON));
            api.expect(requestTo(startsWith("https://open.tiktokapis.com/v2/user/info/")))
                    .andRespond(withSuccess("{\"data\":{\"user\":{\"display_name\":\"Nord\",\"avatar_url\":\"av\"}},\"error\":{\"code\":\"ok\"}}",
                            MediaType.APPLICATION_JSON));
            TokensRede t = tt.conectar("c", "r");
            assertEquals("o1", t.getIdExterno());
            assertEquals("Nord", t.getNome());

            api.verify();
            api.reset();
            api.expect(requestTo("https://open.tiktokapis.com/v2/oauth/token/"))
                    .andRespond(withSuccess("{\"access_token\":\"A2\",\"expires_in\":86400}", MediaType.APPLICATION_JSON));
            TokensRede r = tt.renovar(t);
            assertEquals("A2", r.getTokenAcesso());
            assertEquals("R", r.getTokenRenovacao());
            assertThrows(FalhaRedeException.class, () -> tt.renovar(new TokensRede("a", null, null, "o", "n", null)));

            api.verify();
            api.reset();
            api.expect(requestTo(startsWith("https://open.tiktokapis.com/v2/user/info/")))
                    .andRespond(withSuccess("{\"data\":{\"user\":{\"follower_count\":15}},\"error\":{\"code\":\"ok\"}}", MediaType.APPLICATION_JSON));
            api.expect(requestTo(startsWith("https://open.tiktokapis.com/v2/video/list/")))
                    .andRespond(withSuccess("{\"data\":{\"videos\":[{\"id\":\"v1\",\"title\":\"T\",\"view_count\":90,\"create_time\":1700000000}]},"
                            + "\"error\":{\"code\":\"ok\"}}", MediaType.APPLICATION_JSON));
            PainelRede p = tt.painel("A2", "o1");
            assertEquals(15L, p.getSeguidores());
            assertEquals(90L, p.getViewsTotais());
            assertNotNull(p.getPosts().get(0).getData());

            long tamanho = 25L * 1024 * 1024;
            api.verify();
            api.reset();
            api.expect(requestTo("https://open.tiktokapis.com/v2/post/publish/creator_info/query/"))
                    .andRespond(withSuccess("{\"data\":{\"privacy_level_options\":[\"PUBLIC_TO_EVERYONE\",\"SELF_ONLY\"]},\"error\":{\"code\":\"ok\"}}",
                            MediaType.APPLICATION_JSON));
            api.expect(requestTo("https://open.tiktokapis.com/v2/post/publish/video/init/"))
                    .andExpect(content().string(containsString("PUBLIC_TO_EVERYONE")))
                    .andRespond(withStatus(HttpStatus.FORBIDDEN).body("{\"error\":{\"code\":\"unaudited_client_can_only_post_to_private_accounts\"}}")
                            .contentType(MediaType.APPLICATION_JSON));
            api.expect(requestTo("https://open.tiktokapis.com/v2/post/publish/video/init/"))
                    .andExpect(content().string(containsString("SELF_ONLY")))
                    .andExpect(content().string(containsString("\"total_chunk_count\":2")))
                    .andRespond(withSuccess("{\"data\":{\"publish_id\":\"pub1\",\"upload_url\":\"https://up\"},\"error\":{\"code\":\"ok\"}}",
                            MediaType.APPLICATION_JSON));
            api.expect(requestTo("https://open.tiktokapis.com/v2/post/publish/status/fetch/"))
                    .andRespond(withSuccess("{\"data\":{\"status\":\"PROCESSING_UPLOAD\"},\"error\":{\"code\":\"ok\"}}", MediaType.APPLICATION_JSON));
            api.expect(requestTo("https://open.tiktokapis.com/v2/post/publish/status/fetch/"))
                    .andRespond(withSuccess("{\"data\":{\"status\":\"PUBLISH_COMPLETE\"},\"error\":{\"code\":\"ok\"}}", MediaType.APPLICATION_JSON));
            ResultadoPublicacao res = tt.publicar("A2", "o1", videoDe(pasta, tamanho, true));
            assertFalse(res.isPublico());
            assertEquals("pub1", res.getIdExterno());
            assertTrue(res.getAviso().contains("auditoria"));
            long parte = 10L * 1024 * 1024;
            assertEquals(Arrays.asList("bytes 0-" + (parte - 1) + "/" + tamanho + " " + parte,
                    "bytes " + parte + "-" + (tamanho - 1) + "/" + tamanho + " " + (tamanho - parte)), envios);
            api.verify();
        }

        @Test
        void publicoQuandoPermitidoFalhaDoTiktokEErroComCodigo() {
            api.expect(requestTo("https://open.tiktokapis.com/v2/post/publish/creator_info/query/"))
                    .andRespond(withSuccess("{\"data\":{\"privacy_level_options\":[\"PUBLIC_TO_EVERYONE\"]},\"error\":{\"code\":\"ok\"}}",
                            MediaType.APPLICATION_JSON));
            api.expect(requestTo("https://open.tiktokapis.com/v2/post/publish/video/init/"))
                    .andExpect(content().string(containsString("\"total_chunk_count\":1")))
                    .andRespond(withSuccess("{\"data\":{\"publish_id\":\"p\",\"upload_url\":\"https://up\"},\"error\":{\"code\":\"ok\"}}",
                            MediaType.APPLICATION_JSON));
            api.expect(requestTo("https://open.tiktokapis.com/v2/post/publish/status/fetch/"))
                    .andRespond(withSuccess("{\"data\":{\"status\":\"PUBLISH_COMPLETE\",\"publicaly_available_post_id\":[\"7300\"]},"
                            + "\"error\":{\"code\":\"ok\"}}", MediaType.APPLICATION_JSON));
            ResultadoPublicacao r = tt.publicar("A", "o", videoDe(pasta, 1000, true));
            assertTrue(r.isPublico());
            assertEquals("7300", r.getIdExterno());
            assertNull(r.getAviso());

            api.verify();
            api.reset();
            api.expect(requestTo("https://open.tiktokapis.com/v2/post/publish/creator_info/query/"))
                    .andRespond(withSuccess("{\"data\":{\"privacy_level_options\":[\"SELF_ONLY\"]},\"error\":{\"code\":\"ok\"}}", MediaType.APPLICATION_JSON));
            api.expect(requestTo("https://open.tiktokapis.com/v2/post/publish/video/init/"))
                    .andRespond(withSuccess("{\"data\":{\"publish_id\":\"p\",\"upload_url\":\"https://up\"},\"error\":{\"code\":\"ok\"}}",
                            MediaType.APPLICATION_JSON));
            api.expect(requestTo("https://open.tiktokapis.com/v2/post/publish/status/fetch/"))
                    .andRespond(withSuccess("{\"data\":{\"status\":\"FAILED\",\"fail_reason\":\"duration_check\"},\"error\":{\"code\":\"ok\"}}",
                            MediaType.APPLICATION_JSON));
            assertTrue(assertThrows(FalhaRedeException.class, () -> tt.publicar("A", "o", videoDe(pasta, 1000, false)))
                    .getMessage().contains("duration_check"));

            api.verify();
            api.reset();
            api.expect(requestTo("https://open.tiktokapis.com/v2/post/publish/creator_info/query/"))
                    .andRespond(withSuccess("{\"error\":{\"code\":\"access_token_invalid\",\"message\":\"token inválido\"}}", MediaType.APPLICATION_JSON));
            assertEquals("TikTok recusou: token inválido", assertThrows(FalhaRedeException.class,
                    () -> tt.publicar("A", "o", videoDe(pasta, 1000, true))).getMessage());
            api.verify();
            api.reset();
            api.expect(requestTo("https://open.tiktokapis.com/v2/oauth/token/"))
                    .andRespond(withSuccess("{\"error\":\"invalid_request\",\"error_description\":\"code expirado\"}", MediaType.APPLICATION_JSON));
            assertEquals("TikTok recusou: code expirado", assertThrows(FalhaRedeException.class, () -> tt.conectar("c", "r")).getMessage());
            api.verify();
        }
    }
}
