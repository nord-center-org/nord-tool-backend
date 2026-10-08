package br.com.nord_tool_backend.controller;

import br.com.nord_tool_backend.controller.read.CaixinhaReadController;
import br.com.nord_tool_backend.controller.response.NordHttpEnum;
import br.com.nord_tool_backend.controller.write.CaixinhaWriteController;
import br.com.nord_tool_backend.domain.CaixinhaFiltro;
import br.com.nord_tool_backend.dto.CaixinhaLancamentoDto;
import br.com.nord_tool_backend.dto.CaixinhaListaDto;
import br.com.nord_tool_backend.dto.CaixinhaResumoDto;
import br.com.nord_tool_backend.excepetion.ValidacaoException;
import br.com.nord_tool_backend.handler.GlobalExceptionHandler;
import br.com.nord_tool_backend.security.JwtAuthenticationFilter;
import br.com.nord_tool_backend.security.JwtService;
import br.com.nord_tool_backend.security.JwtServiceImpl;
import br.com.nord_tool_backend.security.SecurityConfig;
import br.com.nord_tool_backend.security.SecurityProperties;
import br.com.nord_tool_backend.service.CaixinhaService;
import br.com.nord_tool_backend.storage.ArquivoConteudo;
import br.com.nord_tool_backend.storage.ArquivoDownload;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {CaixinhaReadController.class, CaixinhaWriteController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtServiceImpl.class, SecurityProperties.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {"nord-tool.security.enabled=true", "nord-tool.security.jwt-secret=segredo-de-teste-com-mais-de-32-bytes!!"})
class CaixinhaControllerTest {

    private static final String BASE = "/api/v1/nord-tool/caixinha";
    private static final String UUID1 = "11111111-1111-1111-1111-111111111111";

    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @MockBean CaixinhaService service;

    private String auth() {
        return "Bearer " + jwt.gerar(1L, "a@b.com", "ADMIN", List.of("*:ESCRITA"), Instant.now());
    }

    private CaixinhaLancamentoDto dto() {
        return new CaixinhaLancamentoDto(5L, LocalDate.of(2026, 10, 1), 1L, "Ana", "Cimento", new BigDecimal("10.50"), false, false, 1, 0);
    }

