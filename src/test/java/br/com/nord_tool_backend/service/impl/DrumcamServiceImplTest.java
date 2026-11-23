package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.dto.DrumcamBilheteDto;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DrumcamServiceImplTest {

    private static final class Relogio extends Clock {
        private Instant agora = Instant.parse("2026-11-23T03:00:00Z");

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
    private Relogio relogio;
    private DrumcamServiceImpl service;
    private long usuarioAtual = 1L;

    @BeforeEach
    void setUp() {
        AutorizacaoService autorizacao = mock(AutorizacaoService.class);
        when(autorizacao.exigir(Modulo.REPERTORIO, Acao.LEITURA)).thenAnswer(i -> new UsuarioAutenticado(usuarioAtual, "u@obra.com", "USUARIO",
            Collections.emptyMap(), 0, Instant.now()));
        relogio = new Relogio();
        service = new DrumcamServiceImpl(autorizacao, mapper, relogio, new SecureRandom());
    }

    private Canal conectar(long usuario, String papel, String nome, String id) {
        usuarioAtual = usuario;
        DrumcamBilheteDto b = service.bilhete(papel, nome);
        Canal c = new Canal(id);
        service.conectar(b.getBilhete(), c);
        return c;
    }

    private JsonNode ultima(Canal c) throws Exception {
        return mapper.readTree(c.recebidas.get(c.recebidas.size() - 1));
    }

    @Test
    void bilheteDeUsoUnicoEPapelValido() {
        DrumcamBilheteDto b = service.bilhete("camera", "Igreja");
        assertEquals("camera", b.getPapel());
        assertNotNull(b.getBilhete());
        Canal um = new Canal("a");
        service.conectar(b.getBilhete(), um);
        assertNull(um.fechadoPor);
        Canal dois = new Canal("b");
        service.conectar(b.getBilhete(), dois);
        assertNotNull(dois.fechadoPor, "o bilhete não serve duas vezes");
        Canal tres = new Canal("c");
        service.conectar(null, tres);
        assertNotNull(tres.fechadoPor);
        assertThrows(EntradaInvalidaException.class, () -> service.bilhete("tela", null));
    }

    @Test
    void bilheteVencidoFechaEALimpezaDescarta() {
        DrumcamBilheteDto b = service.bilhete("controle", null);
        relogio.andar(Duration.ofSeconds(61));
        Canal c = new Canal("x");
        service.conectar(b.getBilhete(), c);
        assertNotNull(c.fechadoPor);
        DrumcamBilheteDto outro = service.bilhete("controle", null);
        relogio.andar(Duration.ofSeconds(61));
        service.limpar();
        Canal d = new Canal("y");
        service.conectar(outro.getBilhete(), d);
        assertNotNull(d.fechadoPor);
    }

    @Test
    void controleVeAsCamerasDaPessoaEAListaAtualiza() throws Exception {
        Canal controle = conectar(1, "controle", null, "ctl");
        JsonNode lista = ultima(controle);
        assertEquals("cameras", lista.path("t").asText());
        assertEquals(0, lista.path("lista").size());
        Canal camera = conectar(1, "camera", "Igreja <b>\u0001", "cam");
        assertEquals("ok", mapper.readTree(camera.recebidas.get(0)).path("t").asText());
        lista = ultima(controle);
        assertEquals(1, lista.path("lista").size());
        assertEquals("cam", lista.path("lista").get(0).path("id").asText());
        assertEquals("Igreja b", lista.path("lista").get(0).path("nome").asText());
        // A câmera de outra pessoa não aparece.
        conectar(2, "camera", "", "cam2");
        assertEquals(1, ultima(controle).path("lista").size());
        service.desconectar(camera);
        assertEquals(0, ultima(controle).path("lista").size());
    }

    @Test
    void repassaPedidoERespostaSoEntreAMesmaPessoa() throws Exception {
        Canal camera = conectar(1, "camera", "Igreja", "cam");
        Canal controle = conectar(1, "controle", null, "ctl");
        Canal intruso = conectar(2, "controle", null, "ctl2");
        service.receber(controle, "{\"t\":\"req\",\"camera\":\"cam\",\"id\":7,\"m\":\"GET\",\"c\":\"estado\"}");
        JsonNode pedido = ultima(camera);
        assertEquals("req", pedido.path("t").asText());
        assertEquals("ctl", pedido.path("de").asText());
        assertEquals("estado", pedido.path("c").asText());
        assertTrue(pedido.path("camera").isMissingNode());
        service.receber(camera, "{\"t\":\"resp\",\"para\":\"ctl\",\"id\":7,\"s\":200,\"b\":\"e30=\"}");
        JsonNode resposta = ultima(controle);
        assertEquals(200, resposta.path("s").asInt());
        assertEquals("cam", resposta.path("camera").asText());
        assertTrue(resposta.path("para").isMissingNode());
        // O controle de outra pessoa não alcança a câmera, e a câmera não responde para ele.
        int antes = camera.recebidas.size();
        service.receber(intruso, "{\"t\":\"req\",\"camera\":\"cam\",\"id\":1,\"m\":\"POST\",\"c\":\"gravar\"}");
        assertEquals(antes, camera.recebidas.size());
        assertEquals(503, ultima(intruso).path("s").asInt());
        int doIntruso = intruso.recebidas.size();
        service.receber(camera, "{\"t\":\"resp\",\"para\":\"ctl2\",\"id\":1,\"s\":200}");
        assertEquals(doIntruso, intruso.recebidas.size());
    }

    @Test
    void ignoraLixoPapelErradoEDesconhecidos() {
        Canal camera = conectar(1, "camera", "Igreja", "cam");
        Canal controle = conectar(1, "controle", null, "ctl");
        int c = camera.recebidas.size(), k = controle.recebidas.size();
        service.receber(controle, "não é json");
        service.receber(controle, "[1,2]");
        service.receber(controle, "{\"t\":\"ping\"}");
        service.receber(camera, "{\"t\":\"req\",\"camera\":\"cam\",\"id\":1}");
        service.receber(controle, "{\"t\":\"resp\",\"para\":\"cam\"}");
        service.receber(new Canal("fantasma"), "{\"t\":\"req\"}");
        service.receber(camera, "{\"t\":\"resp\",\"para\":\"ninguem\",\"id\":1}");
        assertEquals(c, camera.recebidas.size());
        assertEquals(k, controle.recebidas.size());
        service.desconectar(new Canal("fantasma"));
        service.desconectar(controle);
    }

    @Test
    void mensagemGrandeFechaELimitePorSegundo() {
        Canal camera = conectar(1, "camera", "Igreja", "cam");
        Canal controle = conectar(1, "controle", null, "ctl");
        int antes = camera.recebidas.size();
        for (int i = 0; i < DrumcamServiceImpl.MAX_POR_SEGUNDO + 20; i++) {
            service.receber(controle, "{\"t\":\"req\",\"camera\":\"cam\",\"id\":" + i + "}");
        }
        assertEquals(antes + DrumcamServiceImpl.MAX_POR_SEGUNDO, camera.recebidas.size());
        relogio.andar(Duration.ofSeconds(1));
        service.receber(controle, "{\"t\":\"req\",\"camera\":\"cam\",\"id\":999}");
        assertEquals(antes + DrumcamServiceImpl.MAX_POR_SEGUNDO + 1, camera.recebidas.size());
        StringBuilder grande = new StringBuilder();
        while (grande.length() <= DrumcamServiceImpl.MAX_MENSAGEM) grande.append("xxxxxxxxxx");
        service.receber(controle, grande.toString());
        assertNotNull(controle.fechadoPor);
        service.receber(camera, null);
        assertNotNull(camera.fechadoPor);
    }

    @Test
    void limiteDeCamerasEControlesPorPessoaEDeBilhetes() {
        for (int i = 0; i < DrumcamServiceImpl.MAX_CAMERAS_POR_PESSOA; i++) assertNull(conectar(1, "camera", "C" + i, "c" + i).fechadoPor);
        assertNotNull(conectar(1, "camera", "Mais uma", "c-extra").fechadoPor);
        for (int i = 0; i < DrumcamServiceImpl.MAX_CONTROLES_POR_PESSOA; i++) assertNull(conectar(1, "controle", null, "k" + i).fechadoPor);
        assertNotNull(conectar(1, "controle", null, "k-extra").fechadoPor);
        for (int i = 0; i < DrumcamServiceImpl.MAX_BILHETES; i++) service.bilhete("controle", "x".repeat(80));
        assertThrows(EntradaInvalidaException.class, () -> service.bilhete("controle", null));
    }

    @Test
    void sinalDoWebrtcSoEntreAMesmaPessoa() throws Exception {
        Canal camera = conectar(1, "camera", "Palco", "cam");
        Canal controle = conectar(1, "controle", null, "ctl");
        Canal outro = conectar(1, "controle", null, "ctl-b");
        Canal intruso = conectar(2, "controle", null, "ctl2");
        // Controle → câmera.
        service.receber(controle, "{\"t\":\"sinal\",\"camera\":\"cam\",\"dados\":{\"tipo\":\"quero\"}}");
        JsonNode recebido = ultima(camera);
        assertEquals("sinal", recebido.path("t").asText());
        assertEquals("ctl", recebido.path("de").asText());
        assertEquals("quero", recebido.path("dados").path("tipo").asText());
        assertTrue(recebido.path("camera").isMissingNode());
        // Câmera → um controle.
        int doOutro = outro.recebidas.size();
        service.receber(camera, "{\"t\":\"sinal\",\"para\":\"ctl\",\"dados\":{\"tipo\":\"oferta\",\"sdp\":\"v=0\"}}");
        JsonNode oferta = ultima(controle);
        assertEquals("cam", oferta.path("camera").asText());
        assertEquals("oferta", oferta.path("dados").path("tipo").asText());
        assertTrue(oferta.path("para").isMissingNode());
        assertEquals(doOutro, outro.recebidas.size());
        // Câmera → todos os controles da pessoa ("*"), nunca o de outra pessoa.
        int doIntruso = intruso.recebidas.size();
        service.receber(camera, "{\"t\":\"sinal\",\"para\":\"*\",\"dados\":{\"tipo\":\"movimento\"}}");
        assertEquals("movimento", ultima(controle).path("dados").path("tipo").asText());
        assertEquals("movimento", ultima(outro).path("dados").path("tipo").asText());
        assertEquals(doIntruso, intruso.recebidas.size());
        // O intruso não alcança a câmera; câmera sumida não quebra.
        int daCamera = camera.recebidas.size();
        service.receber(intruso, "{\"t\":\"sinal\",\"camera\":\"cam\",\"dados\":{}}");
        service.receber(controle, "{\"t\":\"sinal\",\"camera\":\"sumiu\",\"dados\":{}}");
        assertEquals(daCamera, camera.recebidas.size());
        // Sinal grande demais é ignorado (mas não derruba a conexão).
        StringBuilder grande = new StringBuilder("{\"t\":\"sinal\",\"camera\":\"cam\",\"dados\":\"");
        while (grande.length() <= DrumcamServiceImpl.MAX_SINAL) grande.append("xxxxxxxxxx");
        grande.append("\"}");
        service.receber(controle, grande.toString());
        assertEquals(daCamera, camera.recebidas.size());
        assertNull(controle.fechadoPor);
    }

    @Test
    void cameraTrocaNomeETipoEAListaAtualiza() throws Exception {
        Canal controle = conectar(1, "controle", null, "ctl");
        Canal camera = conectar(1, "camera", "Celular", "cam");
        assertEquals("app", ultima(controle).path("lista").get(0).path("tipo").asText());
        service.receber(camera, "{\"t\":\"info\",\"nome\":\"Bateria <lado>\",\"tipo\":\"aovivo\"}");
        JsonNode lista = ultima(controle);
        assertEquals("Bateria lado", lista.path("lista").get(0).path("nome").asText());
        assertEquals("aovivo", lista.path("lista").get(0).path("tipo").asText());
        // Tipo desconhecido é ignorado; sem nome, o nome fica.
        service.receber(camera, "{\"t\":\"info\",\"tipo\":\"hacker\"}");
        assertEquals("aovivo", ultima(controle).path("lista").get(0).path("tipo").asText());
        assertEquals("Bateria lado", ultima(controle).path("lista").get(0).path("nome").asText());
        // Controle não manda "info".
        int k = controle.recebidas.size();
        service.receber(controle, "{\"t\":\"info\",\"nome\":\"x\"}");
        assertEquals(k, controle.recebidas.size());
    }
}
