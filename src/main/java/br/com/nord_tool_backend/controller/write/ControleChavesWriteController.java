package br.com.nord_tool_backend.controller.write;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.RetiradaControleChavesDto;
import br.com.nord_tool_backend.form.NovaRetiradaControleChavesForm;
import br.com.nord_tool_backend.form.RecebimentoControleChavesForm;
import br.com.nord_tool_backend.service.ControleChavesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/controle-chaves", "/api/v1/nord-tool/controle-chaves"})
@Tag(name = "Controle de Chaves", description = "Endpoints para registrar retiradas e recebimentos de chaves")
public class ControleChavesWriteController implements BaseResponse {

    private final ControleChavesService controleChavesService;

    @Operation(summary = "Registrar nova retirada de chave")
    @PostMapping("/retiradas")
    public ResponseEntity<ApiResponseBody<RetiradaControleChavesDto>> criarRetirada(
            @Valid @RequestBody NovaRetiradaControleChavesForm novaRetiradaControleChavesForm) {
        return created(controleChavesService.criarRetirada(novaRetiradaControleChavesForm));
    }

    @Operation(summary = "Registrar recebimento de chave")
    @PatchMapping("/retiradas/{id}/recebimento")
    public ResponseEntity<ApiResponseBody<RetiradaControleChavesDto>> receberRetirada(
            @PathVariable("id") Long idRequisicao,
            @Valid @RequestBody RecebimentoControleChavesForm recebimentoControleChavesForm) {
        return ok(controleChavesService.receberRetirada(idRequisicao, recebimentoControleChavesForm));
    }
}
