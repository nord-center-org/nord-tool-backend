package br.com.nord_tool_backend.security;

import br.com.nord_tool_backend.domain.Usuario;
import br.com.nord_tool_backend.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminBootstrapRunnerTest {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);

    @Test
    void criaAdminApenasComTabelaVazia() {
        UsuarioRepository repo = mock(UsuarioRepository.class);
        when(repo.contar()).thenReturn(0L);
        when(repo.buscarIdPerfil("ADMIN")).thenReturn(Optional.of(3L));

        new AdminBootstrapRunner(repo, encoder, "admin@nord.com", "Admin", "senha-forte-123").run();

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(repo).inserir(captor.capture());
        Usuario criado = captor.getValue();
        assertEquals("admin@nord.com", criado.getNmEmail());
        assertEquals(3L, criado.getIdPerfil());
        assertNotEquals("senha-forte-123", criado.getNmSenhaHash());
        assertTrue(encoder.matches("senha-forte-123", criado.getNmSenhaHash()));
    }

    @Test
    void naoCriaSeJaExistemUsuarios() {
        UsuarioRepository repo = mock(UsuarioRepository.class);
        when(repo.contar()).thenReturn(1L);

        new AdminBootstrapRunner(repo, encoder, "admin@nord.com", "Admin", "senha-forte-123").run();

        verify(repo, never()).inserir(any());
    }

    @Test
    void naoFazNadaSemVariaveis() {
        UsuarioRepository repo = mock(UsuarioRepository.class);

        new AdminBootstrapRunner(repo, encoder, "", "Admin", "senha").run();

        verifyNoInteractions(repo);
    }

    @Test
    void falhaDeBancoNaoDerrubaAAplicacao() {
        UsuarioRepository repo = mock(UsuarioRepository.class);
        when(repo.contar()).thenThrow(new IllegalStateException("tabela inexistente"));

        assertDoesNotThrow(() -> new AdminBootstrapRunner(repo, encoder, "a@b.com", "A", "senha-forte-123").run());
    }
}
