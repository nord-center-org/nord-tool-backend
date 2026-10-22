package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.domain.CronogramaSemanal;
import br.com.nord_tool_backend.exception.AcessoNegadoException;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.repository.CronogramaSemanalRepository;
import br.com.nord_tool_backend.security.UsuarioAutenticado;
import br.com.nord_tool_backend.service.AutorizacaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Cronograma pessoal: só o dono compartilha e mexe nas exceções; compartilhado comigo é leitura. */
class CronogramaPessoalServiceImplTest {

    private CronogramaSemanalRepository repository;
    private CronogramaSemanalServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(CronogramaSemanalRepository.class);
        AutorizacaoService autorizacao = mock(AutorizacaoService.class);
        UsuarioAutenticado u = mock(UsuarioAutenticado.class);
        when(u.getId()).thenReturn(7L);
        when(autorizacao.exigir(any(), any())).thenReturn(u);
        service = new CronogramaSemanalServiceImpl(repository, autorizacao);
    }

    private static CronogramaSemanal item(Boolean proprio) {
        CronogramaSemanal c = new CronogramaSemanal();
        c.setId(1L);
        c.setInCronogramaFixo(true);
        c.setIdDiaSemana(1);
        c.setInProprio(proprio);
        return c;
    }

    @Test
    void compartilhaSemRepetirNemIncluirOProprioDono() {
        when(repository.buscarPorIdCronogramaSemanal(1L)).thenReturn(item(true));
        when(repository.contarUsuariosAtivos(any())).thenReturn(2);
        service.compartilhar(1L, Arrays.asList(8L, 9L, 8L, 7L, null));
        verify(repository).compartilhar(1L, Arrays.asList(8L, 9L));
    }

    @Test
    void listaVaziaDeixaDeCompartilhar() {
        when(repository.buscarPorIdCronogramaSemanal(1L)).thenReturn(item(true));
        service.compartilhar(1L, Collections.emptyList());
        verify(repository).compartilhar(1L, Collections.emptyList());
        verify(repository, never()).contarUsuariosAtivos(any());
    }

    @Test
    void pessoaInativaNaListaFalha() {
        when(repository.buscarPorIdCronogramaSemanal(1L)).thenReturn(item(true));
        when(repository.contarUsuariosAtivos(any())).thenReturn(1);
        assertThrows(EntradaInvalidaException.class, () -> service.compartilhar(1L, Arrays.asList(8L, 9L)));
        verify(repository, never()).compartilhar(anyLong(), anyList());
    }

    @Test
    void quemRecebeNaoCompartilhaNemMexeNasExcecoes() {
        when(repository.buscarPorIdCronogramaSemanal(1L)).thenReturn(item(false));
        assertThrows(AcessoNegadoException.class, () -> service.compartilhar(1L, Collections.singletonList(8L)));
        assertThrows(AcessoNegadoException.class, () -> service.incluirExcecao(1L, LocalDate.of(2026, 10, 21)));
        assertThrows(AcessoNegadoException.class, () -> service.removerExcecao(1L, LocalDate.of(2026, 10, 21)));
        verify(repository, never()).incluirExcecao(anyLong(), any());
        verify(repository, never()).removerExcecao(anyLong(), any());
    }

    @Test
    void dtoTrazDonoECompartilhados() {
        CronogramaSemanal c = item(false);
        c.setIdDono(3L);
        c.setNmDono("Ana");
        c.setTxCompartilhados("7,9");
        when(repository.buscarPorIdCronogramaSemanal(1L)).thenReturn(c);
        var dto = service.buscarPorIdCronogramaSemanal(1L);
        assertEquals(false, dto.getFlProprio());
        assertEquals("Ana", dto.getNmDono());
        assertEquals(List.of(7L, 9L), dto.getIdsCompartilhados());
        when(repository.buscarPorIdCronogramaSemanal(eq(2L))).thenReturn(new CronogramaSemanal());
        assertEquals(true, service.buscarPorIdCronogramaSemanal(2L).getFlProprio());
    }
}
