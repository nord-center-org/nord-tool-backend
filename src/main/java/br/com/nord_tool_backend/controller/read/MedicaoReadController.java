package br.com.nord_tool_backend.controller.read;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.MedicaoListaDto;
import br.com.nord_tool_backend.service.MedicaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nord-tool/medicoes")
@Tag(name = "Medições", description = "Controle de medições de contratos")
public class MedicaoReadController implements BaseResponse {

    private final MedicaoService service;

    @Operation(summary = "Lista as medições (mais recentes primeiro) e os status disponíveis")
    @GetMapping
    public ResponseEntity<ApiResponseBody<MedicaoListaDto>> listar() {
        return ok(service.listar());
    }
}
