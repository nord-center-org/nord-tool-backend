package br.com.nord_tool_backend.controller.read;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.IntegracaoTokenDto;
import br.com.nord_tool_backend.dto.RelatorioCronogramaDto;
import br.com.nord_tool_backend.service.IntegracaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nord-tool")
@Tag(name = "Integrações", description = "Links de leitura para assistentes de IA (Gemini, ChatGPT…)")
public class IntegracaoReadController implements BaseResponse {

    private final IntegracaoService service;

    @Operation(summary = "Lista os links de integração do usuário logado (sem a chave)")
    @GetMapping("/integracoes/tokens")
    public ResponseEntity<ApiResponseBody<List<IntegracaoTokenDto>>> listar() {
        return ok(service.listarTokens());
    }

    @Operation(summary = "Relatório público do cronograma pela chave do link (texto simples ou JSON)")
    @GetMapping("/publico/cronograma/{chave}")
    public ResponseEntity<?> relatorio(@PathVariable String chave,
                                       @RequestParam(defaultValue = "texto") String formato,
                                       @RequestParam(defaultValue = "7") int dias) {
        ResponseEntity.BodyBuilder resposta = ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("X-Robots-Tag", "noindex, nofollow")
                .header("Referrer-Policy", "no-referrer");
        if ("json".equalsIgnoreCase(formato)) {
            RelatorioCronogramaDto r = service.relatorioCronograma(chave, dias);
            return resposta.contentType(MediaType.APPLICATION_JSON).body(r);
        }
        return resposta.contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .body(service.relatorioCronogramaTexto(chave, dias));
    }
}
