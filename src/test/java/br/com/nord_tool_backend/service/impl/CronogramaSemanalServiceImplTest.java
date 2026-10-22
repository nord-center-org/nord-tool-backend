package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.domain.CronogramaSemanal;
import br.com.nord_tool_backend.dto.CronogramaSemanalDto;
import br.com.nord_tool_backend.form.CronogramaSemanalForm;
import br.com.nord_tool_backend.repository.CronogramaSemanalRepository;
import br.com.nord_tool_backend.service.CronogramaSemanalService;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.security.UsuarioAutenticado;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.exception.AcessoNegadoException;
import br.com.nord_tool_backend.exception.NaoAutenticadoException;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
public class CronogramaSemanalServiceImplTest {

    @Mock

    private AutorizacaoService autorizacao;


    @InjectMocks
    private CronogramaSemanalServiceImpl cronogramaSemanalService;

    @Mock
    private CronogramaSemanalRepository cronogramaSemanalRepository;

    CronogramaSemanal cronogramaSemanal = new CronogramaSemanal();
    CronogramaSemanalDto cronogramaSemanalDto = new CronogramaSemanalDto();
    List<CronogramaSemanalDto> lsCronogramaSemanalDto = new ArrayList<>();
    List<CronogramaSemanal> lsCronogramaSemanal = new ArrayList<>();
    CronogramaSemanalForm cronogramaSemanalForm = new CronogramaSemanalForm();

    @BeforeEach
    public void setup(){
        cronogramaSemanal = CronogramaSemanal.builder()
                .id(1L)
                .idDiaSemana(1)
                .nmDiaSemana("nmDiaSemana")
                .nmCronogramaSemanal("nmCronogramaSemanal")
                .nmHorario("nmHorario")
                .nmCronogramaCategoria("nmCategoria")
                .nmStatusCronograma("Pendente")
                .txObservacao("txObservacao")
                .dtPrazo(LocalDateTime.now())
                .dtFinalizacao(LocalDateTime.now())
                .build();

        cronogramaSemanalDto = CronogramaSemanalDto.builder()
                .id(cronogramaSemanal.getId())
                .idDiaSemana(1)
                .nmDiaSemana("nmDiaSemana")
                .nmCronogramaSemanal("nmCronogramaSemanal")
                .nmHorario("nmHorario")
                .nmCategoria("nmCategoria")
                .nmStatusCronograma("nmStatusCronograma")
                .txObservacao("txObservacao")
                .dtPrazo(LocalDateTime.now())
                .dtFinalizacao(LocalDateTime.now())
                .build();

        lsCronogramaSemanal.add(CronogramaSemanal.builder()
                .id(1L)
                .idDiaSemana(1)
                .nmDiaSemana("nmDiaSemana")
                .nmCronogramaSemanal("nmCronogramaSemanal")
                .nmHorario("nmHorario")
                .nmCronogramaCategoria("nmCategoria")
                .nmStatusCronograma("nmStatusCronograma")
                .txObservacao("txObservacao")
                .dtPrazo(LocalDateTime.now())
                .dtFinalizacao(LocalDateTime.now())
                .build());

        lsCronogramaSemanalDto.add(CronogramaSemanalDto.builder()
                .id(1L)
                .idDiaSemana(1)
                .nmDiaSemana("nmDiaSemana")
                .nmCronogramaSemanal("nmCronogramaSemanal")
                .nmHorario("nmHorario")
                .nmCategoria("nmCategoria")
                .nmStatusCronograma("nmStatusCronograma")
                .txObservacao("txObservacao")
                .dtPrazo(LocalDateTime.now())
                .dtFinalizacao(LocalDateTime.now())
                .build());

        cronogramaSemanalForm = CronogramaSemanalForm.builder()
                .idCronogramaSemanal(1L)
                .idDiaSemana(1)
                .nmCronogramaSemanal("nmCronogramaSemanal")
                .nmHorario("nmHorario")
                .nmCategoria("nmCategoria")
                .nmStatusCronograma("nmStatusCronograma")
                .txObservacao("txObservacao")
                .dtPrazo(LocalDateTime.now())
                .dtFinalizacao(LocalDateTime.now())
                .build();
    }

