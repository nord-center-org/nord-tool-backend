package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.domain.EscadinhaTorre;
import br.com.nord_tool_backend.dto.EscadinhaDto;
import br.com.nord_tool_backend.dto.EscadinhaTorreDto;
import br.com.nord_tool_backend.exception.AcessoNegadoException;
import br.com.nord_tool_backend.exception.ConflitoException;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.exception.NaoEncontradoException;
import br.com.nord_tool_backend.form.EscadinhaModeloForm;
import br.com.nord_tool_backend.form.EscadinhaTorreForm;
import br.com.nord_tool_backend.repository.EscadinhaRepository;
import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.security.UsuarioAutenticado;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.service.ContextoFaseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EscadinhaServiceImplTest {

    private static final long OBRA = 4L;
    private EscadinhaRepository repository;
    private AutorizacaoService autorizacao;
    private ContextoFaseService contexto;
    private final ObjectMapper json = new ObjectMapper();
    private EscadinhaServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(EscadinhaRepository.class);
        autorizacao = mock(AutorizacaoService.class);
        contexto = mock(ContextoFaseService.class);
        service = new EscadinhaServiceImpl(repository, autorizacao, contexto, json);
        UsuarioAutenticado u = new UsuarioAutenticado(9L, "u@o.com", "USER", new EnumMap<>(Modulo.class), 0, Instant.now());
        when(autorizacao.exigir(any(), any())).thenReturn(u);
        when(contexto.obraEscolhida()).thenReturn(OBRA);
    }

    private static EscadinhaTorre torre(long id, int versao) {
        EscadinhaTorre t = new EscadinhaTorre();
        t.setIdTorre(id);
        t.setIdObra(OBRA);
        t.setNmTorre("Torre " + id);
        t.setNrOrdem(0);
        t.setTxConfig("{\"contorno\":[[0,0],[10,0],[10,5]]}");
        t.setTxCiclos("[{\"idPavimento\":\"p1\"}]");
        t.setTemPlanta(true);
        t.setNrVersao(versao);
        return t;
    }

    private EscadinhaTorreForm form(String planta, Integer versao) throws Exception {
        EscadinhaTorreForm f = new EscadinhaTorreForm();
        f.setNmTorre("  Torre A  ");
        f.setConfig(json.readTree("{\"contorno\":[[0,0],[1,1]]}"));
        f.setCiclos(json.readTree("[]"));
        f.setPlanta(planta);
        f.setNrVersao(versao);
        return f;
    }

    @Test
    void carregarDevolveTorresComJsonEModelo() {
        when(repository.listarTorres(OBRA)).thenReturn(Collections.singletonList(torre(1, 2)));
        when(repository.modelo(OBRA)).thenReturn(Optional.of("{\"atividades\":[\"Marcação\"]}"));
        EscadinhaDto d = service.carregar();
        verify(autorizacao).exigir(Modulo.ESCADINHA, Acao.LEITURA);
        assertEquals(1, d.getTorres().size());
        assertEquals(10, d.getTorres().get(0).getConfig().get("contorno").get(1).get(0).asInt());
        assertTrue(d.getTorres().get(0).getTemPlanta());
        assertEquals("Marcação", d.getModelo().get("atividades").get(0).asText());
    }

    @Test
    void semModeloVemNulo() {
        when(repository.listarTorres(OBRA)).thenReturn(Collections.emptyList());
        when(repository.modelo(OBRA)).thenReturn(Optional.empty());
        assertNull(service.carregar().getModelo());
    }

    @Test
    void criarTorreValidaNomeEPrancha() throws Exception {
        when(repository.contarTorres(OBRA)).thenReturn(2);
        when(repository.inserirTorre(eq(OBRA), anyString(), anyInt(), anyString(), anyString(), any(), anyLong())).thenReturn(7L);
        when(repository.buscarTorre(OBRA, 7L)).thenReturn(Optional.of(torre(7, 1)));
        EscadinhaTorreDto d = service.criarTorre(form("data:image/jpeg;base64,QUJD", null));
        verify(autorizacao).exigir(Modulo.ESCADINHA, Acao.ESCRITA);
        verify(repository).inserirTorre(OBRA, "Torre A", 2, "{\"contorno\":[[0,0],[1,1]]}", "[]", "data:image/jpeg;base64,QUJD", 9L);
        assertEquals(7L, d.getIdTorre());
        assertThrows(EntradaInvalidaException.class, () -> service.criarTorre(form("javascript:alert(1)", null)));
    }

    @Test
    void limiteDeTorresPorObra() throws Exception {
        when(repository.contarTorres(OBRA)).thenReturn(EscadinhaServiceImpl.MAX_TORRES);
        assertThrows(EntradaInvalidaException.class, () -> service.criarTorre(form(null, null)));
        verify(repository, never()).inserirTorre(anyLong(), anyString(), anyInt(), anyString(), anyString(), any(), anyLong());
    }

    @Test
    void alterarExigeVersaoEDetectaConflito() throws Exception {
        assertThrows(EntradaInvalidaException.class, () -> service.alterarTorre(1L, form(null, null)));
        when(repository.buscarTorre(OBRA, 1L)).thenReturn(Optional.of(torre(1, 3)));
        when(repository.alterarTorre(eq(OBRA), eq(1L), eq(3), anyString(), anyInt(), anyString(), anyString(), anyBoolean(), any(), anyLong())).thenReturn(0);
        assertThrows(ConflitoException.class, () -> service.alterarTorre(1L, form(null, 3)));
    }

    @Test
    void alterarSemPranchaMantemEVazioRemove() throws Exception {
        when(repository.buscarTorre(OBRA, 1L)).thenReturn(Optional.of(torre(1, 3)));
        when(repository.alterarTorre(eq(OBRA), eq(1L), eq(3), anyString(), anyInt(), anyString(), anyString(), anyBoolean(), any(), anyLong())).thenReturn(1);
        service.alterarTorre(1L, form(null, 3));
        verify(repository).alterarTorre(eq(OBRA), eq(1L), eq(3), eq("Torre A"), eq(0), anyString(), anyString(), eq(false), isNull(), eq(9L));
        service.alterarTorre(1L, form("", 3));
        verify(repository).alterarTorre(eq(OBRA), eq(1L), eq(3), eq("Torre A"), eq(0), anyString(), anyString(), eq(true), isNull(), eq(9L));
    }

    @Test
    void torreDeOutraObraNaoAparece() {
        when(repository.buscarTorre(OBRA, 5L)).thenReturn(Optional.empty());
        assertThrows(NaoEncontradoException.class, () -> service.planta(5L));
        when(repository.excluirTorre(OBRA, 5L)).thenReturn(0);
        assertThrows(NaoEncontradoException.class, () -> service.excluirTorre(5L));
    }

    @Test
    void configPrecisaSerObjetoOuLista() throws Exception {
        assertThrows(EntradaInvalidaException.class, () -> service.documento(json.readTree("\"texto\""), "config"));
        assertEquals("{}", service.documento(json.readTree("{}"), "config"));
    }

    @Test
    void salvarModeloGravaNaObra() throws Exception {
        when(repository.listarTorres(OBRA)).thenReturn(Collections.emptyList());
        when(repository.modelo(OBRA)).thenReturn(Optional.of("{\"dias\":7}"));
        EscadinhaModeloForm f = new EscadinhaModeloForm();
        f.setModelo(json.readTree("{\"dias\":7}"));
        EscadinhaDto d = service.salvarModelo(f);
        verify(repository).gravarModelo(OBRA, "{\"dias\":7}", 9L);
        assertEquals(7, d.getModelo().get("dias").asInt());
    }

    @Test
    void semPermissaoNaoLe() {
        when(autorizacao.exigir(Modulo.ESCADINHA, Acao.LEITURA)).thenThrow(new AcessoNegadoException("sem acesso"));
        assertThrows(AcessoNegadoException.class, () -> service.carregar());
        verify(repository, never()).listarTorres(anyLong());
    }
}
