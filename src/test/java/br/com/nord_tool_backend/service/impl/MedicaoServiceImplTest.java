package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.domain.Medicao;
import br.com.nord_tool_backend.domain.MedicaoStatus;
import br.com.nord_tool_backend.dto.MedicaoDto;
import br.com.nord_tool_backend.dto.MedicaoListaDto;
import br.com.nord_tool_backend.exception.AcessoNegadoException;
import br.com.nord_tool_backend.exception.ConflitoException;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.exception.NaoEncontradoException;
import br.com.nord_tool_backend.form.MedicaoForm;
import br.com.nord_tool_backend.form.MedicaoStatusForm;
import br.com.nord_tool_backend.repository.MedicaoRepository;
import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.service.AutorizacaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MedicaoServiceImplTest {

    private AutorizacaoService autorizacao;
    private MedicaoRepository repository;
    private MedicaoServiceImpl service;

    @BeforeEach
    void setUp() {
        autorizacao = mock(AutorizacaoService.class);
        repository = mock(MedicaoRepository.class);
        service = new MedicaoServiceImpl(repository, autorizacao);
        when(repository.listarStatus()).thenReturn(Arrays.asList(
                status("PENDENTE", true), status("FEITA", true), status("ANTIGO", false)));
    }

    private static MedicaoStatus status(String cd, boolean ativo) {
        MedicaoStatus s = new MedicaoStatus();
        s.setCdStatus(cd);
        s.setNmStatus(cd.substring(0, 1) + cd.substring(1).toLowerCase());
        s.setNrOrdem(1);
        s.setInAtivo(ativo);
        return s;
    }

    private static Medicao medicao(long id, int versao, String cdStatus) {
        Medicao m = new Medicao();
        m.setId(id);
        m.setNrMedicao(11);
        m.setCdContrato("34515");
        m.setNmRazaoSocial("CLEAN XPRESS LTDA");
        m.setNrContrato("CT/4916-209");
        m.setDtMedicao(LocalDate.of(2026, 10, 1));
        m.setVlMedicao(new BigDecimal("14000.05"));
        m.setCdStatus(cdStatus);
        m.setNmStatus(cdStatus);
        m.setNrVersao(versao);
        return m;
    }

    private static MedicaoForm form() {
        MedicaoForm f = new MedicaoForm();
        f.setNrMedicao(11);
        f.setCdContrato(" 34515 ");
        f.setNmRazaoSocial(" CLEAN XPRESS LTDA ");
        f.setNrContrato(" CT/4916-209 ");
        f.setDtMedicao(LocalDate.of(2026, 10, 1));
        f.setVlMedicao(new BigDecimal("14000.05"));
        f.setTxMemorial("   ");
        return f;
    }

    // ---------- permissões ----------

    @Test
    void listarExigeLeituraEEscritaExigeEscrita() {
        service.listar();
        verify(autorizacao).exigir(Modulo.MEDICOES, Acao.LEITURA);

        service.criar(formComInsercao());
        verify(autorizacao).exigir(Modulo.MEDICOES, Acao.ESCRITA);
    }

    @Test
    void semPermissaoNaoTocaNoRepositorio() {
        doThrow(new AcessoNegadoException("negado")).when(autorizacao).exigir(any(), any());
        assertThrows(AcessoNegadoException.class, () -> service.listar());
        assertThrows(AcessoNegadoException.class, () -> service.criar(form()));
        assertThrows(AcessoNegadoException.class, () -> service.alterar(1L, form()));
        assertThrows(AcessoNegadoException.class, () -> service.alterarStatus(1L, new MedicaoStatusForm()));
        assertThrows(AcessoNegadoException.class, () -> service.excluir(1L, 1));
        verifyNoInteractions(repository);
    }

    private MedicaoForm formComInsercao() {
        when(repository.inserir(any())).thenReturn(9L);
        when(repository.buscar(9L)).thenReturn(Optional.of(medicao(9, 1, "PENDENTE")));
        return form();
    }

    // ---------- listagem ----------

    @Test
    void listarDevolveMedicoesEStatus() {
        when(repository.listar()).thenReturn(Arrays.asList(medicao(1, 1, "FEITA")));
        MedicaoListaDto lista = service.listar();
        assertEquals(1, lista.getMedicoes().size());
        assertEquals(3, lista.getStatus().size());
        assertEquals(false, lista.getStatus().get(2).isInAtivo());
    }

    // ---------- criação ----------

    @Test
    void criaPendenteComTextosAparadosEMemorialVazioVirandoNulo() {
        MedicaoDto dto = service.criar(formComInsercao());

        assertEquals(9L, dto.getId());
        ArgumentCaptor<Medicao> c = ArgumentCaptor.forClass(Medicao.class);
        verify(repository).inserir(c.capture());
        assertEquals("PENDENTE", c.getValue().getCdStatus());
        assertEquals("34515", c.getValue().getCdContrato());
        assertEquals("CLEAN XPRESS LTDA", c.getValue().getNmRazaoSocial());
        assertEquals("CT/4916-209", c.getValue().getNrContrato());
        assertNull(c.getValue().getTxMemorial());
    }

    @Test
    void criaComStatusInformadoSemDiferenciarCaixa() {
        MedicaoForm f = formComInsercao();
        f.setCdStatus("feita");
        service.criar(f);
        ArgumentCaptor<Medicao> c = ArgumentCaptor.forClass(Medicao.class);
        verify(repository).inserir(c.capture());
        assertEquals("FEITA", c.getValue().getCdStatus());
    }

    @Test
    void naoCriaComStatusInexistenteOuInativo() {
        MedicaoForm inexistente = form();
        inexistente.setCdStatus("XYZ");
        assertThrows(EntradaInvalidaException.class, () -> service.criar(inexistente));
        MedicaoForm inativo = form();
        inativo.setCdStatus("ANTIGO");
        assertThrows(EntradaInvalidaException.class, () -> service.criar(inativo));
        verify(repository, never()).inserir(any());
    }

    // ---------- alteração ----------

    @Test
    void alterarExigeVersao() {
        assertThrows(EntradaInvalidaException.class, () -> service.alterar(1L, form()));
        verify(repository, never()).alterar(any(), anyInt());
    }

    @Test
    void alterarInexistenteDa404() {
        MedicaoForm f = form();
        f.setNrVersao(1);
        when(repository.buscar(1L)).thenReturn(Optional.empty());
        assertThrows(NaoEncontradoException.class, () -> service.alterar(1L, f));
    }

    @Test
    void alterarComVersaoDivergenteDaConflito() {
        MedicaoForm f = form();
        f.setNrVersao(1);
        when(repository.buscar(1L)).thenReturn(Optional.of(medicao(1, 2, "PENDENTE")));
        when(repository.alterar(any(), anyInt())).thenReturn(0);
        assertThrows(ConflitoException.class, () -> service.alterar(1L, f));
    }

    @Test
    void alterarSemStatusMantemOAtualMesmoInativo() {
        MedicaoForm f = form();
        f.setNrVersao(3);
        when(repository.buscar(1L)).thenReturn(Optional.of(medicao(1, 3, "ANTIGO")));
        when(repository.alterar(any(), anyInt())).thenReturn(1);

        service.alterar(1L, f);

        ArgumentCaptor<Medicao> c = ArgumentCaptor.forClass(Medicao.class);
        verify(repository).alterar(c.capture(), org.mockito.ArgumentMatchers.eq(3));
        assertEquals("ANTIGO", c.getValue().getCdStatus());
        assertEquals(1L, c.getValue().getId());
    }

    @Test
    void alterarParaStatusInativoNaoPermite() {
        MedicaoForm f = form();
        f.setNrVersao(1);
        f.setCdStatus("ANTIGO");
        when(repository.buscar(1L)).thenReturn(Optional.of(medicao(1, 1, "PENDENTE")));
        assertThrows(EntradaInvalidaException.class, () -> service.alterar(1L, f));
    }

    // ---------- status ----------

    @Test
    void alterarStatusGravaOCodigoCanonico() {
        MedicaoStatusForm f = new MedicaoStatusForm();
        f.setCdStatus("feita");
        f.setNrVersao(2);
        when(repository.buscar(1L)).thenReturn(Optional.of(medicao(1, 3, "FEITA")));
        when(repository.alterarStatus(1L, "FEITA", 2)).thenReturn(1);

        assertEquals("FEITA", service.alterarStatus(1L, f).getCdStatus());
        verify(repository).alterarStatus(1L, "FEITA", 2);
    }

    @Test
    void alterarStatusComVersaoDivergenteDaConflito() {
        MedicaoStatusForm f = new MedicaoStatusForm();
        f.setCdStatus("FEITA");
        f.setNrVersao(1);
        when(repository.buscar(1L)).thenReturn(Optional.of(medicao(1, 2, "PENDENTE")));
        when(repository.alterarStatus(anyLong(), anyString(), anyInt())).thenReturn(0);
        assertThrows(ConflitoException.class, () -> service.alterarStatus(1L, f));
    }

    // ---------- exclusão ----------

    @Test
    void excluirExigeVersaoEDetectaConflito() {
        assertThrows(EntradaInvalidaException.class, () -> service.excluir(1L, null));
        when(repository.buscar(1L)).thenReturn(Optional.of(medicao(1, 2, "PENDENTE")));
        when(repository.deletar(1L, 1)).thenReturn(0);
        assertThrows(ConflitoException.class, () -> service.excluir(1L, 1));
        when(repository.deletar(1L, 2)).thenReturn(1);
        service.excluir(1L, 2);
        verify(repository).deletar(1L, 2);
    }
}
