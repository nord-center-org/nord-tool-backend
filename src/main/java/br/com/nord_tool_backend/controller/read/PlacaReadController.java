package br.com.nord_tool_backend.controller.read;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.ArquivoResponse;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.PlacaDto;
import br.com.nord_tool_backend.dto.PlacaModeloDto;
import br.com.nord_tool_backend.service.PlacaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nord-tool/placas")
@Tag(name = "Placas", description = "Modelos de placa e placas da obra")
public class PlacaReadController implements BaseResponse {

    private final PlacaService service;

    @Operation(summary = "Placas da obra escolhida")
    @GetMapping
    public ResponseEntity<ApiResponseBody<List<PlacaDto>>> listar() {
        return ok(service.placas());
    }

    @Operation(summary = "Modelos de placa (todas as obras)")
    @GetMapping("/modelos")
    public ResponseEntity<ApiResponseBody<List<PlacaModeloDto>>> modelos() {
        return ok(service.modelos());
    }

    @Operation(summary = "Imagem do modelo: fundo (marca d'água) ou logo")
    @GetMapping("/modelos/{id}/imagens/{tipo}")
    public ResponseEntity<byte[]> imagem(@PathVariable Long id, @PathVariable String tipo,
                                         @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        return ArquivoResponse.imagem(service.baixarImagem(id, tipo), ifNoneMatch);
    }
}
