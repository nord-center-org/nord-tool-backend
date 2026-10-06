package br.com.nord_tool_backend.controller.read;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.ArquivoResponse;
import br.com.nord_tool_backend.controller.response.CaixinhaResponse;
import br.com.nord_tool_backend.domain.CaixinhaFiltro;
import br.com.nord_tool_backend.dto.CaixinhaComprovanteDto;
import br.com.nord_tool_backend.dto.CaixinhaListaDto;
import br.com.nord_tool_backend.dto.CaixinhaResponsavelDto;
import br.com.nord_tool_backend.dto.CaixinhaResumoDto;
import br.com.nord_tool_backend.service.CaixinhaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nord-tool/caixinha")
@Tag(name = "Caixinha", description = "Controle de despesas, responsáveis e comprovantes em PDF")
public class CaixinhaReadController implements CaixinhaResponse {

    private final CaixinhaService service;

    @Operation(summary = "Lista os lançamentos (com qtComprovantes) e os responsáveis; filtros opcionais")
    @GetMapping("/lancamentos")
    public ResponseEntity<ApiResponseBody<CaixinhaListaDto>> lancamentos(
            @RequestParam(required = false) String responsavel,
            @RequestParam(required = false) String situacao,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return ok(service.listar(new CaixinhaFiltro(responsavel, situacao, de, ate)));
    }

    @Operation(summary = "Resumo (total, pago, a pagar e quantidades) com os mesmos filtros da listagem")
    @GetMapping("/resumo")
    public ResponseEntity<ApiResponseBody<CaixinhaResumoDto>> resumo(
            @RequestParam(required = false) String responsavel,
            @RequestParam(required = false) String situacao,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return ok(service.resumir(new CaixinhaFiltro(responsavel, situacao, de, ate)));
    }

    @Operation(summary = "Lista os responsáveis")
    @GetMapping("/responsaveis")
    public ResponseEntity<ApiResponseBody<List<CaixinhaResponsavelDto>>> responsaveis() {
        return ok(service.listarResponsaveis());
    }

    @Operation(summary = "Metadados dos comprovantes de um lançamento")
    @GetMapping("/lancamentos/{id}/comprovantes")
    public ResponseEntity<ApiResponseBody<List<CaixinhaComprovanteDto>>> comprovantes(@PathVariable Long id) {
        return ok(service.listarComprovantes(id));
    }

    @Operation(summary = "PDF do comprovante (stream application/pdf, sem cache)")
    @GetMapping("/comprovantes/{idComprovante}/arquivo")
    public ResponseEntity<byte[]> arquivo(@PathVariable Long idComprovante) {
        return ArquivoResponse.semCache(service.baixarComprovante(idComprovante));
    }
}
