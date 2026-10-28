package br.com.nord_tool_backend.controller.read;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.CronogramaEquipeDto;
import br.com.nord_tool_backend.service.CronogramaEquipeService;
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
@RequestMapping("/api/v1/nord-tool/cronogramaSemanal/equipes")
@Tag(name = "Cronograma semanal", description = "Equipes do cronograma por obra")
public class CronogramaEquipeReadController implements BaseResponse {

    private final CronogramaEquipeService service;

    @Operation(summary = "Equipes que eu enxergo (todas as obras), para filtrar demandas")
    @GetMapping
    public ResponseEntity<ApiResponseBody<List<CronogramaEquipeDto>>> minhas() {
        return ok(service.minhasEquipes());
    }

    @Operation(summary = "Equipes da obra escolhida (admin do cronograma)")
    @GetMapping("/obra")
    public ResponseEntity<ApiResponseBody<List<CronogramaEquipeDto>>> daObra() {
        return ok(service.equipesDaObra());
    }

    @Operation(summary = "Sugestão de membros: o líder e todos abaixo dele na hierarquia")
    @GetMapping("/hierarquia/{idLider}")
    public ResponseEntity<ApiResponseBody<List<Long>>> hierarquia(@PathVariable Long idLider) {
        return ok(service.pelaHierarquia(idLider));
    }
}
