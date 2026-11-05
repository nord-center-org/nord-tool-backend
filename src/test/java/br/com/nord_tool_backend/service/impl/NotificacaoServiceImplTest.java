package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.dto.DemandaAvisoDto;
import br.com.nord_tool_backend.dto.NotificacaoPreferenciaDto;
import br.com.nord_tool_backend.dto.PushInscricaoDto;
import br.com.nord_tool_backend.repository.NotificacaoRepository;
import br.com.nord_tool_backend.security.UsuarioAutenticado;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.service.push.PushEnviador;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificacaoServiceImplTest {

    private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");
    private NotificacaoRepository repository;
    private PushEnviador enviador;
    private final UsuarioAutenticado nick = new UsuarioAutenticado(1L, "n@obra.com", "ADMIN", Collections.emptyMap(), 0, java.time.Instant.now());

    @BeforeEach
    void preparar() {
        repository = mock(NotificacaoRepository.class);
        enviador = mock(PushEnviador.class);
        doAnswer(inv -> { ((Runnable) inv.getArgument(0)).run(); return null; }).when(enviador).emSegundoPlano(any());
        when(repository.nomeDoUsuario(anyLong())).thenReturn("Nick Silva");
        // Cada pessoa tem um aparelho com o id dela.
        when(repository.inscricoes(any())).thenAnswer(i -> ((Collection<Long>) i.getArgument(0)).stream().map(id -> {
            PushInscricaoDto a = new PushInscricaoDto();
            a.setId(id);
            a.setIdUsuario(id);
            return a;
        }).collect(Collectors.toList()));
    }

    private NotificacaoServiceImpl servico(LocalDateTime agora) {
        Clock c = Clock.fixed(agora.atZone(SP).toInstant(), SP);
        return new NotificacaoServiceImpl(repository, mock(AutorizacaoService.class), enviador, new ObjectMapper(), c);
    }

    private static NotificacaoPreferenciaDto pref(long id) {
        NotificacaoPreferenciaDto p = new NotificacaoPreferenciaDto();
        p.setIdUsuario(id);
        return p;
    }

    private static DemandaAvisoDto demanda(LocalDateTime agendada, String envolvidos) {
        DemandaAvisoDto d = new DemandaAvisoDto();
        d.setId(5L);
        d.setNmCronogramaSemanal("Reunião de obra");
        d.setDtAgendamento(agendada);
        d.setTxEnvolvidos(envolvidos);
        return d;
    }

    @SuppressWarnings("unchecked")
    private List<Long> destinatarios(int vezes) {
        ArgumentCaptor<List<PushInscricaoDto>> cap = ArgumentCaptor.forClass(List.class);
        verify(enviador, times(vezes)).enviar(cap.capture(), anyString());
        return cap.getAllValues().stream().flatMap(List::stream).map(PushInscricaoDto::getIdUsuario).collect(Collectors.toList());
    }

    @Test
    void demandaNovaVaiParaDonoECompartilhadosConformeAPreferencia() {
        NotificacaoPreferenciaDto semNovas = pref(3);
        semNovas.setInDemandaNova(false);
        when(repository.demanda(5L)).thenReturn(Optional.of(demanda(null, "1,2,3")));
        when(repository.preferencias(any())).thenReturn(Arrays.asList(pref(1), pref(2), semNovas));
        servico(LocalDateTime.of(2026, 11, 5, 9, 0)).demandaCriada(5L, nick);
        assertEquals(Arrays.asList(1L, 2L), destinatarios(1));
    }

    @Test
    void quemFezSoRecebeSePediuOQueEleMesmoFez() {
        NotificacaoPreferenciaDto eu = pref(1);
        eu.setInMinhasAcoes(false);
        when(repository.demanda(5L)).thenReturn(Optional.of(demanda(null, "1,2")));
        when(repository.preferencias(any())).thenReturn(Arrays.asList(eu, pref(2)));
        servico(LocalDateTime.of(2026, 11, 5, 9, 0)).demandaAlterada(5L, nick, "etapa:executar");
        assertEquals(Collections.singletonList(2L), destinatarios(1));
        ArgumentCaptor<String> conteudo = ArgumentCaptor.forClass(String.class);
        verify(enviador).enviar(any(), conteudo.capture());
        assertTrue(conteudo.getValue().contains("Nick moveu “Reunião de obra” para Executar"), conteudo.getValue());
    }

    @Test
    void horarioAvisaComAAntecedenciaDeCadaUmESoUmaVez() {
        LocalDateTime agora = LocalDateTime.of(2026, 11, 5, 9, 40);
        DemandaAvisoDto d = demanda(LocalDateTime.of(2026, 11, 5, 10, 0), "1,2");
        NotificacaoPreferenciaDto trinta = pref(1); // 30 min antes: faltam 20 → avisa
        NotificacaoPreferenciaDto dez = pref(2);
        dez.setNrAntecedenciaMin(10); // faltam 20 → ainda não
        when(repository.demandasAgendadas(any(), any())).thenReturn(Collections.singletonList(d));
        when(repository.demandasComPrazo(any())).thenReturn(Collections.emptyList());
        when(repository.preferencias(any())).thenReturn(Arrays.asList(trinta, dez));
        when(repository.registrarEnvio(eq(1L), eq("horario:5:2026-11-05T10:00"))).thenReturn(true, false);
        NotificacaoServiceImpl s = servico(agora);
        s.lembretes();
        s.lembretes(); // segunda volta do agendador: não repete
        assertEquals(Collections.singletonList(1L), destinatarios(1));
        verify(repository, never()).registrarEnvio(eq(2L), anyString());
        ArgumentCaptor<String> conteudo = ArgumentCaptor.forClass(String.class);
        verify(enviador).enviar(any(), conteudo.capture());
        assertTrue(conteudo.getValue().contains("Daqui a 20 min: Reunião de obra"), conteudo.getValue());
    }

    @Test
    void prazoDoDiaSoDepoisDasOito() {
        DemandaAvisoDto d = demanda(null, "1");
        when(repository.demandasAgendadas(any(), any())).thenReturn(Collections.emptyList());
        when(repository.demandasComPrazo(any())).thenReturn(Collections.singletonList(d));
        when(repository.preferencias(any())).thenReturn(Collections.singletonList(pref(1)));
        when(repository.registrarEnvio(eq(1L), eq("prazo:5:2026-11-05"))).thenReturn(true);
        servico(LocalDateTime.of(2026, 11, 5, 7, 30)).lembretes();
        verify(repository, never()).demandasComPrazo(any());
        servico(LocalDateTime.of(2026, 11, 5, 8, 0)).lembretes();
        assertEquals(Collections.singletonList(1L), destinatarios(1));
    }

    @Test
    void falhaNoAvisoNaoQuebraAAcao() {
        when(repository.demanda(5L)).thenThrow(new RuntimeException("banco fora"));
        servico(LocalDateTime.of(2026, 11, 5, 9, 0)).demandaCriada(5L, nick);
        verify(enviador, never()).enviar(any(), anyString());
    }

    @Test
    void envolvidosIgnoraLixo() {
        assertEquals(Arrays.asList(1L, 2L), new java.util.ArrayList<>(NotificacaoServiceImpl.envolvidos(demanda(null, "1, 2,,x,1"))));
    }

    @Test
    void repositorioGravaEmTransacaoPropria() {
        // O pool roda sem auto-commit: sem transação, chaves VAPID, aparelhos e lembretes já enviados nunca seriam gravados.
        assertTrue(br.com.nord_tool_backend.repository.impl.NotificacaoRepositoryImpl.class
                .isAnnotationPresent(org.springframework.transaction.annotation.Transactional.class));
    }
}
