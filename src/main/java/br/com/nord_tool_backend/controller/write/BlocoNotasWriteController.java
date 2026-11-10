package br.com.nord_tool_backend.controller.write;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.BlocoCadernoDto;
import br.com.nord_tool_backend.dto.BlocoPaginaDto;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.form.BlocoCadernoForm;
import br.com.nord_tool_backend.form.BlocoConteudoForm;
import br.com.nord_tool_backend.form.BlocoOrdemForm;
import br.com.nord_tool_backend.form.BlocoPaginaForm;
import br.com.nord_tool_backend.service.BlocoNotasService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nord-tool/blocos-notas")
@Tag(name = "Bloco de notas", description = "Cadernos pessoais com páginas desenhadas à mão")
public class BlocoNotasWriteController implements BaseResponse {

    private final BlocoNotasService service;

    @Operation(summary = "Cria um caderno com a primeira página")
    @PostMapping
    public ResponseEntity<ApiResponseBody<BlocoCadernoDto>> criar(@Valid @RequestBody BlocoCadernoForm form) {
        return created(service.criar(form));
    }

    @Operation(summary = "Renomeia ou troca a cor do caderno")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseBody<BlocoCadernoDto>> alterar(@PathVariable Long id, @Valid @RequestBody BlocoCadernoForm form) {
        return ok(service.alterar(id, form));
    }

    @Operation(summary = "Exclui o caderno com todas as páginas e imagens")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseBody<Void>> excluir(@PathVariable Long id) {
        service.excluir(id);
        return noContent();
    }

    @Operation(summary = "Página nova (em branco, pautada, quadriculada, pontilhada) ou cópia de outra")
    @PostMapping("/{id}/paginas")
    public ResponseEntity<ApiResponseBody<BlocoPaginaDto>> novaPagina(@PathVariable Long id, @Valid @RequestBody BlocoPaginaForm form) {
        return created(service.novaPagina(id, form));
    }

    @Operation(summary = "Página de PDF importado: a imagem da página (JPEG/PNG até 5 MB) e o tamanho dela")
    @PostMapping(value = "/{id}/paginas/pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponseBody<BlocoPaginaDto>> paginaDePdf(@PathVariable Long id, @RequestParam("arquivo") MultipartFile arquivo,
            @RequestParam("nrLargura") int largura, @RequestParam("nrAltura") int altura,
            @RequestParam(value = "nrPosicao", required = false) Integer posicao,
            @RequestParam(value = "idPdf", required = false) Long idPdf, @RequestParam(value = "nrPaginaPdf", required = false) Integer nrPaginaPdf) {
        return created(service.paginaDePdf(id, posicao, largura, altura, nome(arquivo, "pagina.jpg"), bytes(arquivo), idPdf, nrPaginaPdf));
    }

    @Operation(summary = "Salva o desenho da página (com a versão que a tela tinha)")
    @PutMapping("/paginas/{id}")
    public ResponseEntity<ApiResponseBody<BlocoPaginaDto>> salvar(@PathVariable Long id, @Valid @RequestBody BlocoConteudoForm form) {
        return ok(service.salvar(id, form));
    }

    @Operation(summary = "Muda a ordem das páginas")
    @PutMapping("/{id}/paginas/ordem")
    public ResponseEntity<ApiResponseBody<BlocoCadernoDto>> reordenar(@PathVariable Long id, @Valid @RequestBody BlocoOrdemForm form) {
        return ok(service.reordenar(id, form.getIds()));
    }

    @Operation(summary = "Exclui a página (o caderno fica com pelo menos uma)")
    @DeleteMapping("/paginas/{id}")
    public ResponseEntity<ApiResponseBody<BlocoCadernoDto>> excluirPagina(@PathVariable Long id) {
        return ok(service.excluirPagina(id));
    }

    @Operation(summary = "Guarda uma imagem (JPEG/PNG até 5 MB) para colar numa página; devolve o id")
    @PostMapping(value = "/{id}/imagens", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponseBody<Long>> imagem(@PathVariable Long id, @RequestParam("arquivo") MultipartFile arquivo) {
        return created(service.imagem(id, nome(arquivo, "imagem.jpg"), bytes(arquivo)));
    }

    @Operation(summary = "Salva o documento de texto do caderno (com a versão que a tela tinha)")
    @PutMapping("/{id}/documento")
    public ResponseEntity<ApiResponseBody<br.com.nord_tool_backend.dto.BlocoDocumentoDto>> salvarDocumento(@PathVariable Long id,
            @Valid @RequestBody br.com.nord_tool_backend.form.BlocoDocumentoForm form) {
        return ok(service.salvarDocumento(id, form));
    }

    @Operation(summary = "Guarda o PDF original importado (até 15 MB) para a página manter a qualidade no zoom; devolve o id")
    @PostMapping(value = "/{id}/pdfs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponseBody<Long>> pdf(@PathVariable Long id, @RequestParam("arquivo") MultipartFile arquivo) {
        return created(service.enviarPdf(id, nome(arquivo, "arquivo.pdf"), bytes(arquivo)));
    }

    private static String nome(MultipartFile arquivo, String padrao) {
        return arquivo.getOriginalFilename() == null ? padrao : arquivo.getOriginalFilename();
    }

    private static byte[] bytes(MultipartFile arquivo) {
        try {
            return arquivo.getBytes();
        } catch (IOException ex) {
            throw new EntradaInvalidaException("Não foi possível ler o arquivo enviado", ex);
        }
    }
}
