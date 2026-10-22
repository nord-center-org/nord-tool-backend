package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.dto.MapeamentoDto;
import br.com.nord_tool_backend.dto.MapeamentoLinhaDto;
import br.com.nord_tool_backend.exception.AcessoNegadoException;
import br.com.nord_tool_backend.exception.ConflitoException;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.exception.NaoEncontradoException;
import br.com.nord_tool_backend.exception.NegocioException;
import br.com.nord_tool_backend.form.MapeamentoForm;
import br.com.nord_tool_backend.form.MapeamentoLinhaForm;
import br.com.nord_tool_backend.form.MapeamentoStatusForm;
import br.com.nord_tool_backend.repository.MapeamentoRepository;
import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.security.UsuarioAutenticado;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.service.ContextoFaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MapeamentoServiceImplTest {

    private AutorizacaoService autorizacao;
    private MapeamentoRepository repository;
    private ContextoFaseService contexto;
    private MapeamentoServiceImpl service;

    @BeforeEach
    void setUp() {
        autorizacao = mock(AutorizacaoService.class);
        repository = mock(MapeamentoRepository.class);
        contexto = mock(ContextoFaseService.class);
        service = new MapeamentoServiceImpl(repository, autorizacao, contexto);
        UsuarioAutenticado u = mock(UsuarioAutenticado.class);
        when(u.getId()).thenReturn(7L);
        when(autorizacao.exigir(any(), any())).thenReturn(u);
        when(contexto.faseParaCriar(null)).thenReturn(11L);
    }

    private static MapeamentoDto mapeamento(String status) {
        MapeamentoDto m = new MapeamentoDto();
        m.setId(1L);
        m.setCdStatus(status);
        m.setNrVersao(1);
        return m;
    }

    private static MapeamentoLinhaForm linha(String grupo, String item, String unidade, String projeto, String medida) {
        MapeamentoLinhaForm l = new MapeamentoLinhaForm();
        l.setNmGrupo(grupo);
        l.setNmItem(item);
        l.setNmUnidade(unidade);
        l.setQtProjeto(projeto == null ? null : new BigDecimal(projeto));
        l.setQtMedida(medida == null ? null : new BigDecimal(medida));
        return l;
    }

    private static MapeamentoForm form(String tipo) {
        MapeamentoForm f = new MapeamentoForm();
        f.setCdTipo(tipo);
        f.setNmTitulo(" Efetivo de hoje ");
        f.setDtMapeamento(LocalDate.of(2026, 10, 21));
        return f;
    }

    @Test
    void criarExigeEscritaETipoValido() {
        assertThrows(EntradaInvalidaException.class, () -> service.criar(form("PENDENCIA")));
        when(autorizacao.exigir(Modulo.MAPEAMENTOS, Acao.ESCRITA)).thenThrow(new AcessoNegadoException("negado"));
        assertThrows(AcessoNegadoException.class, () -> service.criar(form("CONTAGEM")));
        verify(repository, never()).inserir(any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void criarGravaNaFaseDoContextoComToleranciaPadraoELinhasNormalizadas() {
        when(repository.inserir(any(), eq(11L))).thenReturn(1L);
        when(repository.buscar(1L)).thenReturn(Optional.of(mapeamento("RASCUNHO")));
        MapeamentoForm f = form("contagem");
        f.setLinhas(Arrays.asList(linha(" Nord ", "Pedreiro", " ", null, "4"), linha(null, " Servente ", "un", null, "6")));
        service.criar(f);
        ArgumentCaptor<MapeamentoDto> m = ArgumentCaptor.forClass(MapeamentoDto.class);
        verify(repository).inserir(m.capture(), eq(11L));
        assertEquals("CONTAGEM", m.getValue().getCdTipo());
        assertEquals("Efetivo de hoje", m.getValue().getNmTitulo());
        assertEquals(0, new BigDecimal("5").compareTo(m.getValue().getPcTolerancia()));
        assertEquals(7L, m.getValue().getIdAutor());
        ArgumentCaptor<List<MapeamentoLinhaDto>> linhas = ArgumentCaptor.forClass(List.class);
        verify(repository).substituirLinhas(eq(1L), linhas.capture());
        assertEquals("Nord", linhas.getValue().get(0).getNmGrupo());
        assertEquals("un", linhas.getValue().get(0).getNmUnidade());
        assertEquals("Servente", linhas.getValue().get(1).getNmItem());
        assertNull(linhas.getValue().get(1).getNmGrupo());
        assertEquals(2, linhas.getValue().get(1).getNrOrdem());
    }

    @Test
    void concluidoNaoEditaEVersaoDivergenteE409() {
        MapeamentoForm f = form(null);
        assertThrows(EntradaInvalidaException.class, () -> service.alterar(1L, f));
        f.setNrVersao(1);
        when(repository.buscar(1L)).thenReturn(Optional.of(mapeamento("CONCLUIDO")));
        assertThrows(NegocioException.class, () -> service.alterar(1L, f));
        when(repository.buscar(1L)).thenReturn(Optional.of(mapeamento("RASCUNHO")));
        when(repository.alterar(any(), anyInt())).thenReturn(0);
        assertThrows(ConflitoException.class, () -> service.alterar(1L, f));
        verify(repository, never()).substituirLinhas(anyLong(), any());
    }

    @Test
    void linhasNulasNaoMexemNasLinhasEToleranciaAusenteMantemAAtual() {
        MapeamentoDto atual = mapeamento("RASCUNHO");
        atual.setPcTolerancia(new BigDecimal("3"));
        when(repository.buscar(1L)).thenReturn(Optional.of(atual));
        when(repository.alterar(any(), anyInt())).thenReturn(1);
        MapeamentoForm f = form(null);
        f.setNrVersao(1);
        service.alterar(1L, f);
        verify(repository, never()).substituirLinhas(anyLong(), any());
        ArgumentCaptor<MapeamentoDto> m = ArgumentCaptor.forClass(MapeamentoDto.class);
        verify(repository).alterar(m.capture(), eq(1));
        assertEquals(0, new BigDecimal("3").compareTo(m.getValue().getPcTolerancia()));
    }

    @Test
    void statusEExclusao() {
        MapeamentoStatusForm s = new MapeamentoStatusForm();
        s.setCdStatus("APROVADO");
        s.setNrVersao(1);
        assertThrows(EntradaInvalidaException.class, () -> service.alterarStatus(1L, s));
        when(repository.buscar(1L)).thenReturn(Optional.empty());
        s.setCdStatus("concluido");
        assertThrows(NaoEncontradoException.class, () -> service.alterarStatus(1L, s));
        when(repository.buscar(1L)).thenReturn(Optional.of(mapeamento("RASCUNHO")));
        when(repository.deletar(1L, 1)).thenReturn(0);
        assertThrows(ConflitoException.class, () -> service.excluir(1L, 1));
    }
}
