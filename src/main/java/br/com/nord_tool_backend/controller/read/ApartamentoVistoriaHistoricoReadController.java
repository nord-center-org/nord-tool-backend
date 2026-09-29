package br.com.nord_tool_backend.controller.read;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;

import br.com.nord_tool_backend.dto.ApartamentoVistoriaHistoricoDto;
import br.com.nord_tool_backend.service.ApartamentoVistoriaHistoricoService;
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
@RequestMapping("/api/v1/nord-tool/apartamentoVistoria/historico")
@Tag(name = "Apartamento Vistoria Historico", description = "Endpoint para retornar historico dos apartamentos")
public class ApartamentoVistoriaHistoricoReadController implements BaseResponse {

    private final ApartamentoVistoriaHistoricoService apartamentoVistoriaHistoricoService;

    @Operation(summary = "Buscar histórico do apartamento")
    @GetMapping("/{idApartamentoVistoria}")
    public ResponseEntity<ApiResponseBody<List<ApartamentoVistoriaHistoricoDto>>> buscarHistoricoApartamentoVistoria(@PathVariable("idApartamentoVistoria") Long idApartamentoVistoria) {
        List<ApartamentoVistoriaHistoricoDto> lsApartamentoVistoriaHistoricoDto = apartamentoVistoriaHistoricoService.buscarHistoricoApartamentoVistoria(idApartamentoVistoria);
        return ok(lsApartamentoVistoriaHistoricoDto);
    }
}
