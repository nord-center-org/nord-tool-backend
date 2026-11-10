package br.com.nord_tool_backend.controller.read;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.BlocoArquivoDto;
import br.com.nord_tool_backend.service.BlocoArquivoService;
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
@RequestMapping("/api/v1/nord-tool/blocos-notas/arquivos")
@Tag(name = "Bloco de notas", description = "Planilhas e documentos avulsos")
public class BlocoArquivoReadController implements BaseResponse {

    private final BlocoArquivoService service;

    @Operation(summary = "Minhas planilhas e documentos (sem o conteúdo)")
    @GetMapping
    public ResponseEntity<ApiResponseBody<List<BlocoArquivoDto>>> listar() {
        return ok(service.listar());
    }

    @Operation(summary = "Planilha ou documento com o conteúdo")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseBody<BlocoArquivoDto>> abrir(@PathVariable Long id) {
        return ok(service.abrir(id));
    }
}
