package br.com.nord_tool_backend.guard;

import br.com.nord_tool_backend.controller.read.AuthReadController;
import br.com.nord_tool_backend.controller.read.HealthReadController;
import br.com.nord_tool_backend.controller.write.AuthWriteController;
import br.com.nord_tool_backend.dto.LoginResponseDto;
import br.com.nord_tool_backend.dto.UsuarioDto;
import br.com.nord_tool_backend.handler.GlobalExceptionHandler;
import br.com.nord_tool_backend.security.AcessoModulo;
import br.com.nord_tool_backend.security.JwtAuthenticationFilter;
import br.com.nord_tool_backend.security.JwtService;
import br.com.nord_tool_backend.security.SecurityConfig;
import br.com.nord_tool_backend.security.SecurityProperties;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Guarda de segurança (exceção prevista em steering/nord-tool-backend/testing.md): contrato HTTP de
 * autenticação — 401 sem token ou com token inválido/expirado, rotas públicas, identidade vinda do token
 * e corpo de erro sem detalhes internos. Regras de negócio ficam nos *ServiceImplTest.
 */
class ContratoSegurancaGuardTest {

    private static final String SEGREDO = "segredo-de-teste-com-mais-de-32-bytes!!";
    private static final String ROTA_PROTEGIDA = "/api/v1/nord-tool/apartamentoVistoria";

    /** Rota qualquer, protegida, para provar o 401/200. */
    @RestController
    static class RotaProtegida {
        @GetMapping(ROTA_PROTEGIDA)
        String listar() { return "ok"; }
    }

    @Nested
    @WebMvcTest(controllers = {RotaProtegida.class, HealthReadController.class, AuthReadController.class, AuthWriteController.class})
    @Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class, SecurityProperties.class, GlobalExceptionHandler.class, AcessoModulo.class})
    @TestPropertySource(properties = {"nord-tool.security.enabled=true", "nord-tool.security.jwt-secret=" + SEGREDO})
    class Ligada {
        @Autowired MockMvc mvc;
        @Autowired JwtService jwt;
        @MockBean AuthService authService;

        private String token() {
            return jwt.gerar(1L, "a@b.com", "ADMIN", List.of("*:ESCRITA"), Instant.now());
        }

        @Test
        void rotaProtegidaSemTokenRetorna401ComCorpoPadrao() throws Exception {
            semDetalhesInternos(mvc.perform(get(ROTA_PROTEGIDA)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.nrStatus").value(401))
                    .andExpect(jsonPath("$.txMensagem").exists());
        }

        @Test
        void rotaProtegidaComTokenRetorna200() throws Exception {
            mvc.perform(get(ROTA_PROTEGIDA).header("Authorization", "Bearer " + token()))
                    .andExpect(status().isOk());
        }

        @Test
        void tokenInvalidoOuExpiradoRetorna401() throws Exception {
            semDetalhesInternos(mvc.perform(get(ROTA_PROTEGIDA).header("Authorization", "Bearer lixo")))
                    .andExpect(status().isUnauthorized());
            String expirado = jwt.gerar(1L, "a@b.com", "ADMIN", List.of(), Instant.now().minusSeconds(7200));
            mvc.perform(get(ROTA_PROTEGIDA).header("Authorization", "Bearer " + expirado))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void tokenAssinadoComOutraChaveRetorna401() throws Exception {
            JwtService outraChave = new JwtService(
                    new SecurityProperties(true, "outra-chave-de-teste-com-mais-de-32-bytes", 30, false));
            String forjado = outraChave.gerar(1L, "a@b.com", "ADMIN", List.of("*:ESCRITA"), Instant.now());
            mvc.perform(get(ROTA_PROTEGIDA).header("Authorization", "Bearer " + forjado))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void loginEHealthSaoPublicos() throws Exception {
            when(authService.login(any())).thenReturn(new LoginResponseDto("tok", "x", 30, new UsuarioDto()));
            mvc.perform(post("/api/v1/nord-tool/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"a@b.com\",\"senha\":\"x\"}"))
                    .andExpect(status().isOk());
            mvc.perform(get("/nord-tool/health")).andExpect(status().isOk());
        }

        @Test
        void meERefreshExigemToken() throws Exception {
            mvc.perform(get("/api/v1/nord-tool/auth/me")).andExpect(status().isUnauthorized());
            mvc.perform(post("/api/v1/nord-tool/auth/refresh")).andExpect(status().isUnauthorized());
        }

        @Test
        void identidadeVemDoToken() throws Exception {
            when(authService.me(1L)).thenReturn(new UsuarioDto(1L, "Admin", "a@b.com", "ADMIN", List.of()));
            mvc.perform(get("/api/v1/nord-tool/auth/me").header("Authorization", "Bearer " + token()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.body.email").value("a@b.com"));
        }

        @Test
        void preflightOptionsEhLiberado() throws Exception {
            mvc.perform(options(ROTA_PROTEGIDA)
                            .header("Origin", "http://localhost:5173")
                            .header("Access-Control-Request-Method", "GET"))
                    .andExpect(status().is2xxSuccessful());
        }

        private ResultActions semDetalhesInternos(ResultActions resultado) throws Exception {
            return resultado
                    .andExpect(jsonPath("$.body").doesNotExist())
                    .andExpect(content().string(not(containsString("Exception"))))
                    .andExpect(content().string(not(containsString("at br.com"))));
        }
    }

    /** Modo de transição até o frontend ter login (fase F6 do plano remove este modo). */
    @Nested
    @WebMvcTest(controllers = {RotaProtegida.class})
    @Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class, SecurityProperties.class, GlobalExceptionHandler.class, AcessoModulo.class})
    @TestPropertySource(properties = {"nord-tool.security.enabled=false"})
    class Desligada {
        @Autowired MockMvc mvc;

        @Test
        void comSegurancaDesligadaTudoFicaAberto() throws Exception {
            mvc.perform(get(ROTA_PROTEGIDA)).andExpect(status().isOk());
        }
    }
}
