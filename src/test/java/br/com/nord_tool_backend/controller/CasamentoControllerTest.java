package br.com.nord_tool_backend.controller;

import br.com.nord_tool_backend.controller.read.CasamentoReadController;
import br.com.nord_tool_backend.controller.write.CasamentoWriteController;
import br.com.nord_tool_backend.dto.CasamentoAnexoDto;
import br.com.nord_tool_backend.dto.CasamentoConfiguracaoDto;
import br.com.nord_tool_backend.dto.CasamentoDashboardDto;
import br.com.nord_tool_backend.dto.CasamentoFornecedorDto;
import br.com.nord_tool_backend.dto.CasamentoMarcoDto;
import br.com.nord_tool_backend.dto.ImportacaoConvidadosDto;
import br.com.nord_tool_backend.handler.GlobalExceptionHandler;
import br.com.nord_tool_backend.security.JwtAuthenticationFilter;
import br.com.nord_tool_backend.security.JwtService;
import br.com.nord_tool_backend.security.JwtServiceImpl;
import br.com.nord_tool_backend.security.SecurityConfig;
import br.com.nord_tool_backend.security.SecurityProperties;
import br.com.nord_tool_backend.service.CasamentoConvidadoService;
import br.com.nord_tool_backend.service.CasamentoFornecedorService;
import br.com.nord_tool_backend.service.CasamentoMarcoService;
import br.com.nord_tool_backend.service.CasamentoService;
import br.com.nord_tool_backend.storage.ArquivoConteudo;
import br.com.nord_tool_backend.storage.ArquivoDownload;
import org.junit.jupiter.api.Test;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