    @Test
    void todasAsRotasExigemLogin() throws Exception {
        mvc.perform(get(BASE + "/lancamentos")).andExpect(status().isUnauthorized());
        mvc.perform(get(BASE + "/resumo")).andExpect(status().isUnauthorized());
        mvc.perform(get(BASE + "/responsaveis")).andExpect(status().isUnauthorized());
        mvc.perform(get(BASE + "/lancamentos/5/comprovantes")).andExpect(status().isUnauthorized());
        mvc.perform(get(BASE + "/comprovantes/9/arquivo")).andExpect(status().isUnauthorized());
        mvc.perform(post(BASE + "/lancamentos").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        mvc.perform(put(BASE + "/lancamentos/5/marcacao").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        mvc.perform(delete(BASE + "/lancamentos/5")).andExpect(status().isUnauthorized());
        mvc.perform(delete(BASE + "/comprovantes/9")).andExpect(status().isUnauthorized());
    }

    @Test
    void respostasNaoPodemFicarEmCache() throws Exception {
        when(service.listar(any())).thenReturn(new CaixinhaListaDto(new ArrayList<>(), new ArrayList<>()));
        when(service.resumir(any())).thenReturn(new CaixinhaResumoDto(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0, 0, 0));

        mvc.perform(get(BASE + "/lancamentos").header("Authorization", auth()))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(get(BASE + "/resumo").header("Authorization", auth()))
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    void listagemRepassaOsFiltrosEDevolveDataBrasileira() throws Exception {
        when(service.listar(any())).thenReturn(new CaixinhaListaDto(List.of(dto()), new ArrayList<>()));

        mvc.perform(get(BASE + "/lancamentos").param("responsavel", "Ana").param("situacao", "A_PAGAR")
                        .param("de", "2026-10-01").param("ate", "2026-10-31").header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.lancamentos[0].dtLancamento").value("01/10/2026"))
                .andExpect(jsonPath("$.body.lancamentos[0].nrVersao").value(1));

        ArgumentCaptor<CaixinhaFiltro> captor = ArgumentCaptor.forClass(CaixinhaFiltro.class);
        verify(service).listar(captor.capture());
        assertEquals("Ana", captor.getValue().getResponsavel());
        assertEquals("A_PAGAR", captor.getValue().getSituacao());
        assertEquals(LocalDate.of(2026, 10, 1), captor.getValue().getDe());
        assertEquals(LocalDate.of(2026, 10, 31), captor.getValue().getAte());
    }

    @Test
    void criaLancamentoEValidaValorEDataEInsumo() throws Exception {
        when(service.criar(any())).thenReturn(dto());

        mvc.perform(post(BASE + "/lancamentos").header("Authorization", auth()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cdRequisicao\":\"" + UUID1 + "\",\"dtLancamento\":\"2026-10-01\",\"idResponsavel\":1,\"txInsumo\":\"Cimento\",\"vlValor\":10.50}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.body.idLancamento").value(5));

        mvc.perform(post(BASE + "/lancamentos").header("Authorization", auth()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cdRequisicao\":\"" + UUID1 + "\",\"dtLancamento\":\"2026-10-01\",\"idResponsavel\":1,\"txInsumo\":\"Cimento\",\"vlValor\":0}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post(BASE + "/lancamentos").header("Authorization", auth()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cdRequisicao\":\"" + UUID1 + "\",\"dtLancamento\":\"2026-10-01\",\"idResponsavel\":1,\"txInsumo\":\" \",\"vlValor\":5}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post(BASE + "/lancamentos").header("Authorization", auth()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cdRequisicao\":\"" + UUID1 + "\",\"idResponsavel\":1,\"txInsumo\":\"x\",\"vlValor\":5}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void conflitoDeVersaoVira409ComAMensagemDoPlano() throws Exception {
        when(service.marcar(anyLong(), any())).thenThrow(new ValidacaoException(NordHttpEnum.HTTP_409,
                "O lançamento mudou. Sincronize e tente novamente.", null));

        mvc.perform(put(BASE + "/lancamentos/5/marcacao").header("Authorization", auth()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pago\":true,\"nrVersao\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.txMensagem").value("O lançamento mudou. Sincronize e tente novamente."));
    }

    @Test
    void marcacaoExigeNrVersao() throws Exception {
        mvc.perform(put(BASE + "/lancamentos/5/marcacao").header("Authorization", auth()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pago\":true}"))
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

    @Test
    void anexaComprovanteMultipart() throws Exception {
        when(service.anexarComprovante(eq(5L), eq("nota.pdf"), any(), eq(UUID1)))
                .thenReturn(new br.com.nord_tool_backend.dto.CaixinhaComprovanteDto(9L, 5L, "nota.pdf", "application/pdf", 10L, 1L));

        mvc.perform(multipart(BASE + "/lancamentos/5/comprovantes")
                        .file(new MockMultipartFile("arquivo", "nota.pdf", "application/pdf", "%PDF-1\n%%EOF".getBytes()))
                        .param("cdRequisicao", UUID1).header("Authorization", auth()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.body.idComprovante").value(9));
    }

    @Test
    void comprovanteSaiComoPdfSemCache() throws Exception {
        when(service.baixarComprovante(9L)).thenReturn(new ArquivoDownload(
                new ArquivoConteudo("nota.pdf", "application/pdf", 12L, "%PDF-1\n%%EOF".getBytes()), 123L));

        mvc.perform(get(BASE + "/comprovantes/9/arquivo").header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Cache-Control", "no-store"));
    }
}
