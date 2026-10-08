package br.com.nord_tool_backend.controller;

import br.com.nord_tool_backend.controller.read.FinanceiroInvestimentoReadController;
import br.com.nord_tool_backend.controller.response.NordHttpEnum;
import br.com.nord_tool_backend.controller.write.FinanceiroInvestimentoWriteController;
import br.com.nord_tool_backend.dto.FinanceiroAtivoDto;
import br.com.nord_tool_backend.dto.FinanceiroInvestimentoDto;
import br.com.nord_tool_backend.excepetion.ValidacaoException;
import br.com.nord_tool_backend.handler.GlobalExceptionHandler;
import br.com.nord_tool_backend.security.AcessoModulo;
import br.com.nord_tool_backend.security.JwtAuthenticationFilter;
import br.com.nord_tool_backend.security.JwtService;
import br.com.nord_tool_backend.security.SecurityConfig;
import br.com.nord_tool_backend.security.SecurityProperties;
import br.com.nord_tool_backend.service.FinanceiroInvestimentoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {FinanceiroInvestimentoReadController.class, FinanceiroInvestimentoWriteController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class, SecurityProperties.class, GlobalExceptionHandler.class, AcessoModulo.class})
@TestPropertySource(properties = {"nord-tool.security.enabled=true", "nord-tool.security.jwt-secret=segredo-de-teste-com-mais-de-32-bytes!!"})
class FinanceiroInvestimentoControllerTest {

    private static final String BASE = "/api/v1/nord-tool/financeiro/investimentos";
    private static final String OPERACAO = "{\"cdRequisicao\":\"11111111-1111-1111-1111-111111111111\",\"dtOperacao\":\"2026-10-01\",\"cdTipo\":\"COMPRA\",\"qtCotas\":10,\"vlPreco\":150.00}";

    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @MockBean FinanceiroInvestimentoService service;

    private String auth() {
        return "Bearer " + jwt.gerar(7L, "a@b.com", "ADMIN", List.of("*:ESCRITA"), Instant.now());
    }

    private FinanceiroAtivoDto ativo() {
        return new FinanceiroAtivoDto(1L, "HGLG11", "Logística", 1L, "Nick", 10, new BigDecimal("150.00"), new BigDecimal("1500.00"),
                new BigDecimal("160.00"), "08/10/2026 11:30", true, new BigDecimal("1600.00"), new BigDecimal("100.00"), new BigDecimal("6.67"),
                new BigDecimal("11.00"),
                Collections.singletonList(new FinanceiroAtivoDto.Operacao(1L, LocalDate.of(2026, 10, 1), "COMPRA", 10, new BigDecimal("150.00"))),
                Collections.emptyList(), 1);
    }

    @Test
    void todasAsRotasExigemLogin() throws Exception {
        mvc.perform(get(BASE)).andExpect(status().isUnauthorized());
        mvc.perform(post(BASE + "/ativos").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        mvc.perform(put(BASE + "/ativos/1").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        mvc.perform(post(BASE + "/ativos/1/operacoes").contentType(MediaType.APPLICATION_JSON).content(OPERACAO)).andExpect(status().isUnauthorized());
        mvc.perform(delete(BASE + "/operacoes/1")).andExpect(status().isUnauthorized());
        mvc.perform(post(BASE + "/ativos/1/proventos").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        mvc.perform(delete(BASE + "/proventos/1")).andExpect(status().isUnauthorized());
        mvc.perform(post(BASE + "/ativos/1/proventos/sincronizar")).andExpect(status().isUnauthorized());
    }

    @Test
    void listaSaiSemCacheComAsDatasEmFormatoBrasileiro() throws Exception {
        when(service.listar(1L)).thenReturn(new FinanceiroInvestimentoDto(Collections.singletonList(ativo()), new BigDecimal("1500.00"),
                new BigDecimal("1600.00"), new BigDecimal("100.00"), new BigDecimal("6.67"), new BigDecimal("11.00"), new BigDecimal("11.00"), true));

        mvc.perform(get(BASE).param("idPessoa", "1").header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.body.ativos[0].cdTicker").value("HGLG11"))
                .andExpect(jsonPath("$.body.ativos[0].operacoes[0].dtOperacao").value("01/10/2026"))
                .andExpect(jsonPath("$.body.vlPatrimonio").value(1600.00))
                .andExpect(jsonPath("$.body.cotacaoAoVivo").value(true));
    }

    @Test
    void registraOperacaoComUUIDEDevolve201() throws Exception {
        when(service.registrarOperacao(any(), any())).thenReturn(ativo());
        mvc.perform(post(BASE + "/ativos/1/operacoes").contentType(MediaType.APPLICATION_JSON).content(OPERACAO).header("Authorization", auth()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.body.qtCotas").value(10));
    }

    @Test
    void operacaoSemCamposObrigatoriosVira400() throws Exception {
        mvc.perform(post(BASE + "/ativos/1/operacoes").contentType(MediaType.APPLICATION_JSON).content("{}").header("Authorization", auth()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void erroDeNegocioVira400ComAMensagem() throws Exception {
        when(service.registrarOperacao(any(), any())).thenThrow(new ValidacaoException(NordHttpEnum.HTTP_400, "Não há cotas suficientes para essa venda.", null));
        mvc.perform(post(BASE + "/ativos/1/operacoes").contentType(MediaType.APPLICATION_JSON).content(OPERACAO).header("Authorization", auth()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.txMensagem").value("Não há cotas suficientes para essa venda."));
    }

    @Test
    void conflitoDeVersaoVira409() throws Exception {
        when(service.atualizarAtivo(any(), any())).thenThrow(new ValidacaoException(NordHttpEnum.HTTP_409, "O fundo mudou. Sincronize e tente novamente.", null));
        mvc.perform(put(BASE + "/ativos/1").contentType(MediaType.APPLICATION_JSON).content("{\"nrVersao\":1}").header("Authorization", auth()))
                .andExpect(status().isConflict());
    }

    @Test
    void sincronizarDevolveQuantosForamImportados() throws Exception {
        when(service.sincronizarProventos(1L)).thenReturn(3);
        mvc.perform(post(BASE + "/ativos/1/proventos/sincronizar").header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.importados").value(3));
        verify(service).sincronizarProventos(1L);
    }

    @Test
    void excluirDevolveOFundoAtualizado() throws Exception {
        when(service.excluirOperacao(1L)).thenReturn(ativo());
        mvc.perform(delete(BASE + "/operacoes/1").header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.idAtivo").value(1));
    }
}
