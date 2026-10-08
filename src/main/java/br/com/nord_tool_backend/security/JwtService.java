package br.com.nord_tool_backend.security;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Emissão e validação dos tokens de acesso (JWT). */
public interface JwtService {

    long getInactivityMinutes();

    Instant calcularExpiracao(Instant agora);

    String gerar(Long idUsuario, String email, String perfil, List<String> permissoes, Instant agora);

    /** Devolve o principal se o token for válido e não estiver expirado. */
    Optional<UsuarioAutenticado> validar(String token);
}
