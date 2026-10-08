package br.com.nord_tool_backend.controller;

import br.com.nord_tool_backend.controller.read.FinanceiroMesReadController;
import br.com.nord_tool_backend.controller.response.NordHttpEnum;
import br.com.nord_tool_backend.controller.write.FinanceiroMesWriteController;
import br.com.nord_tool_backend.dto.FinanceiroConfiguracaoDto;
import br.com.nord_tool_backend.dto.FinanceiroFaturaLeituraDto;
import br.com.nord_tool_backend.dto.FinanceiroFechamentoDto;
import br.com.nord_tool_backend.dto.FinanceiroGeracaoDto;
import br.com.nord_tool_backend.dto.FinanceiroProjecaoLinhaDto;
import br.com.nord_tool_backend.dto.FinanceiroProjecaoMesDto;
import br.com.nord_tool_backend.dto.FinanceiroRecorrenciaDto;
import br.com.nord_tool_backend.excepetion.ValidacaoException;
import br.com.nord_tool_backend.handler.GlobalExceptionHandler;
import br.com.nord_tool_backend.security.AcessoModulo;
import br.com.nord_tool_backend.security.JwtAuthenticationFilter;
import br.com.nord_tool_backend.security.JwtService;
import br.com.nord_tool_backend.security.SecurityConfig;
import br.com.nord_tool_backend.security.SecurityProperties;
import br.com.nord_tool_backend.service.FinanceiroProjecaoService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {FinanceiroMesReadController.class, FinanceiroMesWriteController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class, SecurityProperties.class, GlobalExceptionHandler.class, AcessoModulo.class})
@TestPropertySource(properties = {"nord-tool.security.enabled=true", "nord-tool.security.jwt-secret=segredo-de-teste-com-mais-de-32-bytes!!"})
class FinanceiroMesControllerTest {

    private static final String BASE = "/api/v1/nord-tool/financeiro";

    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @MockBean FinanceiroProjecaoService service;

    private String auth() {
        return "Bearer " + jwt.gerar(7L, "a@b.com", "ADMIN", List.of("*:ESCRITA"), Instant.now());
    }

    private FinanceiroProjecaoMesDto mes() {
        FinanceiroProjecaoLinhaDto fatura = new FinanceiroProjecaoLinhaDto(4L, "Fatura", "SAIDA", "RITMO_FATURA",
                new BigDecimal("1200.00"), new BigDecimal("2320.00"), "RITMO", "Fatura em aberto");
        return new FinanceiroProjecaoMesDto("2026-10", false, true, true, new BigDecimal("1500.00"), Collections.emptyList(),
                Collections.singletonList(fatura), new BigDecimal("5000.00"), new BigDecimal("2320.00"), new BigDecimal("4180.00"),
                new BigDecimal("500.00"), new BigDecimal("3680.00"), "VERDE", 3);
    }

    private static final String RECORRENCIA = "{\"idCategoria\":5,\"idPessoa\":1,\"vlRecorrencia\":1956.42,\"dtInicio\":\"2026-01-01\"}";

