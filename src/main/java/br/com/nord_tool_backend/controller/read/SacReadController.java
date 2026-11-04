package br.com.nord_tool_backend.controller.read;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.ArquivoResponse;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.SacChamadoDto;
import br.com.nord_tool_backend.dto.SacNovidadesDto;
import br.com.nord_tool_backend.service.SacService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nord-tool/sac")
@Tag(name = "SAC", description = "Chamados das pessoas para os administradores")
public class SacReadController implements BaseResponse {

    private final SacService service;

    @Operation(summary = "Meus chamados; com todos=true, os de todo mundo (ADMIN do SAC)")
    @GetMapping("/chamados")
    public ResponseEntity<ApiResponseBody<List<SacChamadoDto>>> listar(@RequestParam(value = "todos", defaultValue = "false") boolean todos) {
        return ok(service.listar(todos));
    }

    @Operation(summary = "Novidades da pessoa (respostas não vistas, com o avatar de quem respondeu) e, para o ADMIN do SAC, os abertos")
    @GetMapping("/novidades")
    public ResponseEntity<ApiResponseBody<SacNovidadesDto>> novidades() {
        return ok(service.novidades());
    }

    @Operation(summary = "Imagem (print) do chamado: de quem abriu ou para o ADMIN do SAC")
    @GetMapping("/imagens/{id}")
    public ResponseEntity<byte[]> imagem(@PathVariable Long id,
                                         @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        return ArquivoResponse.imagem(service.baixarImagem(id), ifNoneMatch);
    }
}
