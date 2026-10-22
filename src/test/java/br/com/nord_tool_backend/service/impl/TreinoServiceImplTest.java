package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.dto.TreinoDto;
import br.com.nord_tool_backend.dto.TreinoExercicioDto;
import br.com.nord_tool_backend.dto.TreinoSessaoDto;
import br.com.nord_tool_backend.dto.TreinoSessaoExercicioDto;
import br.com.nord_tool_backend.exception.ConflitoException;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.exception.NaoAutenticadoException;
import br.com.nord_tool_backend.exception.NaoEncontradoException;
import br.com.nord_tool_backend.form.TreinoExercicioForm;
import br.com.nord_tool_backend.form.TreinoForm;
import br.com.nord_tool_backend.form.TreinoSessaoForm;
import br.com.nord_tool_backend.repository.TreinoRepository;
import br.com.nord_tool_backend.security.UsuarioAutenticado;
import br.com.nord_tool_backend.service.AutorizacaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TreinoServiceImplTest {

    private static final Long EU = 7L;

    private TreinoRepository repository;
    private AutorizacaoService autorizacao;
    private TreinoServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(TreinoRepository.class);
        autorizacao = mock(AutorizacaoService.class);
        UsuarioAutenticado u = mock(UsuarioAutenticado.class);
        when(u.getId()).thenReturn(EU);
        when(autorizacao.exigirAutenticado()).thenReturn(u);
        service = new TreinoServiceImpl(repository, autorizacao);
    }

    private static TreinoDto treino(Long id) {
        TreinoDto t = new TreinoDto();
        t.setId(id);
        t.setNmTreino("Treino A");
        t.setNrOrdem(1);
        t.setNrVersao(1);
        return t;
    }

    private static TreinoExercicioDto exercicio(Long id, Long idTreino) {
        TreinoExercicioDto e = new TreinoExercicioDto();
        e.setId(id);
        e.setIdTreino(idTreino);
        e.setNmExercicio("Supino");
        return e;
    }

    private static TreinoExercicioForm exForm(Long id, String nome) {
        TreinoExercicioForm f = new TreinoExercicioForm();
        f.setId(id);
        f.setNmExercicio(nome);
        return f;
    }

    private static TreinoForm form(String nome) {
        TreinoForm f = new TreinoForm();
        f.setNmTreino(nome);
        return f;
    }

    @Test
    void semLoginNaoAcessaNada() {
        when(autorizacao.exigirAutenticado()).thenThrow(new NaoAutenticadoException("login"));
        assertThrows(NaoAutenticadoException.class, () -> service.listar());
        verifyNoInteractions(repository);
    }

    @Test
    void criarUsaODonoLogadoProximaOrdemESeriesPadrao() {
        when(repository.listar(EU)).thenReturn(Arrays.asList(treino(1L), treino(2L)));
        when(repository.inserir(eq(EU), any())).thenReturn(3L);
        when(repository.buscar(EU, 3L)).thenReturn(Optional.of(treino(3L)));
        TreinoForm f = form(" Treino C ");
        f.setExercicios(Arrays.asList(exForm(null, "Agachamento"), exForm(null, "Leg press")));
        service.criar(f);
        ArgumentCaptor<TreinoDto> t = ArgumentCaptor.forClass(TreinoDto.class);
        verify(repository).inserir(eq(EU), t.capture());
        assertEquals("Treino C", t.getValue().getNmTreino());
        assertEquals(3, t.getValue().getNrOrdem());
        ArgumentCaptor<TreinoExercicioDto> e = ArgumentCaptor.forClass(TreinoExercicioDto.class);
        verify(repository, times(2)).inserirExercicio(e.capture());
        assertEquals(3, e.getAllValues().get(0).getNrSeries());
        assertEquals(2, e.getAllValues().get(1).getNrOrdem());
    }

    @Test
    void editarMantemIdsExcluiOsQueSaemERecusaExercicioDeOutroTreino() {
        when(repository.buscar(EU, 1L)).thenReturn(Optional.of(treino(1L)));
        when(repository.alterar(eq(EU), any(), eq(1))).thenReturn(1);
        when(repository.listarExercicios(EU)).thenReturn(Arrays.asList(exercicio(10L, 1L), exercicio(11L, 1L), exercicio(20L, 2L)));
        TreinoForm f = form("Treino A");
        f.setNrVersao(1);
        f.setExercicios(Arrays.asList(exForm(10L, "Supino"), exForm(null, "Crucifixo")));
        service.alterar(1L, f);
        verify(repository).deletarExercicio(1L, 11L);
        verify(repository, never()).deletarExercicio(1L, 10L);
        verify(repository).alterarExercicio(any());
        verify(repository).inserirExercicio(any());

        TreinoForm intruso = form("Treino A");
        intruso.setNrVersao(1);
        intruso.setExercicios(Collections.singletonList(exForm(20L, "De outro")));
        assertThrows(EntradaInvalidaException.class, () -> service.alterar(1L, intruso));
    }

    @Test
    void edicaoSemVersaoOuComVersaoVelha() {
        assertThrows(EntradaInvalidaException.class, () -> service.alterar(1L, form("X")));
        when(repository.buscar(EU, 1L)).thenReturn(Optional.of(treino(1L)));
        when(repository.alterar(eq(EU), any(), anyInt())).thenReturn(0);
        TreinoForm f = form("X");
        f.setNrVersao(9);
        assertThrows(ConflitoException.class, () -> service.alterar(1L, f));
        verify(repository, never()).inserirExercicio(any());
    }

    @Test
    void treinoDeOutraPessoaNaoEEncontrado() {
        when(repository.buscar(EU, 5L)).thenReturn(Optional.empty());
        TreinoSessaoForm s = new TreinoSessaoForm();
        s.setIdTreino(5L);
        s.setDtSessao(LocalDate.of(2026, 10, 21));
        assertThrows(NaoEncontradoException.class, () -> service.registrarSessao(s));
        assertThrows(NaoEncontradoException.class, () -> service.excluir(5L, 1));
        verify(repository, never()).inserirSessao(anyLong(), any());
    }

    @Test
    void sessaoGuardaOQueFoiFeitoEAtualizaCargaSoDoQueFoiFeitoNesseTreino() {
        when(repository.buscar(EU, 1L)).thenReturn(Optional.of(treino(1L)));
        when(repository.listarExercicios(EU)).thenReturn(Arrays.asList(exercicio(10L, 1L), exercicio(11L, 1L), exercicio(20L, 2L)));
        when(repository.inserirSessao(eq(EU), any())).thenReturn(50L);
        TreinoSessaoDto salva = new TreinoSessaoDto();
        salva.setId(50L);
        when(repository.listarSessoes(EU, 200)).thenReturn(Collections.singletonList(salva));
        TreinoSessaoForm s = new TreinoSessaoForm();
        s.setIdTreino(1L);
        s.setDtSessao(LocalDate.of(2026, 10, 21));
        s.setAtualizarCargas(true);
        s.setExercicios(Arrays.asList(item(10L, "42.5", true), item(11L, "30", false), item(20L, "99", true), item(null, "10", true)));
        service.registrarSessao(s);
        ArgumentCaptor<TreinoSessaoDto> sessao = ArgumentCaptor.forClass(TreinoSessaoDto.class);
        verify(repository).inserirSessao(eq(EU), sessao.capture());
        assertEquals("Treino A", sessao.getValue().getNmTreino());
        ArgumentCaptor<TreinoSessaoExercicioDto> itens = ArgumentCaptor.forClass(TreinoSessaoExercicioDto.class);
        verify(repository, times(4)).inserirExercicioDaSessao(itens.capture());
        assertNull(itens.getAllValues().get(2).getIdExercicio(), "exercício de outro treino entra só pelo nome");
        verify(repository).atualizarCarga(EU, 10L, new BigDecimal("42.5"));
        verify(repository, times(1)).atualizarCarga(anyLong(), anyLong(), any());
    }

    @Test
    void historicoLimitaQuantidade() {
        when(repository.listarSessoes(EU, 200)).thenReturn(Collections.emptyList());
        when(repository.listarExerciciosDasSessoes(eq(EU), anyList())).thenReturn(Collections.emptyList());
        assertEquals(0, service.listarSessoes(5000).size());
        verify(repository).listarSessoes(EU, 200);
        service.listarSessoes(null);
        verify(repository).listarSessoes(EU, 30);
    }

    @Test
    void excluirSessaoQueNaoEMinha() {
        when(repository.deletarSessao(EU, 9L)).thenReturn(0);
        assertThrows(NaoEncontradoException.class, () -> service.excluirSessao(9L));
    }

    private static TreinoSessaoForm.Item item(Long idExercicio, String carga, boolean feito) {
        TreinoSessaoForm.Item i = new TreinoSessaoForm.Item();
        i.setIdExercicio(idExercicio);
        i.setNmExercicio("Ex " + idExercicio);
        i.setVlCarga(new BigDecimal(carga));
        i.setInFeito(feito);
        return i;
    }
}
