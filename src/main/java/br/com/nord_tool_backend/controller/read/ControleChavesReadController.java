package br.com.nord_tool_backend.controller.read;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.ApartamentoControleChavesDto;
import br.com.nord_tool_backend.dto.DashboardControleChavesDto;
import br.com.nord_tool_backend.dto.FerramentaControleChavesDto;
import br.com.nord_tool_backend.dto.ObraControleChavesDto;
import br.com.nord_tool_backend.dto.RetiradaControleChavesDto;
import br.com.nord_tool_backend.service.ControleChavesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/controle-chaves", "/api/v1/nord-tool/controle-chaves"})
@Tag(name = "Controle de Chaves", description = "Endpoints para consultar o controle de chaves")
public class ControleChavesReadController implements BaseResponse {

    private final ControleChavesService controleChavesService;

    @Operation(summary = "Listar obras ativas no controle de chaves")
    @GetMapping("/obras")
    public ResponseEntity<ApiResponseBody<List<ObraControleChavesDto>>> listarObras() {
        return ok(controleChavesService.listarObras());
    }

    @Operation(summary = "Listar apartamentos para seleção no controle de chaves")
    @GetMapping("/apartamentos")
    public ResponseEntity<ApiResponseBody<List<ApartamentoControleChavesDto>>> listarApartamentos(
            @RequestParam(value = "busca", required = false, defaultValue = "") String nmBusca,
            @RequestParam(value = "limite", required = false, defaultValue = "20") int nrQuantidadePorPagina,
            @RequestParam(value = "pagina", required = false, defaultValue = "0") int nrPagina) {
        return ok(controleChavesService.listarApartamentos(nmBusca, nrQuantidadePorPagina, nrPagina));
    }

    @Operation(summary = "Listar ferramentas para seleção no controle de chaves")
    @GetMapping("/ferramentas")
    public ResponseEntity<ApiResponseBody<List<FerramentaControleChavesDto>>> listarFerramentas(
            @RequestParam(value = "busca", required = false, defaultValue = "") String nmBusca,
            @RequestParam(value = "limite", required = false, defaultValue = "20") int nrQuantidadePorPagina,
            @RequestParam(value = "pagina", required = false, defaultValue = "0") int nrPagina) {
        return ok(controleChavesService.listarFerramentas(nmBusca, nrQuantidadePorPagina, nrPagina));
    }

    @Operation(summary = "Buscar indicadores e retiradas recentes do dashboard")
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponseBody<DashboardControleChavesDto>> buscarDashboard(
            @RequestParam(value = "limiteRecentes", required = false, defaultValue = "5") int nrLimiteRecentes,
            @RequestParam(value = "idObra", required = false) String idObra) {
        return ok(controleChavesService.buscarDashboard(nrLimiteRecentes, idObra));
    }

    @Operation(summary = "Listar histórico completo de retiradas e recebimentos de chaves")
    @GetMapping("/historico")
    public ResponseEntity<ApiResponseBody<List<RetiradaControleChavesDto>>> listarHistorico(
            @RequestParam(value = "busca", required = false, defaultValue = "") String nmBusca,
            @RequestParam(value = "status", required = false, defaultValue = "") String nmStatusRequisicao,
            @RequestParam(value = "idObra", required = false) String idObra,
            @RequestParam(value = "limite", required = false, defaultValue = "20") int nrQuantidadePorPagina,
            @RequestParam(value = "pagina", required = false, defaultValue = "0") int nrPagina) {
        return ok(controleChavesService.listarHistorico(nmBusca, nmStatusRequisicao, idObra,
                nrQuantidadePorPagina, nrPagina));
    }
}
