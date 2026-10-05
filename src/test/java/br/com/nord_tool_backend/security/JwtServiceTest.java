package br.com.nord_tool_backend.security;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SEGREDO = "segredo-de-teste-com-mais-de-32-bytes!!";

    @Test
    void geraEValidaToken() {
        JwtService jwt = new JwtService(new SecurityProperties(true, SEGREDO, 30, false));
        String token = jwt.gerar(5L, "a@b.com", "ADMIN", List.of("*:ESCRITA"), Instant.now());

        UsuarioAutenticado u = jwt.validar(token).orElseThrow(AssertionError::new);
        assertEquals(5L, u.getId());
        assertEquals("a@b.com", u.getEmail());
        assertEquals(List.of("*:ESCRITA"), u.getPermissoes());
    }

    @Test
    void tokenExpiradoOuAdulteradoEhRejeitado() {
        JwtService jwt = new JwtService(new SecurityProperties(true, SEGREDO, 30, false));
        String expirado = jwt.gerar(5L, "a@b.com", "ADMIN", List.of(), Instant.now().minusSeconds(3600));
        assertFalse(jwt.validar(expirado).isPresent());

        String valido = jwt.gerar(5L, "a@b.com", "ADMIN", List.of(), Instant.now());
        assertFalse(jwt.validar(valido + "x").isPresent());
        assertFalse(jwt.validar("lixo").isPresent());
    }

    @Test
    void tokenAssinadoComOutraChaveEhRejeitado() {
        JwtService a = new JwtService(new SecurityProperties(true, SEGREDO, 30, false));
        JwtService b = new JwtService(new SecurityProperties(true, "outro-segredo-de-teste-com-32-bytes!!!", 30, false));
        assertFalse(b.validar(a.gerar(1L, "a@b.com", "ADMIN", List.of(), Instant.now())).isPresent());
    }

    @Test
    void segurancaLigadaSemSegredoFalhaNaSubida() {
        assertThrows(IllegalStateException.class, () -> new JwtService(new SecurityProperties(true, "", 30, false)));
        assertThrows(IllegalStateException.class, () -> new JwtService(new SecurityProperties(true, "curto", 30, false)));
    }

    @Test
    void segurancaDesligadaOuLocalUsaChaveTemporaria() {
        assertDoesNotThrow(() -> new JwtService(new SecurityProperties(false, "", 30, false)));
        assertDoesNotThrow(() -> new JwtService(new SecurityProperties(true, "", 30, true)));
    }
}
