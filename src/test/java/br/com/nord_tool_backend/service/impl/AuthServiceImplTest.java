package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.domain.PerfilPermissao;
import br.com.nord_tool_backend.domain.Usuario;
import br.com.nord_tool_backend.dto.LoginResponseDto;
import br.com.nord_tool_backend.excepetion.ValidacaoException;
import br.com.nord_tool_backend.form.AlterarSenhaForm;
import br.com.nord_tool_backend.form.LoginForm;
import br.com.nord_tool_backend.repository.UsuarioRepository;
import br.com.nord_tool_backend.security.JwtService;
import br.com.nord_tool_backend.security.SecurityProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

class AuthServiceImplTest {

    private static final String SEGREDO = "segredo-de-teste-com-mais-de-32-bytes!!";
    private static final Clock RELOGIO = Clock.fixed(Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS), ZoneId.of("UTC"));

    private UsuarioRepository repository;
    private BCryptPasswordEncoder encoder;
    private AuthServiceImpl service;
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        repository = mock(UsuarioRepository.class);
        encoder = new BCryptPasswordEncoder(4);
        jwtService = new JwtService(new SecurityProperties(true, SEGREDO, 30, false));
        service = new AuthServiceImpl(repository, encoder, jwtService, RELOGIO);
        when(repository.listarPermissoes(7L)).thenReturn(List.of(new PerfilPermissao("*", "ESCRITA")));
    }

    private Usuario usuario(int falhas, LocalDateTime bloqueadoAte) {
        Usuario u = new Usuario();
        u.setId(1L);
        u.setNmEmail("admin@nord.com");
        u.setNmNome("Admin");
        u.setNmSenhaHash(encoder.encode("senha-correta-123"));
        u.setIdPerfil(7L);
        u.setCdPerfil("ADMIN");
        u.setInAtivo(true);
        u.setNrFalhasLogin(falhas);
        u.setDhBloqueadoAte(bloqueadoAte);
        return u;
    }

    private LoginForm form(String email, String senha) {
        LoginForm f = new LoginForm();
        f.setEmail(email);
        f.setSenha(senha);
        return f;
    }

    @Test
    void loginValidoDevolveTokenEPermissoes() {
        when(repository.buscarPorEmail("admin@nord.com")).thenReturn(Optional.of(usuario(2, null)));

        LoginResponseDto resposta = service.login(form("admin@nord.com", "senha-correta-123"));

        assertEquals("ADMIN", resposta.getUsuario().getPerfil());
        assertEquals(30, resposta.getInatividadeMinutos());
        assertEquals("*", resposta.getUsuario().getPermissoes().get(0).getCdModulo());
        assertEquals(RELOGIO.instant().plusSeconds(30 * 60).toString(), resposta.getExpiraEm());
        assertTrue(jwtService.validar(resposta.getToken()).isPresent());
        verify(repository).registrarLoginSucesso(1L);
    }

    @Test
    void emailInexistenteEhSenhaErradaTemMesmaMensagemGenerica() {
        when(repository.buscarPorEmail("x@nord.com")).thenReturn(Optional.empty());
        when(repository.buscarPorEmail("admin@nord.com")).thenReturn(Optional.of(usuario(0, null)));

        ValidacaoException a = assertThrows(ValidacaoException.class, () -> service.login(form("x@nord.com", "qualquer")));
        ValidacaoException b = assertThrows(ValidacaoException.class, () -> service.login(form("admin@nord.com", "errada")));

        assertEquals("E-mail ou senha inválidos", a.getMessage());
        assertEquals(a.getMessage(), b.getMessage());
        verify(repository).registrarFalha(1L, 1, null);
    }

    @Test
    void quintaFalhaBloqueiaPorQuinzeMinutos() {
        when(repository.buscarPorEmail("admin@nord.com")).thenReturn(Optional.of(usuario(4, null)));

        assertThrows(ValidacaoException.class, () -> service.login(form("admin@nord.com", "errada")));

        ArgumentCaptor<LocalDateTime> ate = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(repository).registrarFalha(eq(1L), eq(0), ate.capture());
        assertEquals(LocalDateTime.now(RELOGIO).plusMinutes(15), ate.getValue());
    }

    @Test
    void contaBloqueadaRejeitaMesmoComSenhaCorreta() {
        LocalDateTime ate = LocalDateTime.now(RELOGIO).plusMinutes(10);
        when(repository.buscarPorEmail("admin@nord.com")).thenReturn(Optional.of(usuario(0, ate)));

        ValidacaoException ex = assertThrows(ValidacaoException.class,
                () -> service.login(form("admin@nord.com", "senha-correta-123")));

        assertTrue(ex.getMessage().contains("bloqueada"));
        assertTrue(ex.getMessage().contains("10 minuto"));
        verify(repository, never()).registrarLoginSucesso(anyLong());
    }

    @Test
    void bloqueioExpiradoPermiteLogin() {
        LocalDateTime ate = LocalDateTime.now(RELOGIO).minusMinutes(1);
        when(repository.buscarPorEmail("admin@nord.com")).thenReturn(Optional.of(usuario(0, ate)));

        assertNotNull(service.login(form("admin@nord.com", "senha-correta-123")).getToken());
    }

    @Test
    void usuarioInativoNaoLogaENaoContaFalha() {
        Usuario u = usuario(0, null);
        u.setInAtivo(false);
        when(repository.buscarPorEmail("admin@nord.com")).thenReturn(Optional.of(u));

        assertThrows(ValidacaoException.class, () -> service.login(form("admin@nord.com", "senha-correta-123")));
        verify(repository, never()).registrarFalha(anyLong(), anyInt(), any());
    }

    @Test
    void refreshRenovaTokenDeUsuarioAtivo() {
        when(repository.buscarPorId(1L)).thenReturn(Optional.of(usuario(0, null)));

        assertTrue(jwtService.validar(service.refresh(1L).getToken()).isPresent());
    }

    @Test
    void refreshDeUsuarioInexistenteEh401() {
        when(repository.buscarPorId(9L)).thenReturn(Optional.empty());

        assertThrows(ValidacaoException.class, () -> service.refresh(9L));
    }

    @Test
    void alterarSenhaGravaNovoHash() {
        when(repository.buscarPorId(1L)).thenReturn(Optional.of(usuario(0, null)));
        AlterarSenhaForm f = new AlterarSenhaForm();
        f.setSenhaAtual("senha-correta-123");
        f.setNovaSenha("outra-senha-forte-456");

        service.alterarSenha(1L, f);

        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(repository).alterarSenha(eq(1L), hash.capture());
        assertTrue(encoder.matches("outra-senha-forte-456", hash.getValue()));
    }

    @Test
    void alterarSenhaComSenhaAtualErradaFalha() {
        when(repository.buscarPorId(1L)).thenReturn(Optional.of(usuario(0, null)));
        AlterarSenhaForm f = new AlterarSenhaForm();
        f.setSenhaAtual("errada");
        f.setNovaSenha("outra-senha-forte-456");

        assertThrows(ValidacaoException.class, () -> service.alterarSenha(1L, f));
        verify(repository, never()).alterarSenha(anyLong(), any());
    }
}
