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
import static org.mockito.ArgumentMatchers.anyString;
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
import static org.mockito.Mockito.never;
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

    @Mock
    private br.com.nord_tool_backend.service.NotificacaoService notificacoes;

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
        if (cronogramaSemanalDto.getId() != null) verify(notificacoes).demandaCriada(org.mockito.ArgumentMatchers.eq(cronogramaSemanalDto.getId()), any());
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
        assertThrows(AcessoNegadoException.class, () -> cronogramaSemanalService.salvarCronogramaSemanal(new CronogramaSemanalForm()));
        verifyNoInteractions(cronogramaSemanalRepository);
    }

    @Test
    void excluirExigeOSubacessoDeExcluir() {
        when(autorizacao.exigir(Modulo.CRONOGRAMA_EXCLUIR, Acao.ESCRITA)).thenThrow(new AcessoNegadoException("Acesso negado"));
        assertThrows(AcessoNegadoException.class, () -> cronogramaSemanalService.deletarCronogramaSemanal(1L));
        verifyNoInteractions(cronogramaSemanalRepository);
    }

    @Test
    void atividadeFixaExigeOSubacessoDaRotina() {
        lenient().when(autorizacao.exigir(Modulo.CRONOGRAMA_ROTINA, Acao.ESCRITA)).thenThrow(new AcessoNegadoException("Acesso negado"));
        CronogramaSemanalForm fixa = new CronogramaSemanalForm();
        fixa.setFlFixo(true);
        fixa.setIdDiaSemana(2);
        assertThrows(AcessoNegadoException.class, () -> cronogramaSemanalService.salvarCronogramaSemanal(fixa));
        assertThrows(AcessoNegadoException.class, () -> cronogramaSemanalService.incluirExcecao(1L, LocalDate.of(2026, 10, 21)));
        // Alterar uma demanda que já era fixa também é rotina.
        CronogramaSemanal existente = new CronogramaSemanal();
        existente.setInCronogramaFixo(true);
        when(cronogramaSemanalRepository.buscarPorIdCronogramaSemanal(5L)).thenReturn(existente);
        CronogramaSemanalForm virouAvulsa = new CronogramaSemanalForm();
        virouAvulsa.setIdCronogramaSemanal(5L);
        virouAvulsa.setFlFixo(false);
        assertThrows(AcessoNegadoException.class, () -> cronogramaSemanalService.alterarCronogramaSemanal(virouAvulsa));
        verify(cronogramaSemanalRepository, never()).salvarCronogramaSemanal(any());
        verify(cronogramaSemanalRepository, never()).alterarCronogramaSemanal(any());
    }

    @Test
    void demandaPontualComPeriodoGuardaOFimEValida() {
        when(cronogramaSemanalRepository.salvarCronogramaSemanal(any(CronogramaSemanal.class))).thenReturn(cronogramaSemanalDto);
        CronogramaSemanalForm periodo = new CronogramaSemanalForm();
        periodo.setFlFixo(false);
        periodo.setDtAgendamento(java.time.LocalDateTime.of(2026, 11, 10, 8, 0));
        periodo.setDtFimAgendamento(java.time.LocalDateTime.of(2026, 11, 14, 0, 0));
        cronogramaSemanalService.salvarCronogramaSemanal(periodo);
        org.mockito.ArgumentCaptor<CronogramaSemanal> salvo = org.mockito.ArgumentCaptor.forClass(CronogramaSemanal.class);
        verify(cronogramaSemanalRepository).salvarCronogramaSemanal(salvo.capture());
        assertEquals(java.time.LocalDateTime.of(2026, 11, 14, 0, 0), salvo.getValue().getDtFimAgendamento());

        // Fim no mesmo dia do início é um dia só.
        CronogramaSemanalForm mesmoDia = new CronogramaSemanalForm();
        mesmoDia.setFlFixo(false);
        mesmoDia.setDtAgendamento(java.time.LocalDateTime.of(2026, 11, 10, 8, 0));
        mesmoDia.setDtFimAgendamento(java.time.LocalDateTime.of(2026, 11, 10, 0, 0));
        cronogramaSemanalService.salvarCronogramaSemanal(mesmoDia);
        verify(cronogramaSemanalRepository, org.mockito.Mockito.times(2)).salvarCronogramaSemanal(salvo.capture());
        assertEquals(null, salvo.getValue().getDtFimAgendamento());

        // Atividade fixa não tem período.
        CronogramaSemanalForm fixa = new CronogramaSemanalForm();
        fixa.setFlFixo(true);
        fixa.setIdDiaSemana(2);
        fixa.setDtFimAgendamento(java.time.LocalDateTime.of(2026, 11, 14, 0, 0));
        cronogramaSemanalService.salvarCronogramaSemanal(fixa);
        verify(cronogramaSemanalRepository, org.mockito.Mockito.times(3)).salvarCronogramaSemanal(salvo.capture());
        assertEquals(null, salvo.getValue().getDtFimAgendamento());
    }

    @Test
    void periodoInvalidoERecusado() {
        java.time.LocalDateTime inicio = java.time.LocalDateTime.of(2026, 11, 10, 8, 0);
        assertThrows(EntradaInvalidaException.class, () -> CronogramaSemanalServiceImpl.validarPeriodo(null, inicio));
        assertThrows(EntradaInvalidaException.class, () -> CronogramaSemanalServiceImpl.validarPeriodo(inicio, inicio.minusDays(1)));
        assertThrows(EntradaInvalidaException.class, () -> CronogramaSemanalServiceImpl.validarPeriodo(inicio, inicio.plusYears(1).plusDays(1)));
        CronogramaSemanalServiceImpl.validarPeriodo(inicio, null);
        CronogramaSemanalServiceImpl.validarPeriodo(inicio, inicio.plusYears(1));
        CronogramaSemanalForm semInicio = new CronogramaSemanalForm();
        semInicio.setFlFixo(false);
        semInicio.setDtFimAgendamento(inicio);
        assertThrows(EntradaInvalidaException.class, () -> cronogramaSemanalService.salvarCronogramaSemanal(semInicio));
    }

    @Test
    void moverStatusValeParaQuemEnxergaOItemEValidaOStatus() {
        when(cronogramaSemanalRepository.moverStatus(1L, "executar")).thenReturn(1);
        when(cronogramaSemanalRepository.buscarPorIdCronogramaSemanal(1L)).thenReturn(cronogramaSemanal);
        cronogramaSemanalService.moverStatus(1L, " Executar ");
        verify(cronogramaSemanalRepository).moverStatus(1L, "executar");

        assertThrows(br.com.nord_tool_backend.exception.EntradaInvalidaException.class, () -> cronogramaSemanalService.moverStatus(1L, "lixo"));
        when(cronogramaSemanalRepository.moverStatus(2L, "finalizada")).thenReturn(0);
        assertThrows(br.com.nord_tool_backend.exception.NaoEncontradoException.class, () -> cronogramaSemanalService.moverStatus(2L, "finalizada"));
    }

    @Test
    void moverStatusExigeEditarNoCronograma() {
        when(autorizacao.exigir(Modulo.CRONOGRAMA, Acao.ESCRITA)).thenThrow(new AcessoNegadoException("Acesso negado"));
        assertThrows(AcessoNegadoException.class, () -> cronogramaSemanalService.moverStatus(1L, "executar"));
        verifyNoInteractions(cronogramaSemanalRepository);
    }

    @Test
    void compartilharExigeOSubacessoDeCompartilhar() {
        when(autorizacao.exigir(Modulo.CRONOGRAMA_COMPARTILHAR, Acao.ESCRITA)).thenThrow(new AcessoNegadoException("Acesso negado"));
        assertThrows(AcessoNegadoException.class, () -> cronogramaSemanalService.compartilhar(1L, Collections.singletonList(8L)));
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
            service = new CronogramaSemanalServiceImpl(repository, autorizacao, mock(br.com.nord_tool_backend.service.LugiaService.class), mock(br.com.nord_tool_backend.service.NotificacaoService.class));
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

    /** Chat da Lugia: o texto livre vira demanda(s) já com valores que a tela aceita. */
    @org.junit.jupiter.api.Nested
    class Interpretar {

        private br.com.nord_tool_backend.service.LugiaService lugia;
        private AutorizacaoService autorizacao;
        private CronogramaSemanalServiceImpl service;

        @BeforeEach
        void setUp() {
            lugia = mock(br.com.nord_tool_backend.service.LugiaService.class);
            autorizacao = mock(AutorizacaoService.class);
            service = new CronogramaSemanalServiceImpl(mock(CronogramaSemanalRepository.class), autorizacao, lugia, mock(br.com.nord_tool_backend.service.NotificacaoService.class));
        }

        private br.com.nord_tool_backend.form.CronogramaInterpretarForm form(String texto) {
            br.com.nord_tool_backend.form.CronogramaInterpretarForm f = new br.com.nord_tool_backend.form.CronogramaInterpretarForm();
            f.setTexto(texto);
            f.setHoje("2026-10-30");
            f.getAnteriores().add("reunião toda segunda");
            return f;
        }

        @Test
        void mandaHojeEPedidosAnterioresParaALugiaEExigeEscrita() {
            when(lugia.responderUmaVez(anyString(), anyString())).thenReturn("{\"mensagem\":\"ok\",\"demandas\":[]}");
            service.interpretar(form("muda para quarta"));
            verify(autorizacao).exigir(Modulo.CRONOGRAMA, Acao.ESCRITA);
            org.mockito.ArgumentCaptor<String> pedido = org.mockito.ArgumentCaptor.forClass(String.class);
            verify(lugia).responderUmaVez(eq(CronogramaSemanalServiceImpl.sistemaInterpretar()), pedido.capture());
            org.junit.jupiter.api.Assertions.assertTrue(pedido.getValue().contains("sexta-feira, 2026-10-30"));
            org.junit.jupiter.api.Assertions.assertTrue(pedido.getValue().contains("- reunião toda segunda"));
            org.junit.jupiter.api.Assertions.assertTrue(pedido.getValue().endsWith("Pedido atual: muda para quarta"));
        }

        @Test
        void semPermissaoNaoChamaALugia() {
            when(autorizacao.exigir(Modulo.CRONOGRAMA, Acao.ESCRITA)).thenThrow(new AcessoNegadoException("sem acesso"));
            assertThrows(AcessoNegadoException.class, () -> service.interpretar(form("x")));
            verifyNoInteractions(lugia);
        }

        @Test
        void leRespostaComBlocoDeCodigoENormalizaOsCampos() {
            String resposta = "Claro! ```json\n{\"mensagem\":\"Montei duas demandas.\",\"demandas\":["
                    + "{\"titulo\":\"Reunião de obra\",\"categoria\":\"engenharia\",\"status\":\"EXECUTAR\",\"fixa\":true,"
                    + "\"dias\":[1,3,0,6,9],\"horario\":\"9:00\",\"agendamento\":\"2026-11-02T09:00\",\"tag\":\"null\"},"
                    + "{\"titulo\":\"Entregar relatório\",\"categoria\":\"programacao\",\"prazo\":\"2026-11-06\",\"fixa\":false,"
                    + "\"agendamento\":\"2026-11-05\",\"observacao\":\"para o cliente\"},"
                    + "{\"titulo\":\"  \"}]}\n```";
            br.com.nord_tool_backend.dto.CronogramaInterpretacaoDto dto = CronogramaSemanalServiceImpl.lerInterpretacao(resposta);
            assertEquals("Montei duas demandas.", dto.getMensagem());
            assertEquals(2, dto.getDemandas().size());
            br.com.nord_tool_backend.dto.CronogramaRascunhoDto fixa = dto.getDemandas().get(0);
            assertEquals("Engenharia", fixa.getNmCategoria());
            assertEquals("executar", fixa.getNmStatusCronograma());
            assertEquals(Boolean.TRUE, fixa.getFlFixo());
            assertEquals(Arrays.asList(1, 3), fixa.getDiasSemana());
            assertEquals("09:00", fixa.getNmHorario());
            org.junit.jupiter.api.Assertions.assertNull(fixa.getDtAgendamento());
            org.junit.jupiter.api.Assertions.assertNull(fixa.getNmTag());
            br.com.nord_tool_backend.dto.CronogramaRascunhoDto pontual = dto.getDemandas().get(1);
            assertEquals("Programação", pontual.getNmCategoria());
            assertEquals("pendentes", pontual.getNmStatusCronograma());
            assertEquals("2026-11-06", pontual.getDtPrazo());
            assertEquals("2026-11-05T09:00", pontual.getDtAgendamento());
            assertEquals(Boolean.FALSE, pontual.getFlFixo());
            assertEquals("para o cliente", pontual.getTxObservacao());
        }

        @Test
        void fixaSemDiaValidoViraDemandaComum() {
            br.com.nord_tool_backend.dto.CronogramaInterpretacaoDto dto = CronogramaSemanalServiceImpl.lerInterpretacao(
                    "{\"demandas\":[{\"titulo\":\"Missa\",\"categoria\":\"Inventada\",\"fixa\":true,\"dias\":[0],\"horario\":\"10:00\"}]}");
            br.com.nord_tool_backend.dto.CronogramaRascunhoDto d = dto.getDemandas().get(0);
            assertEquals(Boolean.FALSE, d.getFlFixo());
            assertEquals("Engenharia", d.getNmCategoria());
            org.junit.jupiter.api.Assertions.assertNull(d.getNmHorario());
            assertEquals("Pronto, montei a demanda.", dto.getMensagem());
        }

        @Test
        void respostaSemJsonDaErroAmigavel() {
            br.com.nord_tool_backend.exception.NegocioException e = assertThrows(br.com.nord_tool_backend.exception.NegocioException.class,
                    () -> CronogramaSemanalServiceImpl.lerInterpretacao("Desculpe, não entendi."));
            org.junit.jupiter.api.Assertions.assertTrue(e.getMessage().contains("Tente escrever de outro jeito"));
        }
    }
}
