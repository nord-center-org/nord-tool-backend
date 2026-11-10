package br.com.nord_tool_backend.controller.read;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.ArquivoResponse;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.BlocoCadernoDto;
import br.com.nord_tool_backend.dto.BlocoPaginaDto;
import br.com.nord_tool_backend.service.BlocoNotasService;
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
@RequestMapping("/api/v1/nord-tool/blocos-notas")
@Tag(name = "Bloco de notas", description = "Cadernos pessoais com páginas desenhadas à mão")
public class BlocoNotasReadController implements BaseResponse {

    private final BlocoNotasService service;

    @Operation(summary = "Meus cadernos (sem páginas)")
    @GetMapping
    public ResponseEntity<ApiResponseBody<List<BlocoCadernoDto>>> cadernos() {
        return ok(service.cadernos());
    }

    @Operation(summary = "Caderno com as páginas (sem o desenho)")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseBody<BlocoCadernoDto>> abrir(@PathVariable Long id) {
        return ok(service.abrir(id));
    }

    @Operation(summary = "Página com o desenho")
    @GetMapping("/paginas/{id}")
    public ResponseEntity<ApiResponseBody<BlocoPaginaDto>> pagina(@PathVariable Long id) {
        return ok(service.pagina(id));
    }

    @Operation(summary = "Imagem de fundo da página (página de PDF importado)")
    @GetMapping("/paginas/{id}/fundo")
    public ResponseEntity<byte[]> fundo(@PathVariable Long id, @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        return ArquivoResponse.imagem(service.baixarFundo(id), ifNoneMatch);
    }

    @Operation(summary = "Imagem colada numa página")
    @GetMapping("/imagens/{id}")
    public ResponseEntity<byte[]> imagem(@PathVariable Long id, @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        return ArquivoResponse.imagem(service.baixarImagem(id), ifNoneMatch);
    }

    @Operation(summary = "Documento de texto do caderno (estilo Word)")
    @GetMapping("/{id}/documento")
    public ResponseEntity<ApiResponseBody<br.com.nord_tool_backend.dto.BlocoDocumentoDto>> documento(@PathVariable Long id) {
        return ok(service.documento(id));
    }

    @Operation(summary = "PDF original de páginas importadas")
    @GetMapping("/pdfs/{id}")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id, @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        return ArquivoResponse.pdf(service.baixarPdf(id), ifNoneMatch);
    }
}
