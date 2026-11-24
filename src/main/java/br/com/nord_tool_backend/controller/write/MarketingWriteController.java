package br.com.nord_tool_backend.controller.write;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.MarketingContaDto;
import br.com.nord_tool_backend.dto.MarketingMidiaDto;
import br.com.nord_tool_backend.dto.MarketingPublicacaoDto;
import br.com.nord_tool_backend.form.MarketingConectarForm;
import br.com.nord_tool_backend.form.MarketingMidiaForm;
import br.com.nord_tool_backend.form.MarketingPublicarForm;
import br.com.nord_tool_backend.service.MarketingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nord-tool/marketing")
@Tag(name = "Marketing", description = "Contas de redes sociais, publicações e painel de views")
public class MarketingWriteController implements BaseResponse {

    private final MarketingService service;

    @Operation(summary = "Conecta a conta com o código da volta do login da rede")
    @PostMapping("/contas")
    public ResponseEntity<ApiResponseBody<MarketingContaDto>> conectar(@Valid @RequestBody MarketingConectarForm form,
                                                                       @RequestHeader(value = "Origin", required = false) String origem) {
        return created(service.conectar(form, origem));
    }

    @Operation(summary = "Desconecta a conta (apaga os tokens dela do NordTool)")
    @DeleteMapping("/contas/{id}")
    public ResponseEntity<ApiResponseBody<Void>> desconectar(@PathVariable Long id) {
        service.desconectar(id);
        return noContent();
    }

    @Operation(summary = "Começa a receber um vídeo em partes")
    @PostMapping("/midias")
    public ResponseEntity<ApiResponseBody<MarketingMidiaDto>> iniciarMidia(@Valid @RequestBody MarketingMidiaForm form) {
        return created(service.iniciarMidia(form));
    }

    @Operation(summary = "Recebe uma parte do vídeo (em ordem, até 8 MB cada)")
    @PutMapping(value = "/midias/{id}/partes/{indice}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<ApiResponseBody<Void>> parte(@PathVariable String id, @PathVariable int indice, @RequestBody byte[] dados) {
        service.receberParte(id, indice, dados);
        return noContent();
    }

    @Operation(summary = "Publica o vídeo nas contas escolhidas (em segundo plano)")
    @PostMapping("/publicacoes")
    public ResponseEntity<ApiResponseBody<List<MarketingPublicacaoDto>>> publicar(@Valid @RequestBody MarketingPublicarForm form) {
        String base = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        return created(service.publicar(form, base));
    }
}