    @Test
    void deveSalvarCronogramaSemanal() {
        when(cronogramaSemanalRepository.salvarCronogramaSemanal(any(CronogramaSemanal.class))).thenReturn(cronogramaSemanalDto);
        CronogramaSemanalDto result = cronogramaSemanalService.salvarCronogramaSemanal(cronogramaSemanalForm);
        assertEquals(cronogramaSemanalDto, result);
        verify(cronogramaSemanalRepository).salvarCronogramaSemanal(any(CronogramaSemanal.class));
    }

    @Test
    void deveAlterarCronogramaSemanal() {
        when(cronogramaSemanalRepository.alterarCronogramaSemanal(any(CronogramaSemanal.class))).thenReturn(cronogramaSemanalDto);
        CronogramaSemanalDto result = cronogramaSemanalService.alterarCronogramaSemanal(cronogramaSemanalForm);
        assertEquals(cronogramaSemanalDto, result);
        verify(cronogramaSemanalRepository).alterarCronogramaSemanal(any(CronogramaSemanal.class));
    }

    @Test
    void deveDeletarCronogramaSemanal(){
        doNothing().when(cronogramaSemanalRepository).deletarCronogramaSemanal(anyLong());
        cronogramaSemanalService.deletarCronogramaSemanal(1L);
        verify(cronogramaSemanalRepository, times(1)).deletarCronogramaSemanal(anyLong());
    }

    @Test
    void deveRetornarUmCronogramaSemanal(){
        when(cronogramaSemanalRepository.buscarPorIdCronogramaSemanal(anyLong())).thenReturn(cronogramaSemanal);
        cronogramaSemanalService.buscarPorIdCronogramaSemanal(1L);
        verify(cronogramaSemanalRepository, times(1)).buscarPorIdCronogramaSemanal(anyLong());
    }

    @Test
    void deveRetornarUmaListaCronogramaSemanal(){
        when(cronogramaSemanalRepository.listarCronogramaSemanal()).thenReturn(lsCronogramaSemanal);
        cronogramaSemanalService.listarCronogramaSemanal();
        verify(cronogramaSemanalRepository, times(1)).listarCronogramaSemanal();
    }

    // ---------- autorização ----------

    @Test
    void semPermissaoDeLeituraDoModuloNaoConsultaODado() {
        when(autorizacao.exigir(Modulo.CRONOGRAMA, Acao.LEITURA)).thenThrow(new AcessoNegadoException("Acesso negado"));
        assertThrows(AcessoNegadoException.class, () -> cronogramaSemanalService.listarCronogramaSemanal());
        verifyNoInteractions(cronogramaSemanalRepository);
    }

    @Test
    void semPermissaoDeEscritaDoModuloNaoAlteraODado() {
        when(autorizacao.exigir(Modulo.CRONOGRAMA, Acao.ESCRITA)).thenThrow(new AcessoNegadoException("Acesso negado"));
        assertThrows(AcessoNegadoException.class, () -> cronogramaSemanalService.deletarCronogramaSemanal(1L));
        verifyNoInteractions(cronogramaSemanalRepository);
    }

    /** Cronograma pessoal: só o dono compartilha e mexe nas exceções; compartilhado comigo é leitura. */
    @org.junit.jupiter.api.Nested
    class Pessoal {


        private CronogramaSemanalRepository repository;
        private CronogramaSemanalServiceImpl service;

        @BeforeEach
        void setUp() {
            repository = mock(CronogramaSemanalRepository.class);
            AutorizacaoService autorizacao = mock(AutorizacaoService.class);
            UsuarioAutenticado u = mock(UsuarioAutenticado.class);
            lenient().when(u.getId()).thenReturn(7L);
            lenient().when(autorizacao.exigir(any(), any())).thenReturn(u);
            service = new CronogramaSemanalServiceImpl(repository, autorizacao);
        }

        private CronogramaSemanal item(Boolean proprio) {
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
}