    @Test
    void todasAsRotasNovasExigemLogin() throws Exception {
        mvc.perform(get(BASE + "/mes/2026-10")).andExpect(status().isUnauthorized());
        mvc.perform(get(BASE + "/configuracao")).andExpect(status().isUnauthorized());
        mvc.perform(get(BASE + "/recorrencias")).andExpect(status().isUnauthorized());
        mvc.perform(get(BASE + "/lancamentos/5/leituras")).andExpect(status().isUnauthorized());
        mvc.perform(put(BASE + "/mes/2026-10/saldo-inicial").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        mvc.perform(post(BASE + "/mes/2026-10/fechar")).andExpect(status().isUnauthorized());
        mvc.perform(post(BASE + "/mes/2026-10/reabrir")).andExpect(status().isUnauthorized());
        mvc.perform(put(BASE + "/configuracao").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        mvc.perform(post(BASE + "/recorrencias").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        mvc.perform(put(BASE + "/recorrencias/1").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        mvc.perform(post(BASE + "/recorrencias/gerar/2026-10")).andExpect(status().isUnauthorized());
    }

    @Test
    void contaDoMesSaiSemCacheComOsValoresEAFiltroDePessoa() throws Exception {
        when(service.obterMes("2026-10", 2L)).thenReturn(mes());

        mvc.perform(get(BASE + "/mes/2026-10").param("idPessoa", "2").header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.body.competencia").value("2026-10"))
                .andExpect(jsonPath("$.body.saldoFinal").value(4180.00))
                .andExpect(jsonPath("$.body.situacao").value("VERDE"))
                .andExpect(jsonPath("$.body.estimado").value(true))
                .andExpect(jsonPath("$.body.saidas[0].origem").value("RITMO"))
                .andExpect(jsonPath("$.body.saidas[0].projetado").value(2320.00));
        verify(service).obterMes("2026-10", 2L);

        when(service.obterMes("2026-10", null)).thenReturn(mes());
        mvc.perform(get(BASE + "/mes/2026-10").header("Authorization", auth())).andExpect(status().isOk());
        verify(service).obterMes(eq("2026-10"), isNull());
    }

    @Test
    void competenciaInvalidaVira400() throws Exception {
        when(service.obterMes("10-2026", null)).thenThrow(new ValidacaoException(NordHttpEnum.HTTP_400, "Competência inválida", null));
        mvc.perform(get(BASE + "/mes/10-2026").header("Authorization", auth())).andExpect(status().isBadRequest());
    }

    @Test
    void fecharUsaOUsuarioDoTokenEDevolveAsGeradas() throws Exception {
        when(service.fechar("2026-10", 7L)).thenReturn(new FinanceiroFechamentoDto(mes(), 2));

        mvc.perform(post(BASE + "/mes/2026-10/fechar").header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.body.recorrenciasGeradas").value(2))
                .andExpect(jsonPath("$.body.mes.competencia").value("2026-10"));
        verify(service).fechar("2026-10", 7L);
    }

    @Test
    void fecharMesJaFechadoVira400ComAMensagem() throws Exception {
        when(service.fechar(any(), any())).thenThrow(new ValidacaoException(NordHttpEnum.HTTP_400, "O mês de outubro de 2026 já está fechado", null));

        mvc.perform(post(BASE + "/mes/2026-10/fechar").header("Authorization", auth()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.txMensagem").value("O mês de outubro de 2026 já está fechado"));
    }

    @Test
    void reabrirEGerarRecorrenciasRepassamACompetencia() throws Exception {
        when(service.reabrir("2026-10")).thenReturn(mes());
        when(service.gerarRecorrencias("2026-11", 7L)).thenReturn(new FinanceiroGeracaoDto("2026-11", 3));

        mvc.perform(post(BASE + "/mes/2026-10/reabrir").header("Authorization", auth())).andExpect(status().isOk());
        mvc.perform(post(BASE + "/recorrencias/gerar/2026-11").header("Authorization", auth()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.body.criados").value(3));
    }

    @Test
    void saldoInicialAceitaValorOuVazioERecusaLixo() throws Exception {
        when(service.definirSaldoInicial(eq("2026-10"), any())).thenReturn(mes());

        mvc.perform(put(BASE + "/mes/2026-10/saldo-inicial").header("Authorization", auth()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"vlSaldoInicial\":74.96}")).andExpect(status().isOk());
        mvc.perform(put(BASE + "/mes/2026-10/saldo-inicial").header("Authorization", auth()).contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andExpect(status().isOk());
        mvc.perform(put(BASE + "/mes/2026-10/saldo-inicial").header("Authorization", auth()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"vlSaldoInicial\":\"abc\"}")).andExpect(status().isBadRequest());
        mvc.perform(put(BASE + "/mes/2026-10/saldo-inicial").header("Authorization", auth()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"vlSaldoInicial\":1.234}")).andExpect(status().isBadRequest());
    }

    @Test
    void configuracaoValidaOsLimites() throws Exception {
        when(service.atualizarConfiguracao(any())).thenReturn(new FinanceiroConfiguracaoDto(new BigDecimal("500.00"), 3, 8, 8));
        String ok = "{\"vlMetaSaldo\":500,\"nrMesesMedia\":3,\"nrDiaConferencia\":8,\"nrDiaFechamentoFatura\":8}";

        mvc.perform(put(BASE + "/configuracao").header("Authorization", auth()).contentType(MediaType.APPLICATION_JSON).content(ok))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"));
        for (String ruim : new String[]{
                ok.replace("\"vlMetaSaldo\":500", "\"vlMetaSaldo\":-1"),
                ok.replace("\"nrMesesMedia\":3", "\"nrMesesMedia\":0"),
                ok.replace("\"nrMesesMedia\":3", "\"nrMesesMedia\":13"),
                ok.replace("\"nrDiaConferencia\":8", "\"nrDiaConferencia\":32"),
                ok.replace(",\"nrDiaFechamentoFatura\":8", ""),
                "{}"}) {
            mvc.perform(put(BASE + "/configuracao").header("Authorization", auth()).contentType(MediaType.APPLICATION_JSON).content(ruim))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void recorrenciaValidaOCorpoEConflitoDeVersaoVira409() throws Exception {
        FinanceiroRecorrenciaDto dto = new FinanceiroRecorrenciaDto(30L, 5L, "Apartamento", "SAIDA", 1L, "Nick", null,
                new BigDecimal("1956.42"), null, LocalDate.of(2026, 1, 1), null, true, 1);
        when(service.criarRecorrencia(any())).thenReturn(dto);
        when(service.listarRecorrencias()).thenReturn(Collections.singletonList(dto));

        mvc.perform(post(BASE + "/recorrencias").header("Authorization", auth()).contentType(MediaType.APPLICATION_JSON).content(RECORRENCIA))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.body.idRecorrencia").value(30));
        mvc.perform(get(BASE + "/recorrencias").header("Authorization", auth()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.body[0].dtInicio").value("01/01/2026"));
        for (String ruim : new String[]{
                RECORRENCIA.replace("\"idCategoria\":5,", ""),
                RECORRENCIA.replace("\"idPessoa\":1,", ""),
                RECORRENCIA.replace("1956.42", "0"),
                RECORRENCIA.replace(",\"dtInicio\":\"2026-01-01\"", ""),
                RECORRENCIA.replace("}", ",\"nrDia\":32}")}) {
            mvc.perform(post(BASE + "/recorrencias").header("Authorization", auth()).contentType(MediaType.APPLICATION_JSON).content(ruim))
                    .andExpect(status().isBadRequest());
        }

        when(service.atualizarRecorrencia(eq(30L), any())).thenThrow(new ValidacaoException(NordHttpEnum.HTTP_409, "O registro mudou. Sincronize e tente novamente.", null));
        mvc.perform(put(BASE + "/recorrencias/30").header("Authorization", auth()).contentType(MediaType.APPLICATION_JSON)
                .content(RECORRENCIA.replace("}", ",\"nrVersao\":1}"))).andExpect(status().isConflict());
    }

    @Test
    void leiturasDaFaturaSaemComDataBrasileira() throws Exception {
        when(service.listarLeituras(5L)).thenReturn(List.of(new FinanceiroFaturaLeituraDto(LocalDate.of(2026, 9, 24), new BigDecimal("1200.00"))));

        mvc.perform(get(BASE + "/lancamentos/5/leituras").header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body[0].dtLeitura").value("24/09/2026"))
                .andExpect(jsonPath("$.body[0].vlLeitura").value(1200.00));
    }
}
