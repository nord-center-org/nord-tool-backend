package br.com.nord_tool_backend.controller.write;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.CronogramaEquipeDto;
import br.com.nord_tool_backend.form.CronogramaEquipeForm;
import br.com.nord_tool_backend.service.CronogramaEquipeService;
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
@RequestMapping("/api/v1/nord-tool/cronogramaSemanal/equipes")
@Tag(name = "Cronograma semanal", description = "Equipes do cronograma por obra")
public class CronogramaEquipeWriteController implements BaseResponse {

    private final CronogramaEquipeService service;

    @Operation(summary = "Cria uma equipe na obra escolhida")
    @PostMapping
    public ResponseEntity<ApiResponseBody<CronogramaEquipeDto>> criar(@Valid @RequestBody CronogramaEquipeForm form) {
        return created(service.criar(form));
    }

    @Operation(summary = "Altera nome, líder e membros da equipe")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseBody<CronogramaEquipeDto>> alterar(@PathVariable Long id, @Valid @RequestBody CronogramaEquipeForm form) {
        return ok(service.alterar(id, form));
    }

    @Operation(summary = "Exclui a equipe")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseBody<Void>> excluir(@PathVariable Long id) {
        service.excluir(id);
        return noContent();
    }
}