@WebMvcTest(controllers = {CasamentoReadController.class, CasamentoWriteController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtServiceImpl.class, SecurityProperties.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {"nord-tool.security.enabled=true", "nord-tool.security.jwt-secret=segredo-de-teste-com-mais-de-32-bytes!!"})
class CasamentoControllerTest {

    private static final String BASE = "/api/v1/nord-tool/casamento";

    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @MockBean CasamentoService casamentoService;
    @MockBean CasamentoFornecedorService fornecedorService;
    @MockBean CasamentoConvidadoService convidadoService;
    @MockBean CasamentoMarcoService marcoService;

    private String auth() {
        return "Bearer " + jwt.gerar(1L, "a@b.com", "ADMIN", List.of("*:ESCRITA"), Instant.now());
    }

    @Test
    void todasAsRotasExigemLogin() throws Exception {
        mvc.perform(get(BASE + "/dashboard")).andExpect(status().isUnauthorized());
        mvc.perform(get(BASE + "/fornecedores")).andExpect(status().isUnauthorized());
        mvc.perform(get(BASE + "/convidados")).andExpect(status().isUnauthorized());
        mvc.perform(get(BASE + "/marcos")).andExpect(status().isUnauthorized());
        mvc.perform(put(BASE + "/configuracao").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        mvc.perform(post(BASE + "/marcos/padrao")).andExpect(status().isUnauthorized());
    }

    @Test
    void respostasNaoPodemFicarEmCache() throws Exception {
        when(casamentoService.dashboard()).thenReturn(new CasamentoDashboardDto());
        when(fornecedorService.listar()).thenReturn(new ArrayList<>());

        mvc.perform(get(BASE + "/dashboard").header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(get(BASE + "/fornecedores").header("Authorization", auth()))
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    void dashboardDevolveOsTotais() throws Exception {
        CasamentoDashboardDto d = new CasamentoDashboardDto(new CasamentoConfiguracaoDto("Ana & Beto", "2027-10-12"),
                5, 2, new BigDecimal("16000.50"), 80, 30, 52, 10, 3, 30, new ArrayList<>());
        when(casamentoService.dashboard()).thenReturn(d);

        mvc.perform(get(BASE + "/dashboard").header("Authorization", auth()))
                .andExpect(jsonPath("$.body.configuracao.casal").value("Ana & Beto"))
                .andExpect(jsonPath("$.body.qtPessoasConfirmadas").value(52))
                .andExpect(jsonPath("$.body.vlContratado").value(16000.50))
                .andExpect(jsonPath("$.body.pcMarcosConcluidos").value(30));
    }

    @Test
    void criaFornecedorEValidaCamposObrigatoriosEValorNegativo() throws Exception {
        when(fornecedorService.criar(any())).thenReturn(new CasamentoFornecedorDto(1L, "Buffet", "Alimentação", null, "PESQUISANDO", BigDecimal.ZERO, null, 0));

        mvc.perform(post(BASE + "/fornecedores").header("Authorization", auth()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nmFornecedor\":\"Buffet\",\"nmCategoria\":\"Alimentação\",\"vlValor\":1500.00}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.body.idFornecedor").value(1));

        mvc.perform(post(BASE + "/fornecedores").header("Authorization", auth()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nmCategoria\":\"x\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post(BASE + "/fornecedores").header("Authorization", auth()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nmFornecedor\":\"A\",\"nmCategoria\":\"B\",\"vlValor\":-5}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anexaContratoPorMultipartEBaixaSemCache() throws Exception {
        when(fornecedorService.anexar(eq(3L), anyString(), any(), eq("Contrato")))
                .thenReturn(new CasamentoAnexoDto(9L, 3L, "c.pdf", "application/pdf", 4L, "Contrato", 1L));
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "c.pdf", "application/pdf", new byte[]{'%', 'P', 'D', 'F'});

        mvc.perform(multipart(BASE + "/fornecedores/3/anexos").file(arquivo).param("descricao", "Contrato").header("Authorization", auth()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.body.idAnexo").value(9));

        when(fornecedorService.baixarAnexo(9L)).thenReturn(new ArquivoDownload(new ArquivoConteudo("c.pdf", "application/pdf", 4, new byte[]{'%', 'P', 'D', 'F'}), 1L));
        mvc.perform(get(BASE + "/fornecedores/anexos/9/arquivo").header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Content-Type", "application/pdf"));

        mvc.perform(delete(BASE + "/fornecedores/anexos/9").header("Authorization", auth())).andExpect(status().is2xxSuccessful());
        verify(fornecedorService).excluirAnexo(9L);
    }

    @Test
    void importaConvidadosEDevolveORelatorio() throws Exception {
        ImportacaoConvidadosDto relatorio = new ImportacaoConvidadosDto(2, new ArrayList<>(List.of(new ImportacaoConvidadosDto.LinhaRejeitada(4, "Status inválido: Talvez"))));
        when(convidadoService.importar(eq("c.xlsx"), any())).thenReturn(relatorio);
        MockMultipartFile planilha = new MockMultipartFile("planilha", "c.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1, 2, 3});

        mvc.perform(multipart(BASE + "/convidados/importar").file(planilha).header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.importados").value(2))
                .andExpect(jsonPath("$.body.rejeitados[0].linha").value(4))
                .andExpect(jsonPath("$.body.rejeitados[0].motivo").value("Status inválido: Talvez"));
    }

    @Test
    void convidadoValidaNomeEAcompanhantes() throws Exception {
        mvc.perform(post(BASE + "/convidados").header("Authorization", auth()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post(BASE + "/convidados").header("Authorization", auth()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nmConvidado\":\"Ana\",\"nrAcompanhantes\":-1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void marcosConcluidoPadraoEDataNoFormatoBrasileiro() throws Exception {
        when(marcoService.concluir(4L, true)).thenReturn(new CasamentoMarcoDto(4L, "Contratar buffet", LocalDate.parse("2027-01-12"), true, null));
        when(marcoService.criarPadrao()).thenReturn(new ArrayList<>());

        mvc.perform(put(BASE + "/marcos/4/concluido").header("Authorization", auth()).contentType(MediaType.APPLICATION_JSON).content("{\"concluido\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.dtPrazo").value("12/01/2027"))
                .andExpect(jsonPath("$.body.inConcluido").value(true));
        mvc.perform(put(BASE + "/marcos/4/concluido").header("Authorization", auth()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post(BASE + "/marcos/padrao").header("Authorization", auth())).andExpect(status().isCreated());
    }

    @Test
    void configuracaoExigeDataNoFormatoIso() throws Exception {
        mvc.perform(put(BASE + "/configuracao").header("Authorization", auth()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"casal\":\"Ana & Beto\",\"dataCasamento\":\"12/10/2027\"}"))
                .andExpect(status().isBadRequest());

        when(casamentoService.salvarConfiguracao(any())).thenReturn(new CasamentoConfiguracaoDto("Ana & Beto", "2027-10-12"));
        mvc.perform(put(BASE + "/configuracao").header("Authorization", auth()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"casal\":\"Ana & Beto\",\"dataCasamento\":\"2027-10-12\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"));
    }
}
