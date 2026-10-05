package br.com.nord_tool_backend.controller;

import br.com.nord_tool_backend.controller.read.TermoReprovaReadController;
import br.com.nord_tool_backend.controller.write.TermoReprovaWriteController;
import br.com.nord_tool_backend.dto.TermoReprovaDto;
import br.com.nord_tool_backend.dto.TermoReprovaResumoDto;
import br.com.nord_tool_backend.excepetion.ValidacaoException;
import br.com.nord_tool_backend.controller.response.NordHttpEnum;
import br.com.nord_tool_backend.handler.GlobalExceptionHandler;
import br.com.nord_tool_backend.security.JwtAuthenticationFilter;
import br.com.nord_tool_backend.security.JwtService;
import br.com.nord_tool_backend.security.SecurityConfig;
import br.com.nord_tool_backend.security.SecurityProperties;
import br.com.nord_tool_backend.service.TermoReprovaService;
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

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {TermoReprovaReadController.class, TermoReprovaWriteController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class, SecurityProperties.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {"nord-tool.security.enabled=true", "nord-tool.security.jwt-secret=segredo-de-teste-com-mais-de-32-bytes!!"})
class TermoReprovaControllerTest {

    private static final String BASE = "/api/v1/nord-tool/apartamentoVistoria";

    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @MockBean TermoReprovaService service;

    private String auth() {
        return "Bearer " + jwt.gerar(1L, "a@b.com", "ADMIN", List.of("*:ESCRITA"), java.time.Instant.now());
    }

    private TermoReprovaDto dto() {
        TermoReprovaDto d = new TermoReprovaDto();
        d.setIdTermoReprova(7L);
        d.setNrTermo(2);
        d.setNrPaginas(3);
        return d;
    }

    @Test
    void rotasExigemToken() throws Exception {
        mvc.perform(get(BASE + "/10/termos-reprova")).andExpect(status().isUnauthorized());
        mvc.perform(get(BASE + "/termos-reprova/7/arquivo")).andExpect(status().isUnauthorized());
        mvc.perform(delete(BASE + "/termos-reprova/7")).andExpect(status().isUnauthorized());
    }

    @Test
    void listaOsTermosDoApartamento() throws Exception {
        when(service.listarPorApartamento(10L)).thenReturn(List.of(
                new TermoReprovaResumoDto(7L, 2, "t.pdf", "PENDENTE", 3, 0, "01/01/2026 10:00:00", 1L)));

        mvc.perform(get(BASE + "/10/termos-reprova").header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body[0].nrTermo").value(2))
                .andExpect(jsonPath("$.body[0].qtFotos").value(0));
    }

    @Test
    void pdfVemComEtagECacheEResponde304() throws Exception {
        byte[] bytes = {'%', 'P', 'D', 'F'};
        when(service.abrirPdf(7L)).thenReturn(new ArquivoDownload(new ArquivoConteudo("termo 1º.pdf", "application/pdf", 4, bytes), 1234L));

        mvc.perform(get(BASE + "/termos-reprova/7/arquivo").header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("ETag", "\"1234\""))
                .andExpect(header().string("Cache-Control", "private, max-age=300, must-revalidate"))
                .andExpect(content().bytes(bytes));

        mvc.perform(get(BASE + "/termos-reprova/7/arquivo").header("Authorization", auth()).header("If-None-Match", "\"1234\""))
                .andExpect(status().isNotModified());
    }

    @Test
    void imagemTemCacheImutavel() throws Exception {
        byte[] bytes = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
        when(service.abrirImagem(5L)).thenReturn(new ArquivoDownload(new ArquivoConteudo("f.jpg", "image/jpeg", 3, bytes), 99L));
        when(service.abrirMiniatura(5L)).thenReturn(new ArquivoDownload(new ArquivoConteudo("m.jpg", "image/jpeg", 3, bytes), 99L));

        for (String sufixo : new String[]{"imagem", "miniatura"}) {
            mvc.perform(get(BASE + "/termos-reprova/fotos/5/" + sufixo + "?v=99").header("Authorization", auth()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("image/jpeg"))
                    .andExpect(header().string("Cache-Control", "private, max-age=31536000, immutable"));
        }
    }

    @Test
    void criaTermoPorMultipart() throws Exception {
        when(service.criar(eq(10L), anyString(), any(), eq(3))).thenReturn(dto());
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "t.pdf", "application/pdf", new byte[]{'%', 'P', 'D', 'F'});

        mvc.perform(multipart(BASE + "/10/termos-reprova").file(arquivo).param("nrPaginas", "3").header("Authorization", auth()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.body.nrTermo").value(2));
    }

    @Test
    void adicionaFotoPorMultipart() throws Exception {
        MockMultipartFile imagem = new MockMultipartFile("imagem", "f.jpg", "image/jpeg", new byte[]{1, 2});
        MockMultipartFile miniatura = new MockMultipartFile("miniatura", "m.jpg", "image/jpeg", new byte[]{1});

        mvc.perform(multipart(BASE + "/termos-reprova/7/fotos").file(imagem).file(miniatura)
                        .param("nrPagina", "2").param("legenda", "Parede").header("Authorization", auth()))
                .andExpect(status().isCreated());

        verify(service).adicionarFoto(eq(7L), eq("f.jpg"), any(), any(), eq(2), eq("Parede"));
    }

    @Test
    void situacaoExigeCorpoValido() throws Exception {
        mvc.perform(put(BASE + "/termos-reprova/7/situacao").header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        when(service.atualizarSituacao(eq(7L), any())).thenReturn(dto());
        mvc.perform(put(BASE + "/termos-reprova/7/situacao").header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"situacao\":\"EM_ANDAMENTO\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void erroDeNegocioViraRespostaPadronizada() throws Exception {
        when(service.atualizarSituacao(eq(7L), any()))
                .thenThrow(new ValidacaoException(NordHttpEnum.HTTP_400, "Anexe ao menos uma foto antes de concluir o termo.", null));

        mvc.perform(put(BASE + "/termos-reprova/7/situacao").header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"situacao\":\"CONCLUIDO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.txMensagem").value("Anexe ao menos uma foto antes de concluir o termo."));
    }

    @Test
    void ordenaFotosEExcluiTermoEFoto() throws Exception {
        when(service.ordenarFotos(eq(7L), any())).thenReturn(new ArrayList<>());

        mvc.perform(put(BASE + "/termos-reprova/7/fotos/ordem").header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON).content("[{\"idTermoFoto\":5,\"nrOrdem\":0}]"))
                .andExpect(status().isOk());
        mvc.perform(delete(BASE + "/termos-reprova/7").header("Authorization", auth())).andExpect(status().is2xxSuccessful());
        mvc.perform(delete(BASE + "/termos-reprova/fotos/5").header("Authorization", auth())).andExpect(status().is2xxSuccessful());

        verify(service).deletar(7L);
        verify(service).excluirFoto(5L);
    }
}
