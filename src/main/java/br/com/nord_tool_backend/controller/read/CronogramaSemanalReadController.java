package br.com.nord_tool_backend.controller.read;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.ApartamentoVistoriaDto;
import br.com.nord_tool_backend.dto.CronogramaSemanalDto;
import br.com.nord_tool_backend.dto.CronogramaSemanalExcecaoDto;
import br.com.nord_tool_backend.service.CronogramaSemanalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nord-tool/cronogramaSemanal")
@Tag(name = "Cronograma semanal", description = "Endpoints para buscar e listar os cronogramas semanais retornados")
public class CronogramaSemanalReadController implements BaseResponse {

    private final CronogramaSemanalService cronogramaSemanalService;

    @Operation(summary = "Buscar um cronograma semanal")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseBody<CronogramaSemanalDto>> buscarCronogramaSemanal(@PathVariable("id") Long id) {
        CronogramaSemanalDto cronogramaSemanalDto = cronogramaSemanalService.buscarPorIdCronogramaSemanal(id);
        return ok(cronogramaSemanalDto);
    }

    @Operation(summary = "Listar todos os cronograma semanais")
    @GetMapping
    public ResponseEntity<ApiResponseBody<List<CronogramaSemanalDto>>> listarApartamentoVistoria() {
        List<CronogramaSemanalDto> cronogramaSemanalDto = cronogramaSemanalService.listarCronogramaSemanal();
        return ok(cronogramaSemanalDto);
    }

    @Operation(summary = "Listar os dias desmarcados dos compromissos fixos")
    @GetMapping("/excecoes")
    public ResponseEntity<ApiResponseBody<List<CronogramaSemanalExcecaoDto>>> listarExcecoes() {
        return ok(cronogramaSemanalService.listarExcecoes());
    }
}
