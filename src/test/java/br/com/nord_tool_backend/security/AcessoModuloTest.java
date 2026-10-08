package br.com.nord_tool_backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AcessoModuloTest {

    private static Authentication auth(String... permissoes) {
        return new UsernamePasswordAuthenticationToken(new UsuarioAutenticado(1L, "a@b.com", "X", Arrays.asList(permissoes)), null, Collections.emptyList());
    }

    private static AcessoModulo acesso(boolean segurancaLigada) {
        SecurityProperties props = mock(SecurityProperties.class);
        when(props.isEnabled()).thenReturn(segurancaLigada);
        return new AcessoModulo(props);
    }

    @Test
    void escritaIncluiLeituraMasNaoOContrario() {
        AcessoModulo a = acesso(true);
        assertTrue(a.leitura(auth("FINANCEIRO:ESCRITA"), "FINANCEIRO"));
        assertTrue(a.escrita(auth("FINANCEIRO:ESCRITA"), "FINANCEIRO"));
        assertTrue(a.leitura(auth("FINANCEIRO:LEITURA"), "FINANCEIRO"));
        assertFalse(a.escrita(auth("FINANCEIRO:LEITURA"), "FINANCEIRO"));
    }

    @Test
    void outroModuloNaoDaAcesso() {
        AcessoModulo a = acesso(true);
        assertFalse(a.leitura(auth("CASAMENTO:ESCRITA"), "FINANCEIRO"));
        assertFalse(a.leitura(auth("FINANCEIRO"), "FINANCEIRO"));
        assertFalse(a.leitura(auth(), "FINANCEIRO"));
    }

    @Test
    void coringaVaiParaTodosOsModulos() {
        assertTrue(acesso(true).escrita(auth("*:ESCRITA"), "FINANCEIRO"));
        assertFalse(acesso(true).escrita(auth("*:LEITURA"), "FINANCEIRO"));
    }

    @Test
    void semAutenticacaoOuPrincipalEstranhoNegaComSegurancaLigada() {
        AcessoModulo a = acesso(true);
        assertFalse(a.leitura(null, "FINANCEIRO"));
        assertFalse(a.leitura(new UsernamePasswordAuthenticationToken("anonimo", null, Collections.emptyList()), "FINANCEIRO"));
    }

    @Test
    void comSegurancaDesligadaAApiFicaAbertaComoAntes() {
        assertTrue(acesso(false).leitura(null, "FINANCEIRO"));
        assertTrue(acesso(false).escrita(null, "FINANCEIRO"));
    }
}
