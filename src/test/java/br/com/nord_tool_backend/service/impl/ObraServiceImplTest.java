package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.domain.Fase;
import br.com.nord_tool_backend.domain.Obra;
import br.com.nord_tool_backend.dto.FaseDto;
import br.com.nord_tool_backend.dto.ObraDto;
import br.com.nord_tool_backend.exception.AcessoNegadoException;
import br.com.nord_tool_backend.exception.ConflitoException;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.exception.NaoEncontradoException;
import br.com.nord_tool_backend.form.FaseForm;
import br.com.nord_tool_backend.form.FaseUsuariosForm;
import br.com.nord_tool_backend.form.ObraForm;
import br.com.nord_tool_backend.repository.ObraRepository;
import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.service.AutorizacaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ObraServiceImplTest {

    private ObraRepository repository;
    private AutorizacaoService autorizacao;
    private ObraServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(ObraRepository.class);
        autorizacao = mock(AutorizacaoService.class);
        service = new ObraServiceImpl(repository, autorizacao);
        when(repository.usuariosPorFase()).thenReturn(new HashMap<>());
    }

    private static Obra obra(long id) {
        Obra o = new Obra();
        o.setId(id); o.setNmObra("Nord"); o.setCdAcesso("NORD2026"); o.setInAtivo(true); o.setNrVersao(1);
        return o;
    }

    private static Fase fase(long id, long idObra) {
        Fase f = new Fase();
        f.setId(id); f.setIdObra(idObra); f.setNmFase("Nord 1"); f.setCdFase("N1"); f.setNrOrdem(1); f.setInAtivo(true); f.setNrVersao(1);
        return f;
    }

    private static ObraForm obraForm() {
        ObraForm f = new ObraForm();
        f.setNmObra("  Nord ");
        f.setCdAcesso(" nord2026 ");
        return f;
    }

    private static FaseForm faseForm(String sigla) {
        FaseForm f = new FaseForm();
        f.setNmFase(" Nord 1 ");
        f.setCdFase(sigla);
        return f;
    }

    @Test
    void tudoExigeAdministracaoAdmin() {
        doThrow(new AcessoNegadoException("negado")).when(autorizacao).exigir(Modulo.ADMINISTRACAO, Acao.ADMIN);
        assertThrows(AcessoNegadoException.class, () -> service.listar());
        assertThrows(AcessoNegadoException.class, () -> service.criarObra(obraForm()));
        assertThrows(AcessoNegadoException.class, () -> service.criarFase(1L, faseForm("n1")));
        assertThrows(AcessoNegadoException.class, () -> service.definirUsuarios(1L, new FaseUsuariosForm()));
        verifyNoInteractions(repository);
    }

    @Test
    void listarAgrupaFasesEParticipantes() {
        when(repository.listarObras()).thenReturn(Arrays.asList(obra(1), obra(2)));
        when(repository.listarFases()).thenReturn(Arrays.asList(fase(10, 1), fase(11, 1)));
        Map<Long, List<Long>> part = new HashMap<>();
        part.put(10L, Arrays.asList(5L, 6L));
        when(repository.usuariosPorFase()).thenReturn(part);
        List<ObraDto> lista = service.listar();
        assertEquals(2, lista.get(0).getFases().size());
        assertEquals(Arrays.asList(5L, 6L), lista.get(0).getFases().get(0).getIdsUsuarios());
        assertEquals(0, lista.get(1).getFases().size());
    }

    @Test
    void criarObraAparaENormalizaOCodigo() {
        when(repository.inserirObra(any())).thenReturn(1L);
        when(repository.buscarObra(1L)).thenReturn(Optional.of(obra(1)));
        service.criarObra(obraForm());
        ArgumentCaptor<Obra> c = ArgumentCaptor.forClass(Obra.class);
        verify(repository).inserirObra(c.capture());
        assertEquals("Nord", c.getValue().getNmObra());
        assertEquals("NORD2026", c.getValue().getCdAcesso());
        assertEquals(Boolean.TRUE, c.getValue().getInAtivo());
    }

    @Test
    void alterarObraExigeVersaoEDetectaConflito() {
        assertThrows(EntradaInvalidaException.class, () -> service.alterarObra(1L, obraForm()));
        ObraForm f = obraForm();
        f.setNrVersao(1);
        when(repository.buscarObra(1L)).thenReturn(Optional.of(obra(1)));
        when(repository.alterarObra(any(), anyInt())).thenReturn(0);
        assertThrows(ConflitoException.class, () -> service.alterarObra(1L, f));
        when(repository.buscarObra(9L)).thenReturn(Optional.empty());
        assertThrows(NaoEncontradoException.class, () -> service.alterarObra(9L, f));
    }

    @Test
    void criarFaseNaObraComSiglaMaiusculaOuNula() {
        when(repository.buscarObra(1L)).thenReturn(Optional.of(obra(1)));
        when(repository.inserirFase(any())).thenReturn(10L);
        when(repository.buscarFase(10L)).thenReturn(Optional.of(fase(10, 1)));
        service.criarFase(1L, faseForm("n1"));
        service.criarFase(1L, faseForm("  "));
        ArgumentCaptor<Fase> c = ArgumentCaptor.forClass(Fase.class);
        verify(repository, org.mockito.Mockito.times(2)).inserirFase(c.capture());
        assertEquals("N1", c.getAllValues().get(0).getCdFase());
        assertEquals("Nord 1", c.getAllValues().get(0).getNmFase());
        assertEquals(1L, c.getAllValues().get(0).getIdObra());
        assertNull(c.getAllValues().get(1).getCdFase());
        when(repository.buscarObra(2L)).thenReturn(Optional.empty());
        assertThrows(NaoEncontradoException.class, () -> service.criarFase(2L, faseForm("x")));
    }

    @Test
    void alterarFaseMantemAObra() {
        FaseForm f = faseForm("n2");
        f.setNrVersao(1);
        when(repository.buscarFase(10L)).thenReturn(Optional.of(fase(10, 1)));
        when(repository.alterarFase(any(), anyInt())).thenReturn(1);
        FaseDto dto = service.alterarFase(10L, f);
        ArgumentCaptor<Fase> c = ArgumentCaptor.forClass(Fase.class);
        verify(repository).alterarFase(c.capture(), org.mockito.ArgumentMatchers.eq(1));
        assertEquals(1L, c.getValue().getIdObra());
        assertEquals(10L, dto.getId());
    }

    @Test
    void definirUsuariosSemRepetidosEValidaExistencia() {
        when(repository.buscarFase(10L)).thenReturn(Optional.of(fase(10, 1)));
        FaseUsuariosForm f = new FaseUsuariosForm();
        f.setIdsUsuarios(Arrays.asList(5L, 6L, 5L));
        when(repository.contarUsuarios(Arrays.asList(5L, 6L))).thenReturn(2);
        assertEquals(Arrays.asList(5L, 6L), service.definirUsuarios(10L, f).getIdsUsuarios());
        verify(repository).substituirUsuariosDaFase(10L, Arrays.asList(5L, 6L));

        f.setIdsUsuarios(Arrays.asList(5L, 99L));
        when(repository.contarUsuarios(Arrays.asList(5L, 99L))).thenReturn(1);
        assertThrows(EntradaInvalidaException.class, () -> service.definirUsuarios(10L, f));

        f.setIdsUsuarios(Collections.emptyList());
        assertEquals(0, service.definirUsuarios(10L, f).getIdsUsuarios().size());
        verify(repository, never()).substituirUsuariosDaFase(org.mockito.ArgumentMatchers.eq(11L), anyList());
    }
}
