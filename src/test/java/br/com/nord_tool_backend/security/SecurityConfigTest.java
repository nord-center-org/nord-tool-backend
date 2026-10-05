package br.com.nord_tool_backend.security;

import br.com.nord_tool_backend.controller.read.AuthReadController;
import br.com.nord_tool_backend.controller.read.HealthReadController;
import br.com.nord_tool_backend.controller.write.AuthWriteController;
import br.com.nord_tool_backend.dto.LoginResponseDto;
import br.com.nord_tool_backend.dto.UsuarioDto;
import br.com.nord_tool_backend.handler.GlobalExceptionHandler;
import br.com.nord_tool_backend.service.AuthService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityConfigTest {

    private static final String SEGREDO = "segredo-de-teste-com-mais-de-32-bytes!!";

    /** Rota qualquer, protegida, para provar o 401/200. */
    @RestController
    static class RotaProtegida {
        @GetMapping("/api/v1/nord-tool/apartamentoVistoria")
        String listar() { return "ok"; }
    }

    @Nested
    @WebMvcTest(controllers = {RotaProtegida.class, HealthReadController.class, AuthReadController.class, AuthWriteController.class})
    @Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class, SecurityProperties.class, GlobalExceptionHandler.class})
    @TestPropertySource(properties = {"nord-tool.security.enabled=true", "nord-tool.security.jwt-secret=" + SEGREDO})
    class Ligada {
        @Autowired MockMvc mvc;
        @Autowired JwtService jwt;
        @MockBean AuthService authService;

        private String token() {
            return jwt.gerar(1L, "a@b.com", "ADMIN", List.of("*:ESCRITA"), Instant.now());
        }

        @Test
        void rotaProtegidaSemTokenRetorna401() throws Exception {
            mvc.perform(get("/api/v1/nord-tool/apartamentoVistoria"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.txMensagem").exists());
        }

        @Test
        void rotaProtegidaComTokenRetorna200() throws Exception {
            mvc.perform(get("/api/v1/nord-tool/apartamentoVistoria").header("Authorization", "Bearer " + token()))
                    .andExpect(status().isOk());
        }

        @Test
        void tokenInvalidoOuExpiradoRetorna401() throws Exception {
            mvc.perform(get("/api/v1/nord-tool/apartamentoVistoria").header("Authorization", "Bearer lixo"))
                    .andExpect(status().isUnauthorized());
            String expirado = jwt.gerar(1L, "a@b.com", "ADMIN", List.of(), Instant.now().minusSeconds(7200));
            mvc.perform(get("/api/v1/nord-tool/apartamentoVistoria").header("Authorization", "Bearer " + expirado))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void loginEHealthSaoPublicos() throws Exception {
            when(authService.login(any())).thenReturn(new LoginResponseDto("tok", "x", 30, new UsuarioDto()));
            mvc.perform(post("/api/v1/nord-tool/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"a@b.com\",\"senha\":\"x\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.body.token").value("tok"));
            mvc.perform(get("/nord-tool/health")).andExpect(status().isOk());
        }

        @Test
        void loginSemCorpoValidoRetorna400() throws Exception {
            mvc.perform(post("/api/v1/nord-tool/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void meERefreshExigemToken() throws Exception {
            mvc.perform(get("/api/v1/nord-tool/auth/me")).andExpect(status().isUnauthorized());
            mvc.perform(post("/api/v1/nord-tool/auth/refresh")).andExpect(status().isUnauthorized());
        }

        @Test
        void meComTokenUsaOIdDoToken() throws Exception {
            when(authService.me(1L)).thenReturn(new UsuarioDto(1L, "Admin", "a@b.com", "ADMIN", List.of()));
            mvc.perform(get("/api/v1/nord-tool/auth/me").header("Authorization", "Bearer " + token()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.body.email").value("a@b.com"));
        }

        @Test
        void preflightOptionsEhLiberado() throws Exception {
            mvc.perform(options("/api/v1/nord-tool/apartamentoVistoria")
                            .header("Origin", "http://localhost:5173")
                            .header("Access-Control-Request-Method", "GET"))
                    .andExpect(status().is2xxSuccessful());
        }
    }

    @Nested
    @WebMvcTest(controllers = {RotaProtegida.class})
    @Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class, SecurityProperties.class, GlobalExceptionHandler.class})
    @TestPropertySource(properties = {"nord-tool.security.enabled=false"})
    class Desligada {
        @Autowired MockMvc mvc;

        @Test
        void comSegurancaDesligadaTudoFicaAberto() throws Exception {
            mvc.perform(get("/api/v1/nord-tool/apartamentoVistoria")).andExpect(status().isOk());
        }
    }
}
