package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.domain.CronogramaSemanal;
import br.com.nord_tool_backend.domain.IntegracaoToken;
import br.com.nord_tool_backend.domain.PerfilPermissao;
import br.com.nord_tool_backend.domain.Usuario;
import br.com.nord_tool_backend.dto.CronogramaSemanalExcecaoDto;
import br.com.nord_tool_backend.dto.IntegracaoTokenDto;
import br.com.nord_tool_backend.dto.RelatorioCronogramaDto;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.exception.NaoEncontradoException;
import br.com.nord_tool_backend.exception.LimiteRequisicoesException;
import br.com.nord_tool_backend.form.IntegracaoTokenForm;
import br.com.nord_tool_backend.repository.IntegracaoRepository;
import br.com.nord_tool_backend.repository.UsuarioRepository;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.security.UsuarioAutenticado;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.service.LimiteTentativasService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IntegracaoServiceImplTest {

    /** Quarta-feira, 28/10/2026, 10h em Brasília. */
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-28T13:00:00Z"), ZoneOffset.UTC);
    private static final String CHAVE = "ntk_" + "a".repeat(43);

    private IntegracaoRepository repository;
    private UsuarioRepository usuarios;
    private AutorizacaoService autorizacao;
    private LimiteTentativasService limite;
    private IntegracaoServiceImpl service;
    private Usuario dono;

    @BeforeEach
    void setUp() {
        repository = mock(IntegracaoRepository.class);
        usuarios = mock(UsuarioRepository.class);
        autorizacao = mock(AutorizacaoService.class);
        limite = mock(LimiteTentativasService.class);
        service = new IntegracaoServiceImpl(repository, usuarios, autorizacao, limite, CLOCK);
        UsuarioAutenticado u = new UsuarioAutenticado(9L, "u@o.com", "USER", new EnumMap<>(Modulo.class), 0, Instant.now());
        when(autorizacao.exigir(any(), any())).thenReturn(u);
        when(autorizacao.exigirAutenticado()).thenReturn(u);

        dono = new Usuario();
        dono.setId(9L);
        dono.setNmNome("Nick Gomes");
        dono.setInAtivo(true);
        dono.setCdSituacaoCadastro("APROVADO");
        dono.setIdPerfil(2L);
        dono.setIdCargo(3);
        IntegracaoToken t = new IntegracaoToken();
        t.setIdToken(5L);
        t.setIdUsuario(9L);
        t.setCdEscopo(IntegracaoServiceImpl.ESCOPO_CRONOGRAMA);
        when(repository.buscarAtivoPorHash(IntegracaoServiceImpl.hash(CHAVE))).thenReturn(Optional.of(t));
        when(usuarios.buscarPorId(9L)).thenReturn(Optional.of(dono));
        permitir("LEITURA");
    }

    private void permitir(String acao) {
        when(usuarios.listarPermissoes(2L)).thenReturn(Collections.singletonList(new PerfilPermissao("CRONOGRAMA", acao)));
    }

    private static CronogramaSemanal item(String nome) {
        CronogramaSemanal c = new CronogramaSemanal();
        c.setNmCronogramaSemanal(nome);
        c.setNmStatusCronograma("executar");
        c.setNmCronogramaCategoria("Engenharia");
        c.setInCronogramaFixo(false);
        return c;
    }

    private static CronogramaSemanal fixo(long id, int dia, String horario, String nome) {
        CronogramaSemanal c = item(nome);
        c.setId(id);
        c.setInCronogramaFixo(true);
        c.setIdDiaSemana(dia);
        c.setNmHorario(horario);
        return c;
    }

    @Test
    void criarTokenGeraChaveAleatoriaEGuardaSoOHash() {
        when(repository.criar(anyLong(), anyString(), anyString(), anyString(), anyString())).thenReturn(11L);
        IntegracaoTokenForm f = new IntegracaoTokenForm();
        f.setNmRotulo("  Gemini ");

        IntegracaoTokenDto d = service.criarToken(f);

        assertTrue(d.getChave().matches("^ntk_[A-Za-z0-9_-]{43}$"), d.getChave());
        assertEquals(11L, d.getIdToken());
        assertEquals("Gemini", d.getNmRotulo());
        assertEquals(d.getChave().substring(0, 10), d.getTxPrefixo());
        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(repository).criar(eq(9L), eq(IntegracaoServiceImpl.ESCOPO_CRONOGRAMA), eq("Gemini"), hash.capture(), eq(d.getTxPrefixo()));
        assertEquals(IntegracaoServiceImpl.hash(d.getChave()), hash.getValue());
        assertNotEquals(d.getChave(), hash.getValue());
        assertNotEquals(d.getChave(), service.criarToken(f).getChave());
    }

    @Test
    void criarTokenRespeitaOLimiteDeLinksAtivos() {
        when(repository.contarAtivos(9L)).thenReturn(IntegracaoServiceImpl.MAX_LINKS);
        IntegracaoTokenForm f = new IntegracaoTokenForm();
        f.setNmRotulo("Gemini");
        assertThrows(EntradaInvalidaException.class, () -> service.criarToken(f));
        verify(repository, never()).criar(anyLong(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void revogarLinkDeOutroUsuarioDa404() {
        when(repository.revogar(9L, 77L)).thenReturn(0);
        assertThrows(NaoEncontradoException.class, () -> service.revogarToken(77L));
        when(repository.revogar(9L, 5L)).thenReturn(1);
        service.revogarToken(5L);
    }

    @Test
    void chaveMalFormadaOuDesconhecidaDa404SemConsultarOBancoAToa() {
        assertThrows(NaoEncontradoException.class, () -> service.relatorioCronograma("abc", 7));
        assertThrows(NaoEncontradoException.class, () -> service.relatorioCronograma(null, 7));
        verify(repository, never()).buscarAtivoPorHash(anyString());
        assertThrows(NaoEncontradoException.class, () -> service.relatorioCronograma("ntk_" + "b".repeat(43), 7));
    }

    @Test
    void limiteDeLeiturasBarraAntesDeLerOCronograma() {
        doThrow(new LimiteRequisicoesException("calma")).when(limite).registrar(anyString(), anyInt());
        assertThrows(LimiteRequisicoesException.class, () -> service.relatorioCronograma(CHAVE, 7));
        verify(repository, never()).cronogramaDoUsuario(anyLong(), any(Boolean.class));
    }

    @Test
    void donoInativoOuSemPermissaoNaoLeNada() {
        dono.setInAtivo(false);
        assertThrows(NaoEncontradoException.class, () -> service.relatorioCronograma(CHAVE, 7));
        dono.setInAtivo(true);
        permitir("NENHUM");
        assertThrows(NaoEncontradoException.class, () -> service.relatorioCronograma(CHAVE, 7));
        verify(repository, never()).marcarUso(anyLong());
    }

    @Test
    void relatorioMontaHojeAtrasadasProximosDiasESemData() {
        CronogramaSemanal reuniao = item("Reunião de obra");
        reuniao.setDtAgendamento(LocalDateTime.of(2026, 10, 28, 14, 30));
        CronogramaSemanal atrasada = item("Enviar medição");
        atrasada.setDtPrazo(LocalDateTime.of(2026, 10, 26, 0, 0));
        CronogramaSemanal finalizada = item("Velha");
        finalizada.setDtPrazo(LocalDateTime.of(2026, 10, 20, 0, 0));
        finalizada.setNmStatusCronograma("finalizada");
        CronogramaSemanal prazoSexta = item("Entregar relatório");
        prazoSexta.setDtPrazo(LocalDateTime.of(2026, 10, 30, 0, 0));
        CronogramaSemanal semData = item("Estudar NR-18");
        CronogramaSemanal academia = fixo(1L, 3, "06:00", "Academia");
        CronogramaSemanal culto = fixo(2L, 3, "19:30", "Culto");
        when(repository.cronogramaDoUsuario(9L, false)).thenReturn(Arrays.asList(reuniao, atrasada, finalizada, prazoSexta, semData, academia, culto));
        CronogramaSemanalExcecaoDto pulado = new CronogramaSemanalExcecaoDto();
        pulado.setIdCronogramaSemanal(2L);
        pulado.setDtExcecao("2026-10-28");
        when(repository.excecoesDoUsuario(9L, false)).thenReturn(Collections.singletonList(pulado));

        RelatorioCronogramaDto r = service.relatorioCronograma(CHAVE, 3);

        assertEquals("Nick", r.getPessoa());
        assertEquals("28/10/2026 10:00", r.getGeradoEm());
        assertEquals("quarta-feira", r.getHoje().getDiaSemana());
        List<RelatorioCronogramaDto.ItemDto> hoje = r.getHoje().getItens();
        assertEquals(Arrays.asList("Academia", "Reunião de obra"), Arrays.asList(hoje.get(0).getTitulo(), hoje.get(1).getTitulo()));
        assertEquals(2, hoje.size(), "o culto foi desmarcado hoje");
        assertEquals("14:30", hoje.get(1).getHorario());
        assertEquals(1, r.getAtrasadas().size());
        assertEquals("Enviar medição", r.getAtrasadas().get(0).getTitulo());
        assertEquals(2, r.getProximosDias().size());
        assertEquals("sexta-feira", r.getProximosDias().get(1).getDiaSemana());
        assertEquals("Entregar relatório", r.getProximosDias().get(1).getItens().get(0).getTitulo());
        assertEquals(1, r.getSemData().size());
        verify(repository).marcarUso(5L);
    }

    @Test
    void textoTrazAsSecoesEmPortugues() {
        CronogramaSemanal atrasada = item("Enviar medição");
        atrasada.setDtPrazo(LocalDateTime.of(2026, 10, 26, 0, 0));
        atrasada.setNmTag("obra");
        when(repository.cronogramaDoUsuario(9L, false)).thenReturn(Arrays.asList(atrasada, fixo(1L, 3, "06:00", "Academia")));

        String t = service.relatorioCronogramaTexto(CHAVE, 2);

        assertTrue(t.contains("HOJE — quarta-feira, 28/10"), t);
        assertTrue(t.contains("- 06:00 · [Rotina] Academia (Engenharia)"), t);
        assertTrue(t.contains("- [Prazo 26/10] Enviar medição (Engenharia · Executar · #obra)"), t);
        assertTrue(t.contains("quinta-feira, 29/10\n- Sem tarefas."), t);
    }

    @Test
    void adminDoCronogramaVeTambemOsItensSemDonoEDiasFicamEntre1E14() {
        permitir("ADMIN");
        RelatorioCronogramaDto r = service.relatorioCronograma(CHAVE, 99);
        verify(repository).cronogramaDoUsuario(9L, true);
        assertEquals(IntegracaoServiceImpl.MAX_DIAS - 1, r.getProximosDias().size());
        assertEquals(0, service.relatorioCronograma(CHAVE, -3).getProximosDias().size());
    }
}
