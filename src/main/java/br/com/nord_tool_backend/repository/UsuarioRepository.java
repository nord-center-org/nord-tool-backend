package br.com.nord_tool_backend.repository;

import br.com.nord_tool_backend.domain.PerfilPermissao;
import br.com.nord_tool_backend.domain.Usuario;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository {
    Optional<Usuario> buscarPorEmail(String email);
    Optional<Usuario> buscarPorId(Long id);
    long contar();
    List<PerfilPermissao> listarPermissoes(Long idPerfil);
    Optional<Long> buscarIdPerfil(String cdPerfil);
    Usuario inserir(Usuario usuario);
    void registrarFalha(Long id, int falhas, LocalDateTime bloqueadoAte);
    void registrarLoginSucesso(Long id);
    void alterarSenha(Long id, String hash);
}
