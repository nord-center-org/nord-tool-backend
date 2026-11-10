package br.com.nord_tool_backend.controller.write;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.BlocoArquivoDto;
import br.com.nord_tool_backend.form.BlocoArquivoForm;
import br.com.nord_tool_backend.service.BlocoArquivoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nord-tool/blocos-notas/arquivos")
@Tag(name = "Bloco de notas", description = "Planilhas e documentos avulsos")
public class BlocoArquivoWriteController implements BaseResponse {

    private final BlocoArquivoService service;

    @Operation(summary = "Cria uma planilha ou um documento")
    @PostMapping
    public ResponseEntity<ApiResponseBody<BlocoArquivoDto>> criar(@Valid @RequestBody BlocoArquivoForm form) {
        return created(service.criar(form));
    }

    @Operation(summary = "Salva nome, cor e/ou conteúdo (com a versão que a tela tinha)")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseBody<BlocoArquivoDto>> salvar(@PathVariable Long id, @Valid @RequestBody BlocoArquivoForm form) {
        return ok(service.salvar(id, form));
    }

    @Operation(summary = "Exclui a planilha ou o documento")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseBody<Void>> excluir(@PathVariable Long id) {
        service.excluir(id);
        return noContent();
    }
}
