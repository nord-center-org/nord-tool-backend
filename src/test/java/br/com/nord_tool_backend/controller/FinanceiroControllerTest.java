package br.com.nord_tool_backend.controller;

import br.com.nord_tool_backend.controller.read.FinanceiroReadController;
import br.com.nord_tool_backend.controller.response.NordHttpEnum;
import br.com.nord_tool_backend.controller.write.FinanceiroWriteController;
import br.com.nord_tool_backend.domain.FinanceiroFiltro;
import br.com.nord_tool_backend.dto.FinanceiroLancamentoDto;
import br.com.nord_tool_backend.dto.FinanceiroListaDto;
import br.com.nord_tool_backend.dto.FinanceiroResumoDto;
import br.com.nord_tool_backend.excepetion.ValidacaoException;
import br.com.nord_tool_backend.handler.GlobalExceptionHandler;
import br.com.nord_tool_backend.security.AcessoModulo;
import br.com.nord_tool_backend.security.JwtAuthenticationFilter;
import br.com.nord_tool_backend.security.JwtService;
import br.com.nord_tool_backend.security.JwtServiceImpl;
import br.com.nord_tool_backend.security.SecurityConfig;
import br.com.nord_tool_backend.security.SecurityProperties;
import br.com.nord_tool_backend.service.FinanceiroService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {FinanceiroReadController.class, FinanceiroWriteController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtServiceImpl.class, SecurityProperties.class, GlobalExceptionHandler.class, AcessoModulo.class})
@TestPropertySource(properties = {"nord-tool.security.enabled=true", "nord-tool.security.jwt-secret=segredo-de-teste-com-mais-de-32-bytes!!"})
class FinanceiroControllerTest {

    private static final String BASE = "/api/v1/nord-tool/financeiro";
    private static final String UUID1 = "11111111-1111-1111-1111-111111111111";

    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @MockBean FinanceiroService service;

    private String auth() {
        return "Bearer " + jwt.gerar(1L, "a@b.com", "ADMIN", List.of("*:ESCRITA"), Instant.now());
    }

    private FinanceiroLancamentoDto dto() {
        return new FinanceiroLancamentoDto(5L, "2026-10", LocalDate.of(2026, 10, 8), 1L, "Fatura", "SAIDA", 1L, "Nick",
                "Cartão", new BigDecimal("1500.00"), false, null, null, 1L, "Nick", 1);
    }

    private static final String CORPO_VALIDO = "{\"cdRequisicao\":\"" + UUID1 + "\",\"dtLancamento\":\"2026-10-08\","
            + "\"idCategoria\":1,\"idPessoa\":1,\"vlLancamento\":1500.00}";

