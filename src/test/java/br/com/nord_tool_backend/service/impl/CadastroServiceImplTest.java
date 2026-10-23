package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.domain.Obra;
import br.com.nord_tool_backend.domain.Usuario;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.exception.LimiteRequisicoesException;
import br.com.nord_tool_backend.form.CadastroForm;
import br.com.nord_tool_backend.form.EsqueciSenhaForm;
import br.com.nord_tool_backend.form.RedefinirSenhaForm;
import br.com.nord_tool_backend.repository.CadastroRepository;
import br.com.nord_tool_backend.repository.UsuarioRepository;
import br.com.nord_tool_backend.service.EmailService;
import br.com.nord_tool_backend.service.LimiteTentativasService;
import br.com.nord_tool_backend.service.SessaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CadastroServiceImplTest {

    private static final Clock RELOGIO = Clock.fixed(Instant.parse("2026-10-21T12:00:00Z"), ZoneId.of("America/Sao_Paulo"));
    private CadastroRepository repository;
    private UsuarioRepository usuarios;
    private PasswordEncoder encoder;
    private LimiteTentativasService limite;
    private EmailService email;
    private SessaoService sessao;
    private CadastroServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(CadastroRepository.class);
        usuarios = mock(UsuarioRepository.class);
        encoder = mock(PasswordEncoder.class);
        limite = mock(LimiteTentativasService.class);
        email = mock(EmailService.class);
        sessao = mock(SessaoService.class);
        when(encoder.encode(anyString())).thenReturn("HASH");
        service = new CadastroServiceImpl(repository, usuarios, encoder, limite, email, sessao, RELOGIO, "https://app.nord/");
    }

    private static CadastroForm form() {
        CadastroForm f = new CadastroForm();
        f.setNmNome(" João Encarregado ");
        f.setNmEmail(" Joao@Obra.com ");
        f.setNrCelular(" ");
        f.setCdObra(" nord2026 ");
        f.setSenha("senha-forte-123");
        return f;
    }

    private static Obra obra() {
        Obra o = new Obra();
        o.setId(3L);
        o.setNmObra("Nord");
        return o;
    }

    private static Usuario usuario(boolean ativo, String situacao) {
        Usuario u = new Usuario();
        u.setId(9L);
        u.setNmNome("João");
        u.setNmEmail("joao@obra.com");
        u.setInAtivo(ativo);
        u.setCdSituacaoCadastro(situacao);
        return u;
    }

    // ---------- cadastro ----------

    @Test
    void cadastroCriaPendenteNaObraDoCodigo() {
        when(repository.buscarObraAtivaPorCodigo("nord2026")).thenReturn(Optional.of(obra()));
        service.cadastrar(form(), "1.1.1.1");
        verify(repository).inserirPendente("João Encarregado", "joao@obra.com", "HASH", null, 3L);
        verify(limite).registrar("cadastro-ip:1.1.1.1", CadastroServiceImpl.MAX_CADASTROS_IP);
    }

    @Test
    void cadastroSemCodigoDaObraFicaPendenteParaOAdminDefinir() {
        CadastroForm f = form();
        f.setCdObra("  ");
        service.cadastrar(f, "1.1.1.1");
        verify(repository, never()).buscarObraAtivaPorCodigo(any());
        verify(repository).inserirPendente("João Encarregado", "joao@obra.com", "HASH", null, null);
        CadastroForm semCampo = form();
        semCampo.setCdObra(null);
        service.cadastrar(semCampo, "1.1.1.1");
        verify(repository, org.mockito.Mockito.times(2)).inserirPendente("João Encarregado", "joao@obra.com", "HASH", null, null);
    }

    @Test
    void cadastroRecusaCodigoInvalidoESenhaFraca() {
        when(repository.buscarObraAtivaPorCodigo(anyString())).thenReturn(Optional.empty());
        assertThrows(EntradaInvalidaException.class, () -> service.cadastrar(form(), "ip"));
        CadastroForm fraca = form();
        fraca.setSenha("123");
        assertThrows(EntradaInvalidaException.class, () -> service.cadastrar(fraca, "ip"));
        verify(repository, never()).inserirPendente(any(), any(), any(), any(), any());
    }

    @Test
    void emailRepetidoRespondeIgualENaoCria() {
        when(repository.buscarObraAtivaPorCodigo("nord2026")).thenReturn(Optional.of(obra()));
        when(repository.emailExiste("joao@obra.com")).thenReturn(true);
        assertEquals(CadastroServiceImpl.MSG_CADASTRO, service.cadastrar(form(), "ip").getTxMensagem());
        verify(repository, never()).inserirPendente(any(), any(), any(), any(), any());
        verify(email).enviar(eq("joao@obra.com"), any(), any());
    }

    @Test
    void cadastroRespeitaLimiteDeTentativas() {
        doThrow(new LimiteRequisicoesException("muitas")).when(limite).registrar(anyString(), org.mockito.ArgumentMatchers.anyInt());
        assertThrows(LimiteRequisicoesException.class, () -> service.cadastrar(form(), "ip"));
        verify(repository, never()).buscarObraAtivaPorCodigo(any());
    }

    // ---------- esqueci a senha ----------

    @Test
    void esqueciSenhaEnviaLinkComTokenQueSoFicaComoHash() {
        when(usuarios.buscarPorEmail("joao@obra.com")).thenReturn(Optional.of(usuario(true, "APROVADO")));
        EsqueciSenhaForm f = new EsqueciSenhaForm();
        f.setNmEmail(" JOAO@obra.com ");
        assertEquals(CadastroServiceImpl.MSG_ESQUECI, service.esqueciSenha(f, "ip").getTxMensagem());

        ArgumentCaptor<String> texto = ArgumentCaptor.forClass(String.class);
        verify(email).enviar(eq("joao@obra.com"), any(), texto.capture());
        String link = texto.getValue().lines().filter(l -> l.startsWith("https://app.nord/redefinir-senha?token=")).findFirst().orElseThrow();
        String token = link.substring(link.indexOf('=') + 1);
        assertEquals(43, token.length());
        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<LocalDateTime> expira = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(repository).inserirToken(eq(9L), hash.capture(), expira.capture());
        assertEquals(CadastroServiceImpl.sha256(token), hash.getValue());
        assertEquals(LocalDateTime.now(RELOGIO).plusMinutes(30), expira.getValue());
        verify(repository).invalidarTokens(9L);
    }

    @Test
    void esqueciSenhaNaoRevelaEmailInexistenteOuInativo() {
        EsqueciSenhaForm f = new EsqueciSenhaForm();
        f.setNmEmail("ninguem@obra.com");
        when(usuarios.buscarPorEmail("ninguem@obra.com")).thenReturn(Optional.empty());
        assertEquals(CadastroServiceImpl.MSG_ESQUECI, service.esqueciSenha(f, "ip").getTxMensagem());
        f.setNmEmail("joao@obra.com");
        when(usuarios.buscarPorEmail("joao@obra.com")).thenReturn(Optional.of(usuario(false, "APROVADO")));
        assertEquals(CadastroServiceImpl.MSG_ESQUECI, service.esqueciSenha(f, "ip").getTxMensagem());
        verify(repository, never()).inserirToken(anyLong(), any(), any());
        verify(email, never()).enviar(any(), any(), any());
    }

    // ---------- redefinir ----------

    private RedefinirSenhaForm redefinir(String senha) {
        RedefinirSenhaForm f = new RedefinirSenhaForm();
        f.setToken("tok");
        f.setNovaSenha(senha);
        return f;
    }

    @Test
    void redefinirTrocaASenhaUsaOTokenEDerrubaSessoes() {
        when(repository.buscarToken(CadastroServiceImpl.sha256("tok"))).thenReturn(Optional.of(
                new CadastroRepository.TokenRedefinicao(5L, 9L, LocalDateTime.now(RELOGIO).plusMinutes(10), null)));
        when(usuarios.buscarPorId(9L)).thenReturn(Optional.of(usuario(true, "APROVADO")));
        when(repository.usarToken(5L)).thenReturn(true);
        service.redefinirSenha(redefinir("nova-senha-123"), "ip");
        verify(usuarios).alterarSenha(9L, "HASH");
        verify(repository).invalidarTokens(9L);
        verify(sessao).invalidar(9L);
    }

    @Test
    void redefinirRecusaTokenExpiradoUsadoOuInexistente() {
        when(repository.buscarToken(any())).thenReturn(Optional.of(
                new CadastroRepository.TokenRedefinicao(5L, 9L, LocalDateTime.now(RELOGIO).minusMinutes(1), null)));
        assertThrows(EntradaInvalidaException.class, () -> service.redefinirSenha(redefinir("nova-senha-123"), "ip"));
        when(repository.buscarToken(any())).thenReturn(Optional.of(
                new CadastroRepository.TokenRedefinicao(5L, 9L, LocalDateTime.now(RELOGIO).plusMinutes(5), LocalDateTime.now(RELOGIO))));
        assertThrows(EntradaInvalidaException.class, () -> service.redefinirSenha(redefinir("nova-senha-123"), "ip"));
        when(repository.buscarToken(any())).thenReturn(Optional.empty());
        assertThrows(EntradaInvalidaException.class, () -> service.redefinirSenha(redefinir("nova-senha-123"), "ip"));
        verify(usuarios, never()).alterarSenha(anyLong(), any());
    }

    @Test
    void redefinirRecusaSenhaFracaECorrida() {
        when(repository.buscarToken(any())).thenReturn(Optional.of(
                new CadastroRepository.TokenRedefinicao(5L, 9L, LocalDateTime.now(RELOGIO).plusMinutes(10), null)));
        assertThrows(EntradaInvalidaException.class, () -> service.redefinirSenha(redefinir("curta"), "ip"));
        when(usuarios.buscarPorId(9L)).thenReturn(Optional.of(usuario(true, "APROVADO")));
        when(repository.usarToken(5L)).thenReturn(false);
        assertThrows(EntradaInvalidaException.class, () -> service.redefinirSenha(redefinir("nova-senha-123"), "ip"));
        verify(usuarios, never()).alterarSenha(anyLong(), any());
        assertTrue(true);
    }
}
