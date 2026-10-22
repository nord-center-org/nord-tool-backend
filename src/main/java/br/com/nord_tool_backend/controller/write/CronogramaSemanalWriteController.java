package br.com.nord_tool_backend.controller.write;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.CronogramaSemanalDto;
import br.com.nord_tool_backend.form.CronogramaSemanalForm;
import br.com.nord_tool_backend.service.CronogramaSemanalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestBody;

import javax.validation.Valid;
import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nord-tool/cronogramaSemanal")
@Tag(name = "Cronograma semanal", description = "Endpoints para criar, manipular e deletar os cronogramas semanais retornados")
public class CronogramaSemanalWriteController implements BaseResponse {

    private final CronogramaSemanalService cronogramaSemanalService;

    @Operation(summary = "Criar um cronograma semanal")
    @PostMapping
    public ResponseEntity<ApiResponseBody<CronogramaSemanalDto>> criarCronogramaSemanal(@Valid @RequestBody CronogramaSemanalForm cronogramaSemanalForm) {
        CronogramaSemanalDto cronogramaSemanalDto = cronogramaSemanalService.salvarCronogramaSemanal(cronogramaSemanalForm);
        return created(cronogramaSemanalDto);
    }

    @Operation(summary = "Editar um cronograma semanal")
    @PutMapping
    public ResponseEntity<ApiResponseBody<CronogramaSemanalDto>> alterarCronogramaSemanal(@Valid @RequestBody CronogramaSemanalForm cronogramaSemanalForm) {
        CronogramaSemanalDto cronogramaSemanalDto = cronogramaSemanalService.alterarCronogramaSemanal(cronogramaSemanalForm);
        return ok(cronogramaSemanalDto);
    }

    @Operation(summary = "Excluir um cronograma Semanal")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseBody<Void>> deletarCronogramaSemanal(@PathVariable("id") Long id) {
        this.cronogramaSemanalService.deletarCronogramaSemanal(id);
        return noContent();
    }

    @Operation(summary = "Desmarcar um compromisso fixo em um dia específico")
    @PutMapping("/{id}/excecoes/{data}")
    public ResponseEntity<ApiResponseBody<Void>> incluirExcecao(@PathVariable("id") Long id,
            @PathVariable("data") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        cronogramaSemanalService.incluirExcecao(id, data);
        return noContent();
    }

    @Operation(summary = "Reativar um compromisso fixo em um dia específico")
    @DeleteMapping("/{id}/excecoes/{data}")
    public ResponseEntity<ApiResponseBody<Void>> removerExcecao(@PathVariable("id") Long id,
            @PathVariable("data") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        cronogramaSemanalService.removerExcecao(id, data);
        return noContent();
    }

    @PutMapping("/{id}/compartilhamento")
    public ResponseEntity<ApiResponseBody<CronogramaSemanalDto>> compartilhar(@PathVariable("id") Long id,
            @Valid @RequestBody br.com.nord_tool_backend.form.CompartilhamentoForm form) {
        return ok(cronogramaSemanalService.compartilhar(id, form.getIdsUsuarios()));
    }
}
