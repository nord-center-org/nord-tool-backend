package br.com.nord_tool_backend.controller.read;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.EscadinhaDto;
import br.com.nord_tool_backend.dto.EscadinhaPlantaDto;
import br.com.nord_tool_backend.service.EscadinhaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nord-tool/escadinha")
@Tag(name = "Escadinha", description = "Prédio 3D, apartamentos, lados A/B e ciclos por pavimento")
public class EscadinhaReadController implements BaseResponse {

    private final EscadinhaService service;

    @Operation(summary = "Torres e modelo de ciclos da obra escolhida (cabeçalho X-Obra)")
    @GetMapping
    public ResponseEntity<ApiResponseBody<EscadinhaDto>> carregar() {
        return ok(service.carregar());
    }

    @Operation(summary = "Imagem da prancha da torre (data URL), para o fundo da planta 2D")
    @GetMapping("/torres/{id}/planta")
    public ResponseEntity<ApiResponseBody<EscadinhaPlantaDto>> planta(@PathVariable Long id) {
        return ok(service.planta(id));
    }

    @Operation(summary = "Tipologias de apartamento da torre (sem a imagem)")
    @GetMapping("/torres/{id}/tipologias")
    public ResponseEntity<ApiResponseBody<java.util.List<br.com.nord_tool_backend.dto.EscadinhaTipologiaDto>>> tipologias(@PathVariable Long id) {
        return ok(service.tipologias(id));
    }

    @Operation(summary = "Imagem da planta da tipologia (data URL)")
    @GetMapping("/tipologias/{id}/imagem")
    public ResponseEntity<ApiResponseBody<EscadinhaPlantaDto>> imagemTipologia(@PathVariable Long id) {
        return ok(service.imagemTipologia(id));
    }
}
