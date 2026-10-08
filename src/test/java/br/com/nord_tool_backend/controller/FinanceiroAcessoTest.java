package br.com.nord_tool_backend.controller;

import br.com.nord_tool_backend.controller.read.FinanceiroInvestimentoReadController;
import br.com.nord_tool_backend.controller.read.FinanceiroMesReadController;
import br.com.nord_tool_backend.controller.read.FinanceiroReadController;
import br.com.nord_tool_backend.controller.write.FinanceiroInvestimentoWriteController;
import br.com.nord_tool_backend.controller.write.FinanceiroMesWriteController;
import br.com.nord_tool_backend.controller.write.FinanceiroWriteController;
import br.com.nord_tool_backend.handler.GlobalExceptionHandler;
import br.com.nord_tool_backend.security.AcessoModulo;
import br.com.nord_tool_backend.security.JwtAuthenticationFilter;
import br.com.nord_tool_backend.security.JwtService;
import br.com.nord_tool_backend.security.JwtServiceImpl;
import br.com.nord_tool_backend.security.SecurityConfig;
import br.com.nord_tool_backend.security.SecurityProperties;
import br.com.nord_tool_backend.service.FinanceiroInvestimentoService;
import br.com.nord_tool_backend.service.FinanceiroProjecaoService;
import br.com.nord_tool_backend.service.FinanceiroService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** O Financeiro tem dados pessoais: só quem tem o módulo FINANCEIRO (ou "*") entra, LEITURA consulta e ESCRITA altera. */
@WebMvcTest(controllers = {FinanceiroReadController.class, FinanceiroWriteController.class, FinanceiroMesReadController.class,
        FinanceiroMesWriteController.class, FinanceiroInvestimentoReadController.class, FinanceiroInvestimentoWriteController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtServiceImpl.class, SecurityProperties.class, GlobalExceptionHandler.class, AcessoModulo.class})
@TestPropertySource(properties = {"nord-tool.security.enabled=true", "nord-tool.security.jwt-secret=segredo-de-teste-com-mais-de-32-bytes!!"})
class FinanceiroAcessoTest {

    private static final String BASE = "/api/v1/nord-tool/financeiro";
    private static final String[] LEITURAS = {BASE + "/lancamentos", BASE + "/resumo", BASE + "/pessoas", BASE + "/categorias", BASE + "/mes/2026-10",
            BASE + "/configuracao", BASE + "/recorrencias", BASE + "/lancamentos/1/leituras", BASE + "/investimentos"};

    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @MockBean FinanceiroService service;
    @MockBean FinanceiroProjecaoService projecaoService;
    @MockBean FinanceiroInvestimentoService investimentoService;

    private String token(String... permissoes) {
        return "Bearer " + jwt.gerar(9L, "x@y.com", "COLABORADOR", List.of(permissoes), Instant.now());
    }

    @Test
    void quemSoTemOutroModuloNaoVeNemAlteraOFinanceiro() throws Exception {
        for (String rota : LEITURAS) {
            mvc.perform(get(rota).header("Authorization", token("CASAMENTO:ESCRITA", "APARTAMENTOS:ESCRITA"))).andExpect(status().isForbidden());
        }
        mvc.perform(post(BASE + "/lancamentos").contentType(MediaType.APPLICATION_JSON).content("{}").header("Authorization", token("CASAMENTO:ESCRITA")))
                .andExpect(status().isForbidden());
        mvc.perform(post(BASE + "/investimentos/ativos").contentType(MediaType.APPLICATION_JSON).content("{}").header("Authorization", token("CASAMENTO:ESCRITA")))
                .andExpect(status().isForbidden());
    }

    @Test
    void semNenhumaPermissaoNaoEntra() throws Exception {
        mvc.perform(get(BASE + "/lancamentos").header("Authorization", token())).andExpect(status().isForbidden());
    }

    @Test
    void leituraConsultaMasNaoAltera() throws Exception {
        for (String rota : LEITURAS) {
            mvc.perform(get(rota).header("Authorization", token("FINANCEIRO:LEITURA"))).andExpect(status().isOk());
        }
        String t = token("FINANCEIRO:LEITURA");
        mvc.perform(post(BASE + "/lancamentos").contentType(MediaType.APPLICATION_JSON).content("{}").header("Authorization", t)).andExpect(status().isForbidden());
        mvc.perform(put(BASE + "/configuracao").contentType(MediaType.APPLICATION_JSON).content("{}").header("Authorization", t)).andExpect(status().isForbidden());
        mvc.perform(post(BASE + "/mes/2026-10/fechar").header("Authorization", t)).andExpect(status().isForbidden());
        mvc.perform(post(BASE + "/investimentos/ativos/1/proventos/sincronizar").header("Authorization", t)).andExpect(status().isForbidden());
    }

    @Test
    void escritaConsultaEAltera() throws Exception {
        String t = token("FINANCEIRO:ESCRITA");
        mvc.perform(get(BASE + "/lancamentos").header("Authorization", t)).andExpect(status().isOk());
        mvc.perform(post(BASE + "/mes/2026-10/fechar").header("Authorization", t)).andExpect(status().isOk());
    }

    @Test
    void coringaDoAdministradorVale() throws Exception {
        mvc.perform(get(BASE + "/lancamentos").header("Authorization", token("*:ESCRITA"))).andExpect(status().isOk());
        mvc.perform(post(BASE + "/mes/2026-10/fechar").header("Authorization", token("*:ESCRITA"))).andExpect(status().isOk());
        mvc.perform(get(BASE + "/lancamentos").header("Authorization", token("*:LEITURA"))).andExpect(status().isOk());
        mvc.perform(post(BASE + "/mes/2026-10/fechar").header("Authorization", token("*:LEITURA"))).andExpect(status().isForbidden());
    }

    @Test
    void moduloComOutraCaixaOuSemSeparadorNaoEngana() throws Exception {
        mvc.perform(get(BASE + "/lancamentos").header("Authorization", token("financeiro:leitura"))).andExpect(status().isOk());
        mvc.perform(get(BASE + "/lancamentos").header("Authorization", token("FINANCEIRO"))).andExpect(status().isForbidden());
        mvc.perform(get(BASE + "/lancamentos").header("Authorization", token("FINANCEIRO2:ESCRITA", "XFINANCEIRO:ESCRITA"))).andExpect(status().isForbidden());
    }
}
