package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.dto.AvatarDto;
import br.com.nord_tool_backend.dto.GameSalaDto;
import br.com.nord_tool_backend.exception.ConflitoException;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.exception.NaoEncontradoException;
import br.com.nord_tool_backend.repository.AvatarRepository;
import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.security.UsuarioAutenticado;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.websocket.CanalSala;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GameSalaServiceImplTest {

    /** Relógio que o teste empurra. */
    private static final class Relogio extends Clock {
        private Instant agora = Instant.parse("2026-11-21T18:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return agora;
        }

        void andar(Duration d) {
            agora = agora.plus(d);
        }
    }

    /** Conexão falsa que guarda o que recebeu. */
    private static final class Canal implements CanalSala {
        private final String id;
        private final List<String> recebidas = new ArrayList<>();
        private String fechadoPor;

        Canal(String id) {
            this.id = id;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public void enviar(String json) {
            recebidas.add(json);
        }

        @Override
        public void fechar(String motivo) {
            fechadoPor = motivo;
        }
    }

    private final ObjectMapper mapper = new ObjectMapper();
    private AutorizacaoService autorizacao;
    private Relogio relogio;
    private GameSalaServiceImpl service;
    private long usuarioAtual;

    @BeforeEach
    void setUp() {
        autorizacao = mock(AutorizacaoService.class);
        when(autorizacao.exigir(Modulo.GAMES, Acao.LEITURA)).thenAnswer(i -> new UsuarioAutenticado(usuarioAtual, "u@obra.com", "USUARIO",
                Collections.emptyMap(), 0, Instant.now()));
        AvatarRepository avatares = mock(AvatarRepository.class);
        when(avatares.buscar(anyLong())).thenAnswer(i -> {
            Long id = i.getArgument(0);
            if (id == 99L) return Optional.empty();
            AvatarDto a = new AvatarDto();
            a.setIdUsuario(id);
            a.setNmNome("Pessoa " + id);
            a.setDsConfig("{\"pele\":" + id + "}");
            return Optional.of(a);
        });
        relogio = new Relogio();
        service = new GameSalaServiceImpl(autorizacao, avatares, mapper, relogio, new SecureRandom());
    }

    private GameSalaDto como(long usuario, java.util.function.Supplier<GameSalaDto> acao) {
        usuarioAtual = usuario;
        return acao.get();
    }

    private JsonNode ultima(Canal c) throws Exception {
        return mapper.readTree(c.recebidas.get(c.recebidas.size() - 1));
    }

    private Canal conectar(GameSalaDto ingresso, String id) {
        Canal c = new Canal(id);
        service.conectar(ingresso.getBilhete(), c);
        return c;
    }

    @Test
    void criarDaVagaZeroECodigoSemLetrasAmbiguas() {
        GameSalaDto s = como(1, () -> service.criar("bombas"));
        verify(autorizacao).exigir(Modulo.GAMES, Acao.LEITURA);
        assertEquals(0, s.getVaga());
        assertTrue(s.isAnfitriao());
        assertEquals("bombas", s.getJogo());
        assertTrue(s.getCodigo().matches("^[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{4}$"));
        assertTrue(s.getBilhete().length() >= 40);
        assertThrows(EntradaInvalidaException.class, () -> service.criar("Bombas!"));
        assertThrows(EntradaInvalidaException.class, () -> service.criar(null));
    }

    @Test
    void abrirOutraSalaFechaAAnterior() throws Exception {
        GameSalaDto a = como(1, () -> service.criar("bombas"));
        Canal tela = conectar(a, "t1");
        como(1, () -> service.criar("bombas"));
        assertEquals(1, service.salasAbertas());
        assertEquals("fechada", ultima(tela).path("t").asText());
        assertEquals("Quem hospeda abriu outra sala", tela.fechadoPor);
    }

    @Test
    void entrarDaAProximaVagaEAMesmaPessoaVoltaParaElaComCodigoEmMinusculas() throws Exception {
        GameSalaDto sala = como(1, () -> service.criar("bombas"));
        Canal tela = conectar(sala, "t");
        GameSalaDto p2 = como(2, () -> service.entrar(sala.getCodigo().toLowerCase()));
        assertEquals(1, p2.getVaga());
        assertFalse(p2.isAnfitriao());
        JsonNode lista = ultima(tela);
        assertEquals("jogadores", lista.path("t").asText());
        assertEquals("Pessoa 2", lista.path("jogadores").get(1).path("nome").asText());
        assertFalse(lista.path("jogadores").get(1).path("conectado").asBoolean());
        GameSalaDto volta = como(2, () -> service.entrar(" " + sala.getCodigo() + " "));
        assertEquals(1, volta.getVaga());
        assertNotEquals(p2.getBilhete(), volta.getBilhete());
        GameSalaDto anfitriaoVolta = como(1, () -> service.entrar(sala.getCodigo()));
        assertEquals(0, anfitriaoVolta.getVaga());
        assertTrue(anfitriaoVolta.isAnfitriao());
    }

    @Test
    void salaCheiaComOitoECodigoErrado() {
        GameSalaDto sala = como(1, () -> service.criar("bombas"));
        for (long u = 2; u <= 8; u++) como(u, () -> service.entrar(sala.getCodigo()));
        assertThrows(ConflitoException.class, () -> como(9, () -> service.entrar(sala.getCodigo())));
        assertThrows(EntradaInvalidaException.class, () -> service.entrar("AB"));
        assertThrows(EntradaInvalidaException.class, () -> service.entrar(null));
        assertThrows(NaoEncontradoException.class, () -> service.entrar("ZZZZ".equals(sala.getCodigo()) ? "YYYY" : "ZZZZ"));
    }

    @Test
    void pessoaSemAvatarEntraComoJogador() throws Exception {
        GameSalaDto sala = como(99, () -> service.criar("bombas"));
        Canal tela = conectar(sala, "t");
        JsonNode boasVindas = mapper.readTree(tela.recebidas.get(0));
        assertEquals("sala", boasVindas.path("t").asText());
        assertEquals(sala.getCodigo(), boasVindas.path("codigo").asText());
        assertTrue(boasVindas.path("anfitriao").asBoolean());
        assertEquals("Jogador", boasVindas.path("jogadores").get(0).path("nome").asText());
        assertTrue(boasVindas.path("jogadores").get(0).path("avatar").isNull());
    }

    @Test
    void bilheteEUsoUnicoEVence() {
        GameSalaDto sala = como(1, () -> service.criar("bombas"));
        Canal primeira = conectar(sala, "a");
        assertNull(primeira.fechadoPor);
        Canal repetida = conectar(sala, "b");
        assertEquals("Bilhete inválido ou vencido", repetida.fechadoPor);
        Canal semBilhete = new Canal("c");
        service.conectar(null, semBilhete);
        assertEquals("Bilhete inválido ou vencido", semBilhete.fechadoPor);
        GameSalaDto p2 = como(2, () -> service.entrar(sala.getCodigo()));
        relogio.andar(Duration.ofSeconds(61));
        Canal tarde = conectar(p2, "d");
        assertEquals("Bilhete inválido ou vencido", tarde.fechadoPor);
    }

    @Test
    void repassaEntradaDoCelularParaATelaEAvisosDaTelaParaOsCelulares() throws Exception {
        GameSalaDto sala = como(1, () -> service.criar("bombas"));
        Canal tela = conectar(sala, "t");
        Canal p2 = conectar(como(2, () -> service.entrar(sala.getCodigo())), "p2");
        Canal p3 = conectar(como(3, () -> service.entrar(sala.getCodigo())), "p3");
        assertTrue(ultima(tela).path("jogadores").get(2).path("conectado").asBoolean());

        service.receber(p2, "{\"t\":\"entrada\",\"d\":{\"direcao\":\"cima\",\"a\":true}}");
        JsonNode entrada = ultima(tela);
        assertEquals("entrada", entrada.path("t").asText());
        assertEquals(1, entrada.path("vaga").asInt());
        assertEquals("cima", entrada.path("d").path("direcao").asText());

        service.receber(tela, "{\"t\":\"todos\",\"d\":{\"fase\":\"jogando\"}}");
        assertEquals("jogando", ultima(p2).path("d").path("fase").asText());
        assertEquals("host", ultima(p3).path("t").asText());

        int antes = p2.recebidas.size();
        service.receber(tela, "{\"t\":\"para\",\"vaga\":2,\"d\":{\"vibrar\":80}}");
        assertEquals(80, ultima(p3).path("d").path("vibrar").asInt());
        assertEquals(antes, p2.recebidas.size());
        service.receber(tela, "{\"t\":\"para\",\"vaga\":0,\"d\":{}}");
        service.receber(tela, "{\"t\":\"para\",\"vaga\":7,\"d\":{}}");

        service.receber(p2, "{\"t\":\"ping\"}");
        assertEquals("pong", ultima(p2).path("t").asText());
    }

    @Test
    void celularNaoFalaComoTelaEMensagensRuinsVoltamErro() throws Exception {
        GameSalaDto sala = como(1, () -> service.criar("bombas"));
        Canal tela = conectar(sala, "t");
        Canal p2 = conectar(como(2, () -> service.entrar(sala.getCodigo())), "p2");
        service.receber(p2, "{\"t\":\"todos\",\"d\":{}}");
        assertEquals("erro", ultima(p2).path("t").asText());
        service.receber(tela, "{\"t\":\"qualquer\"}");
        assertEquals("erro", ultima(tela).path("t").asText());
        service.receber(p2, "{x");
        assertEquals("Mensagem inválida", ultima(p2).path("m").asText());
        service.receber(p2, "[1]");
        assertEquals("Mensagem inválida", ultima(p2).path("m").asText());
        char[] grande = new char[GameSalaServiceImpl.MAX_MENSAGEM + 1];
        java.util.Arrays.fill(grande, 'a');
        service.receber(p2, new String(grande));
        assertEquals("Mensagem grande demais", ultima(p2).path("m").asText());
        int antes = tela.recebidas.size();
        service.receber(new Canal("desconhecido"), "{\"t\":\"entrada\"}");
        service.receber(p2, null);
        assertEquals(antes, tela.recebidas.size());
    }

    @Test
    void limitaMensagensPorSegundo() {
        GameSalaDto sala = como(1, () -> service.criar("bombas"));
        Canal tela = conectar(sala, "t");
        Canal p2 = conectar(como(2, () -> service.entrar(sala.getCodigo())), "p2");
        int antes = tela.recebidas.size();
        for (int i = 0; i < GameSalaServiceImpl.MAX_POR_SEGUNDO + 20; i++) service.receber(p2, "{\"t\":\"entrada\",\"d\":{}}");
        assertEquals(antes + GameSalaServiceImpl.MAX_POR_SEGUNDO, tela.recebidas.size());
        relogio.andar(Duration.ofSeconds(1));
        service.receber(p2, "{\"t\":\"entrada\",\"d\":{}}");
        assertEquals(antes + GameSalaServiceImpl.MAX_POR_SEGUNDO + 1, tela.recebidas.size());
    }

    @Test
    void entradaSemTelaConectadaSePerdeSemErro() {
        GameSalaDto sala = como(1, () -> service.criar("bombas"));
        Canal p2 = conectar(como(2, () -> service.entrar(sala.getCodigo())), "p2");
        int antes = p2.recebidas.size();
        service.receber(p2, "{\"t\":\"entrada\",\"d\":{}}");
        assertEquals(antes, p2.recebidas.size());
    }

    @Test
    void quemCaiGuardaAVagaEOutroAparelhoDerrubaOAnterior() throws Exception {
        GameSalaDto sala = como(1, () -> service.criar("bombas"));
        Canal tela = conectar(sala, "t");
        Canal p2 = conectar(como(2, () -> service.entrar(sala.getCodigo())), "p2");
        service.desconectar(p2);
        assertFalse(ultima(tela).path("jogadores").get(1).path("conectado").asBoolean());
        service.desconectar(p2);
        Canal p2b = conectar(como(2, () -> service.entrar(sala.getCodigo())), "p2b");
        assertTrue(ultima(tela).path("jogadores").get(1).path("conectado").asBoolean());
        Canal p2c = conectar(como(2, () -> service.entrar(sala.getCodigo())), "p2c");
        assertEquals("Conectado em outro aparelho", p2b.fechadoPor);
        service.desconectar(p2b);
        assertTrue(ultima(tela).path("jogadores").get(1).path("conectado").asBoolean());
        assertNull(p2c.fechadoPor);
    }

    @Test
    void celularSaiELiberaAVaga() throws Exception {
        GameSalaDto sala = como(1, () -> service.criar("bombas"));
        Canal tela = conectar(sala, "t");
        Canal p2 = conectar(como(2, () -> service.entrar(sala.getCodigo())), "p2");
        service.receber(p2, "{\"t\":\"sair\"}");
        assertEquals("Saiu da sala", p2.fechadoPor);
        assertEquals(1, ultima(tela).path("jogadores").size());
        assertEquals(1, como(3, () -> service.entrar(sala.getCodigo())).getVaga());
    }

    @Test
    void telaFechaASala() throws Exception {
        GameSalaDto sala = como(1, () -> service.criar("bombas"));
        Canal tela = conectar(sala, "t");
        Canal p2 = conectar(como(2, () -> service.entrar(sala.getCodigo())), "p2");
        como(3, () -> service.entrar(sala.getCodigo()));
        service.receber(tela, "{\"t\":\"fechar\"}");
        assertEquals("fechada", ultima(p2).path("t").asText());
        assertEquals("Quem hospeda fechou a sala", p2.fechadoPor);
        assertEquals(0, service.salasAbertas());
        assertTrue(service.bilhetesGuardados().isEmpty());
    }

    @Test
    void limpezaFechaSalaVaziaOciosaEAntigaEApagaBilhetesVencidos() {
        GameSalaDto vazia = como(1, () -> service.criar("bombas"));
        GameSalaDto ativa = como(2, () -> service.criar("bombas"));
        Canal tela = conectar(ativa, "t");
        assertEquals(1, service.bilhetesGuardados().size());
        relogio.andar(Duration.ofMinutes(5));
        service.limpar();
        assertEquals(2, service.salasAbertas());
        assertTrue(service.bilhetesGuardados().isEmpty());
        relogio.andar(Duration.ofMinutes(6));
        service.limpar();
        assertEquals(1, service.salasAbertas());
        assertThrows(NaoEncontradoException.class, () -> como(3, () -> service.entrar(vazia.getCodigo())));
        relogio.andar(Duration.ofHours(6));
        service.limpar();
        assertEquals(0, service.salasAbertas());
        assertEquals("Sala encerrada por tempo", tela.fechadoPor);
    }

    @Test
    void recusaQuandoHaSalasDemais() {
        for (long u = 1; u <= GameSalaServiceImpl.MAX_SALAS; u++) como(u + 1000, () -> service.criar("bombas"));
        assertThrows(ConflitoException.class, () -> como(1, () -> service.criar("bombas")));
    }
}