    @Test
    void todasAsRotasExigemLogin() throws Exception {
        mvc.perform(get(BASE + "/lancamentos")).andExpect(status().isUnauthorized());
        mvc.perform(get(BASE + "/resumo")).andExpect(status().isUnauthorized());
        mvc.perform(get(BASE + "/pessoas")).andExpect(status().isUnauthorized());
        mvc.perform(get(BASE + "/categorias")).andExpect(status().isUnauthorized());
        mvc.perform(post(BASE + "/lancamentos").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        mvc.perform(put(BASE + "/lancamentos/5").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        mvc.perform(put(BASE + "/lancamentos/5/realizado").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        mvc.perform(delete(BASE + "/lancamentos/5")).andExpect(status().isUnauthorized());
        mvc.perform(post(BASE + "/pessoas").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        mvc.perform(put(BASE + "/pessoas/1").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        mvc.perform(post(BASE + "/categorias").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        mvc.perform(put(BASE + "/categorias/1").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
    }

    @Test
    void respostasNaoPodemFicarEmCache() throws Exception {
        when(service.listar(any())).thenReturn(new FinanceiroListaDto(new ArrayList<>(), null, new ArrayList<>(), new ArrayList<>()));
        when(service.resumir(any())).thenReturn(new FinanceiroResumoDto(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0, 0));

        mvc.perform(get(BASE + "/lancamentos").header("Authorization", auth()))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(get(BASE + "/resumo").header("Authorization", auth()))
                .andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(get(BASE + "/pessoas").header("Authorization", auth()))
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    void listagemRepassaTodosOsFiltrosEDevolveDataBrasileira() throws Exception {
        when(service.listar(any())).thenReturn(new FinanceiroListaDto(List.of(dto()), null, new ArrayList<>(), new ArrayList<>()));

        mvc.perform(get(BASE + "/lancamentos").param("competencia", "2026-10").param("de", "2026-10-01").param("ate", "2026-10-31")
                        .param("idPessoa", "2").param("idCategoria", "3").param("tipo", "SAIDA").param("situacao", "PREVISTO")
                        .param("texto", "luz").header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.lancamentos[0].dtLancamento").value("08/10/2026"))
                .andExpect(jsonPath("$.body.lancamentos[0].competencia").value("2026-10"))
                .andExpect(jsonPath("$.body.lancamentos[0].inRealizado").value(false))
                .andExpect(jsonPath("$.body.lancamentos[0].nrVersao").value(1));

        ArgumentCaptor<FinanceiroFiltro> captor = ArgumentCaptor.forClass(FinanceiroFiltro.class);
        verify(service).listar(captor.capture());
        FinanceiroFiltro f = captor.getValue();
        assertEquals("2026-10", f.getCompetencia());
        assertEquals(LocalDate.of(2026, 10, 1), f.getDe());
        assertEquals(LocalDate.of(2026, 10, 31), f.getAte());
        assertEquals(2L, f.getIdPessoa());
        assertEquals(3L, f.getIdCategoria());
        assertEquals("SAIDA", f.getTipo());
        assertEquals("PREVISTO", f.getSituacao());
        assertEquals("luz", f.getTexto());
    }

    @Test
    void criaLancamentoEValidaOCorpo() throws Exception {
        when(service.criar(any(), any())).thenReturn(List.of(dto()));

        mvc.perform(post(BASE + "/lancamentos").header("Authorization", auth()).contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.body[0].idLancamento").value(5));
        // o autor é o usuário do token
        verify(service).criar(any(), eq(1L));

        String semData = "{\"cdRequisicao\":\"" + UUID1 + "\",\"idCategoria\":1,\"idPessoa\":1,\"vlLancamento\":5}";
        String valorZero = CORPO_VALIDO.replace("1500.00", "0");
        String semCategoria = CORPO_VALIDO.replace("\"idCategoria\":1,", "");
        String semPessoa = CORPO_VALIDO.replace("\"idPessoa\":1,", "");
        String parcelasDemais = CORPO_VALIDO.replace("}", ",\"qtParcelas\":121}");
        for (String ruim : new String[]{semData, valorZero, semCategoria, semPessoa, parcelasDemais}) {
            mvc.perform(post(BASE + "/lancamentos").header("Authorization", auth()).contentType(MediaType.APPLICATION_JSON).content(ruim))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void conflitoDeVersaoVira409ComAMensagem() throws Exception {
        when(service.marcarRealizado(anyLong(), any())).thenThrow(new ValidacaoException(NordHttpEnum.HTTP_409,
                "O lançamento mudou. Sincronize e tente novamente.", null));

        mvc.perform(put(BASE + "/lancamentos/5/realizado").header("Authorization", auth()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inRealizado\":true,\"nrVersao\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.txMensagem").value("O lançamento mudou. Sincronize e tente novamente."));
    }

    @Test
    void marcacaoExigeOsDoisCampos() throws Exception {
        mvc.perform(put(BASE + "/lancamentos/5/realizado").header("Authorization", auth()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inRealizado\":true}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put(BASE + "/lancamentos/5/realizado").header("Authorization", auth()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nrVersao\":1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void exclusaoRepassaANrVersaoDaQueryString() throws Exception {
        mvc.perform(delete(BASE + "/lancamentos/5").param("nrVersao", "3").header("Authorization", auth()))
                .andExpect(status().isNoContent());
        verify(service).excluir(5L, 3);

        doThrow(new ValidacaoException(NordHttpEnum.HTTP_409, "O lançamento mudou. Sincronize e tente novamente.", null))
                .when(service).excluir(eq(6L), any());
        mvc.perform(delete(BASE + "/lancamentos/6").param("nrVersao", "1").header("Authorization", auth()))
                .andExpect(status().isConflict());
    }
}
