package br.com.nord_tool_backend.controller.write;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.FerramentaDto;
import br.com.nord_tool_backend.form.FerramentaForm;
import br.com.nord_tool_backend.service.FerramentaService;
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
@RequestMapping("/api/v1/nord-tool/ferramentas")
@Tag(name = "Ferramentas", description = "Endpoints para criar, alterar e deletar ferramentas")
public class FerramentaWriteController implements BaseResponse {
    private final FerramentaService ferramentaService;

    @Operation(summary = "Criar ferramenta")
    @PostMapping
    public ResponseEntity<ApiResponseBody<FerramentaDto>> criarFerramenta(
            @Valid @RequestBody FerramentaForm ferramentaForm) {
        return created(ferramentaService.salvarFerramenta(ferramentaForm));
    }

    @Operation(summary = "Alterar ferramenta")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseBody<FerramentaDto>> alterarFerramenta(
            @PathVariable Long id, @Valid @RequestBody FerramentaForm ferramentaForm) {
        return ok(ferramentaService.alterarFerramenta(id, ferramentaForm));
    }

    @Operation(summary = "Excluir ferramenta")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseBody<Void>> deletarFerramenta(@PathVariable Long id) {
        ferramentaService.deletarFerramenta(id);
        return noContent();
    }
}
