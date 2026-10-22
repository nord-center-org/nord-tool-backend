package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.domain.Perfil;
import br.com.nord_tool_backend.domain.PerfilPermissao;
import br.com.nord_tool_backend.domain.Usuario;
import br.com.nord_tool_backend.dto.AcessoUsuarioDto;
import br.com.nord_tool_backend.dto.AcessosDto;
import br.com.nord_tool_backend.exception.AcessoNegadoException;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.exception.NaoEncontradoException;
import br.com.nord_tool_backend.exception.NegocioException;
import br.com.nord_tool_backend.form.AcessoUsuarioForm;
import br.com.nord_tool_backend.repository.AcessoRepository;
import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.security.UsuarioAutenticado;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.service.SessaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AcessoServiceImplTest {

    private static final long ADMIN = 1L, USER = 2L;
    private AcessoRepository repository;
    private AutorizacaoService autorizacao;
    private SessaoService sessao;
    private br.com.nord_tool_backend.service.EmailService email;
    private AcessoServiceImpl service;
    private final List<Usuario> usuarios = new ArrayList<>();
    private final Map<Long, List<PerfilPermissao>> excecoes = new HashMap<>();

    @BeforeEach
    void setUp() {
        repository = mock(AcessoRepository.class);
        autorizacao = mock(AutorizacaoService.class);
        sessao = mock(SessaoService.class);
        email = mock(br.com.nord_tool_backend.service.EmailService.class);
        service = new AcessoServiceImpl(repository, autorizacao, sessao, email);
        usuarios.add(usuario(10, "Admin", ADMIN, "ADMIN", true));
        usuarios.add(usuario(20, "Ana", USER, "USER", true));
        when(repository.listarUsuarios()).thenReturn(usuarios);
        when(repository.listarPerfis()).thenReturn(Arrays.asList(perfil(ADMIN, "ADMIN"), perfil(USER, "USER")));
        Map<Long, List<PerfilPermissao>> doPerfil = new HashMap<>();
        doPerfil.put(ADMIN, Collections.singletonList(new PerfilPermissao("*", "ADMIN")));
        when(repository.permissoesPorPerfil()).thenReturn(doPerfil);
        when(repository.excecoesPorUsuario()).thenReturn(excecoes);
        logadoComo(10L);
    }

    private void logadoComo(long id) {
        Map<Modulo, Acao> todas = new EnumMap<>(Modulo.class);
        for (Modulo m : Modulo.values()) todas.put(m, Acao.ADMIN);
        when(autorizacao.exigir(Modulo.ADMINISTRACAO, Acao.ADMIN))
                .thenReturn(new UsuarioAutenticado(id, "a@b.com", "ADMIN", todas, 0, Instant.now()));
    }

    private static Usuario usuario(long id, String nome, long idPerfil, String cdPerfil, boolean ativo) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNmNome(nome);
        u.setNmEmail(nome.toLowerCase() + "@obra.com");
        u.setIdPerfil(idPerfil);
        u.setCdPerfil(cdPerfil);
        u.setInAtivo(ativo);
        return u;
    }

    private static Perfil perfil(long id, String cd) {
        Perfil p = new Perfil();
        p.setIdPerfil(id);
        p.setCdPerfil(cd);
        p.setNmPerfil(cd);
        return p;
    }

    private static AcessoUsuarioForm form(String perfil, boolean ativo, String... pares) {
        AcessoUsuarioForm f = new AcessoUsuarioForm();
        f.setCdPerfil(perfil);
        f.setInAtivo(ativo);
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i < pares.length; i += 2) m.put(pares[i], pares[i + 1]);
        f.setAcessos(m);
        return f;
    }

    @Test
    void exigeAdministracaoAdmin() {
        doThrow(new AcessoNegadoException("negado")).when(autorizacao).exigir(Modulo.ADMINISTRACAO, Acao.ADMIN);
        assertThrows(AcessoNegadoException.class, () -> service.listar());
        assertThrows(AcessoNegadoException.class, () -> service.salvar(20L, form("USER", true)));
        verifyNoInteractions(repository);
    }

    @Test
    void listarMostraAcessosEfetivosEPerfis() {
        excecoes.put(20L, Collections.singletonList(new PerfilPermissao("MEDICOES", "ESCRITA")));
        AcessosDto dto = service.listar();
        assertEquals(2, dto.getUsuarios().size());
        assertEquals("ADMIN", dto.getUsuarios().get(0).getAcessos().get("MEDICOES"));
        assertEquals(Collections.singletonMap("MEDICOES", "ESCRITA"), dto.getUsuarios().get(1).getAcessos());
        assertTrue(dto.getPerfis().stream().anyMatch(p -> p.getCdPerfil().equals("USER") && p.getAcessos().isEmpty()));
        assertTrue(dto.getModulos().contains("MEDICOES"));
    }

    @Test
    void salvaSoAsDiferencasDoPerfilEInvalidaASessao() {
        AcessoUsuarioDto dto = service.salvar(20L, form("USER", true, "MEDICOES", "ESCRITA", "CRONOGRAMA", "leitura", "CAIXINHA", "NENHUM"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PerfilPermissao>> c = ArgumentCaptor.forClass(List.class);
        verify(repository).substituirExcecoes(eq(20L), c.capture());
        assertEquals(Arrays.asList(new PerfilPermissao("CRONOGRAMA", "LEITURA"), new PerfilPermissao("MEDICOES", "ESCRITA")), c.getValue());
        verify(repository).alterarPerfilESituacao(20L, USER, true);
        verify(sessao).invalidar(20L);
        assertEquals("ESCRITA", dto.getAcessos().get("MEDICOES"));
        assertFalse(dto.getAcessos().containsKey("CAIXINHA"));
    }

    @Test
    void semMudancaNaoGravaNemDerrubaASessao() {
        service.salvar(20L, form("USER", true, "CAIXINHA", "NENHUM"));
        verify(repository, never()).alterarPerfilESituacao(anyLong(), anyLong(), anyBoolean());
        verify(sessao, never()).invalidar(any());
    }

    @Test
    void admimPodeNegarUmModuloDoPerfil() {
        usuarios.add(usuario(30, "Bia", ADMIN, "ADMIN", true));
        service.salvar(30L, form("ADMIN", true, "FINANCEIRO", "NENHUM"));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PerfilPermissao>> c = ArgumentCaptor.forClass(List.class);
        verify(repository).substituirExcecoes(eq(30L), c.capture());
        assertEquals(Collections.singletonList(new PerfilPermissao("FINANCEIRO", "NENHUM")), c.getValue());
    }

    @Test
    void naoDeixaSeDesativarNemTirarOProprioAdmin() {
        assertThrows(NegocioException.class, () -> service.salvar(10L, form("ADMIN", false)));
        assertThrows(NegocioException.class, () -> service.salvar(10L, form("USER", true)));
        assertThrows(NegocioException.class, () -> service.salvar(10L, form("ADMIN", true, "ADMINISTRACAO", "ESCRITA")));
        verify(repository, never()).substituirExcecoes(anyLong(), any());
    }

    @Test
    void naoDeixaOSistemaSemAdminAtivo() {
        logadoComo(99L); // outro admin fora da lista (ex.: sessão de suporte)
        assertThrows(NegocioException.class, () -> service.salvar(10L, form("USER", true)));
    }

    @Test
    void validaEntrada() {
        assertThrows(NaoEncontradoException.class, () -> service.salvar(77L, form("USER", true)));
        assertThrows(EntradaInvalidaException.class, () -> service.salvar(20L, form("CHEFE", true)));
        assertThrows(EntradaInvalidaException.class, () -> service.salvar(20L, form("USER", true, "XPTO", "LEITURA")));
        assertThrows(EntradaInvalidaException.class, () -> service.salvar(20L, form("USER", true, "MEDICOES", "TUDO")));
    }

    @Test
    void salvarDadosAparaEDeixaCelularVazioComoNulo() {
        br.com.nord_tool_backend.form.DadosUsuarioForm f = new br.com.nord_tool_backend.form.DadosUsuarioForm();
        f.setNmNome("  Ana Souza ");
        f.setNrCelular("  ");
        f.setIdEmpresa(3);
        f.setIdCargo(4);
        service.salvarDados(20L, f);
        verify(repository).alterarDados(20L, "Ana Souza", null, 3, 4);
        assertThrows(NaoEncontradoException.class, () -> service.salvarDados(77L, f));
    }

    @Test
    void vincularColaboradorAntigo() {
        br.com.nord_tool_backend.dto.ColaboradorLegadoDto ze = new br.com.nord_tool_backend.dto.ColaboradorLegadoDto(5L, "Zé", "Nord", "Pedreiro", null, null, null, 3);
        when(repository.listarColaboradoresLegados()).thenReturn(Collections.singletonList(ze));

        service.vincularColaborador(5L, 20L);
        verify(repository).desvincularColaborador(5L);
        verify(repository).vincularColaborador(5L, 20L);

        service.vincularColaborador(5L, null);
        verify(repository, org.mockito.Mockito.times(2)).desvincularColaborador(5L);

        assertThrows(NaoEncontradoException.class, () -> service.vincularColaborador(9L, 20L));
        assertThrows(NaoEncontradoException.class, () -> service.vincularColaborador(5L, 77L));
    }

    @Test
    void dadosELegadosExigemAdmin() {
        doThrow(new AcessoNegadoException("negado")).when(autorizacao).exigir(Modulo.ADMINISTRACAO, Acao.ADMIN);
        assertThrows(AcessoNegadoException.class, () -> service.listarColaboradoresLegados());
        assertThrows(AcessoNegadoException.class, () -> service.vincularColaborador(5L, 20L));
        assertThrows(AcessoNegadoException.class, () -> service.salvarDados(20L, new br.com.nord_tool_backend.form.DadosUsuarioForm()));
    }

    @Test
    void aprovarCadastroPendenteIncluiNasFasesEAvisaPorEmail() {
        Usuario pendente = usuario(40, "Novo", USER, "USER", true);
        pendente.setCdSituacaoCadastro("PENDENTE");
        usuarios.add(pendente);
        br.com.nord_tool_backend.form.DecisaoCadastroForm f = new br.com.nord_tool_backend.form.DecisaoCadastroForm();
        f.setAprovar(true);
        f.setIdsFases(Arrays.asList(7L, 8L, 7L));
        when(repository.contarFases(Arrays.asList(7L, 8L))).thenReturn(2);
        service.decidirCadastro(40L, f);
        verify(repository).decidirCadastro(40L, "APROVADO", true);
        verify(repository).adicionarFases(40L, Arrays.asList(7L, 8L));
        verify(sessao).invalidar(40L);
        verify(email).enviar(eq("novo@obra.com"), any(), any());
    }

    @Test
    void recusarDesativaENaoIncluiFases() {
        Usuario pendente = usuario(41, "Outro", USER, "USER", true);
        pendente.setCdSituacaoCadastro("PENDENTE");
        usuarios.add(pendente);
        br.com.nord_tool_backend.form.DecisaoCadastroForm f = new br.com.nord_tool_backend.form.DecisaoCadastroForm();
        f.setAprovar(false);
        f.setIdsFases(Collections.singletonList(7L));
        service.decidirCadastro(41L, f);
        verify(repository).decidirCadastro(41L, "RECUSADO", false);
        verify(repository, never()).adicionarFases(anyLong(), any());
        verify(email, never()).enviar(any(), any(), any());
    }

    @Test
    void naoDecideDuasVezesNemComFaseInexistente() {
        br.com.nord_tool_backend.form.DecisaoCadastroForm f = new br.com.nord_tool_backend.form.DecisaoCadastroForm();
        f.setAprovar(true);
        assertThrows(NegocioException.class, () -> service.decidirCadastro(20L, f)); // Ana já é APROVADO (nulo = aprovado)
        Usuario pendente = usuario(42, "Mais um", USER, "USER", true);
        pendente.setCdSituacaoCadastro("PENDENTE");
        usuarios.add(pendente);
        f.setIdsFases(Collections.singletonList(99L));
        when(repository.contarFases(Collections.singletonList(99L))).thenReturn(0);
        assertThrows(EntradaInvalidaException.class, () -> service.decidirCadastro(42L, f));
    }

    @Test
    void cargoDaAcessoMinimoEExcecaoEhSoODiferenteDoCargo() {
        usuarios.get(1).setIdCargo(4);
        Map<Integer, List<PerfilPermissao>> doCargo = new HashMap<>();
        doCargo.put(4, Collections.singletonList(new PerfilPermissao("MEDICOES", "LEITURA")));
        when(repository.permissoesPorCargo()).thenReturn(doCargo);

        assertEquals("LEITURA", service.listar().getUsuarios().get(1).getAcessos().get("MEDICOES"));
        assertEquals("LEITURA", service.listar().getUsuarios().get(1).getAcessosPadrao().get("MEDICOES"));

        // Manter o que o cargo dá não vira exceção; subir para Escrita vira.
        service.salvar(20L, form("USER", true, "MEDICOES", "LEITURA", "CAIXINHA", "NENHUM"));
        verify(repository, never()).substituirExcecoes(anyLong(), any());
        service.salvar(20L, form("USER", true, "MEDICOES", "ESCRITA"));
        verify(repository).substituirExcecoes(20L, Collections.singletonList(new PerfilPermissao("MEDICOES", "ESCRITA")));
    }

    @Test
    void salvarAcessosDoCargoGuardaSoOQueDaERenovaSessoes() {
        br.com.nord_tool_backend.domain.Cargo c = new br.com.nord_tool_backend.domain.Cargo();
        c.setId(4L);
        c.setNmCargo("Encarregado");
        when(repository.listarCargos()).thenReturn(Collections.singletonList(c));
        when(repository.renovarSessoesDoCargo(4)).thenReturn(Arrays.asList(20L, 30L));
        Map<String, String> acessos = new HashMap<>();
        acessos.put("MEDICOES", "escrita");
        acessos.put("CAIXINHA", "NENHUM");
        assertEquals("ESCRITA", service.salvarAcessosCargo(4, acessos).getAcessos().get("MEDICOES"));
        verify(repository).substituirAcessosCargo(4, Collections.singletonList(new PerfilPermissao("MEDICOES", "ESCRITA")));
        verify(sessao).invalidar(20L);
        verify(sessao).invalidar(30L);
        assertThrows(NaoEncontradoException.class, () -> service.salvarAcessosCargo(9, acessos));
        acessos.put("XPTO", "LEITURA");
        assertThrows(EntradaInvalidaException.class, () -> service.salvarAcessosCargo(4, acessos));
    }

    @Test
    void superiorNaoAceitaCicloNemAPropriaPessoa() {
        usuarios.get(1).setIdSuperior(10L); // Ana responde ao Admin
        service.definirSuperior(20L, 10L);
        verify(repository).definirSuperior(20L, 10L);
        assertThrows(EntradaInvalidaException.class, () -> service.definirSuperior(10L, 20L)); // Admin abaixo da Ana: ciclo
        assertThrows(EntradaInvalidaException.class, () -> service.definirSuperior(20L, 20L));
        assertThrows(NaoEncontradoException.class, () -> service.definirSuperior(20L, 99L));
        service.definirSuperior(20L, null);
        verify(repository).definirSuperior(20L, null);
    }

    @Test
    void trocarCargoNosDadosRenovaASessao() {
        br.com.nord_tool_backend.form.DadosUsuarioForm f = new br.com.nord_tool_backend.form.DadosUsuarioForm();
        f.setNmNome("Ana");
        f.setIdCargo(4);
        service.salvarDados(20L, f);
        verify(repository).renovarSessao(20L);
        verify(sessao).invalidar(20L);
    }
}
