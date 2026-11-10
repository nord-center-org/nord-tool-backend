package br.com.nord_tool_backend.controller.write;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.CubicagemDto;
import br.com.nord_tool_backend.form.CubicagemForm;
import br.com.nord_tool_backend.service.CubicagemService;
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
@RequestMapping("/api/v1/nord-tool/cubicagens")
@Tag(name = "Cubicagem", description = "Volume de concreto das peças de um projeto")
public class CubicagemWriteController implements BaseResponse {

    private final CubicagemService service;

    @Operation(summary = "Cria uma cubicagem")
    @PostMapping
    public ResponseEntity<ApiResponseBody<CubicagemDto>> criar(@Valid @RequestBody CubicagemForm form) {
        return created(service.criar(form));
    }

    @Operation(summary = "Salva nome, perda e peças (com a versão que a tela tinha)")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseBody<CubicagemDto>> salvar(@PathVariable Long id, @Valid @RequestBody CubicagemForm form) {
        return ok(service.salvar(id, form));
    }

    @Operation(summary = "Exclui a cubicagem")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseBody<Void>> excluir(@PathVariable Long id) {
        service.excluir(id);
        return noContent();
    }
}
