package br.com.nord_tool_backend.controller;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.ApartamentoControleChavesDto;
import br.com.nord_tool_backend.dto.DashboardControleChavesDto;
import br.com.nord_tool_backend.dto.ObraControleChavesDto;
import br.com.nord_tool_backend.dto.RetiradaControleChavesDto;
import br.com.nord_tool_backend.form.NovaRetiradaControleChavesForm;
import br.com.nord_tool_backend.form.RecebimentoControleChavesForm;
import br.com.nord_tool_backend.service.ControleChavesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/controle-chaves", "/api/v1/nord-tool/controle-chaves"})
@Tag(name = "Controle de Chaves", description = "Endpoints para gerenciamento e controle de chaves")
public class ControleChavesController implements BaseResponse {

    private final ControleChavesService controleChavesService;

    @Operation(summary = "Listar obras ativas no controle de chaves")
    @GetMapping("/obras")
    public ResponseEntity<ApiResponseBody<List<ObraControleChavesDto>>> listarObras() {
        return ok(controleChavesService.listarObras());
    }

    @Operation(summary = "Listar apartamentos para seleção no controle de chaves")
    @GetMapping("/apartamentos")
    public ResponseEntity<ApiResponseBody<List<ApartamentoControleChavesDto>>> listarApartamentos(
            @RequestParam(value = "busca", required = false, defaultValue = "") String busca,
            @RequestParam(value = "limite", required = false, defaultValue = "20") int limite,
            @RequestParam(value = "pagina", required = false, defaultValue = "0") int pagina) {
        return ok(controleChavesService.listarApartamentos(busca, limite, pagina));
    }

    @Operation(summary = "Buscar indicadores e retiradas recentes do dashboard")
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponseBody<DashboardControleChavesDto>> buscarDashboard(
            @RequestParam(value = "limiteRecentes", required = false, defaultValue = "5") int limiteRecentes,
            @RequestParam(value = "idObra", required = false) String idObra) {
        return ok(controleChavesService.buscarDashboard(limiteRecentes, idObra));
    }

    @Operation(summary = "Listar histórico completo de retiradas e devoluções de chaves")
    @GetMapping("/historico")
    public ResponseEntity<ApiResponseBody<List<RetiradaControleChavesDto>>> listarHistorico(
            @RequestParam(value = "busca", required = false, defaultValue = "") String busca,
            @RequestParam(value = "status", required = false, defaultValue = "") String status,
            @RequestParam(value = "idObra", required = false) String idObra,
            @RequestParam(value = "limite", required = false, defaultValue = "20") int limite,
            @RequestParam(value = "pagina", required = false, defaultValue = "0") int pagina) {
        return ok(controleChavesService.listarHistorico(busca, status, idObra, limite, pagina));
    }

    @Operation(summary = "Registrar nova retirada de chave")
    @PostMapping("/retiradas")
    public ResponseEntity<ApiResponseBody<RetiradaControleChavesDto>> criarRetirada(
            @RequestBody NovaRetiradaControleChavesForm form) {
        return ok(controleChavesService.criarRetirada(form));
    }

    @Operation(summary = "Registrar devolução/recebimento de chave")
    @PatchMapping("/retiradas/{id}/recebimento")
    public ResponseEntity<ApiResponseBody<RetiradaControleChavesDto>> receberRetirada(
            @PathVariable("id") Long id,
            @RequestBody RecebimentoControleChavesForm form) {
        return ok(controleChavesService.receberRetirada(id, form));
    }
}
