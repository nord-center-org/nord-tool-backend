package br.com.nord_tool_backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service @Slf4j
public class JwtServiceImpl implements JwtService {

    private static final int MIN_SECRET_BYTES = 32;

    private final SecurityProperties props;
    private final SecretKey key;

    public JwtServiceImpl(SecurityProperties props) {
        this.props = props;
        this.key = criarChave(props);
    }

    private static SecretKey criarChave(SecurityProperties props) {
        String segredo = props.getJwtSecret();
        if (segredo != null && !segredo.trim().isEmpty()) {
            byte[] bytes = segredo.getBytes(StandardCharsets.UTF_8);
            if (bytes.length < MIN_SECRET_BYTES) {
                throw new IllegalStateException("NORD_JWT_SECRET deve ter ao menos 32 bytes");
            }
            return Keys.hmacShaKeyFor(bytes);
        }
        if (props.isEnabled() && !props.isAllowEphemeralSecret()) {
            throw new IllegalStateException("NORD_JWT_SECRET é obrigatório quando a segurança está ligada");
        }
        log.warn("NORD_JWT_SECRET ausente: usando chave temporária (tokens deixam de valer ao reiniciar).");
        byte[] aleatoria = new byte[MIN_SECRET_BYTES];
        new SecureRandom().nextBytes(aleatoria);
        return Keys.hmacShaKeyFor(aleatoria);
    }

    @Override
    public long getInactivityMinutes() {
        return props.getInactivityMinutes();
    }

    @Override
    public Instant calcularExpiracao(Instant agora) {
        return agora.plusSeconds(props.getInactivityMinutes() * 60);
    }

    @Override
    public String gerar(Long idUsuario, String email, String perfil, List<String> permissoes, Instant agora) {
        return Jwts.builder()
                .setSubject(String.valueOf(idUsuario))
                .claim("email", email)
                .claim("perfil", perfil)
                .claim("permissoes", permissoes)
                .setIssuedAt(Date.from(agora))
                .setExpiration(Date.from(calcularExpiracao(agora)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<UsuarioAutenticado> validar(String token) {
        try {
            Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
            List<String> permissoes = claims.get("permissoes", List.class);
            return Optional.of(new UsuarioAutenticado(
                    Long.valueOf(claims.getSubject()),
                    claims.get("email", String.class),
                    claims.get("perfil", String.class),
                    permissoes == null ? List.of() : permissoes));
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
