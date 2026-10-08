package br.com.nord_tool_backend.controller.read;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.FinanceiroResponse;
import br.com.nord_tool_backend.domain.FinanceiroFiltro;
import br.com.nord_tool_backend.dto.FinanceiroCategoriaDto;
import br.com.nord_tool_backend.dto.FinanceiroListaDto;
import br.com.nord_tool_backend.dto.FinanceiroPessoaDto;
import br.com.nord_tool_backend.dto.FinanceiroResumoDto;
import br.com.nord_tool_backend.service.FinanceiroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nord-tool/financeiro")
@Tag(name = "Financeiro", description = "Controle financeiro: lançamentos, pessoas e categorias")
public class FinanceiroReadController implements FinanceiroResponse {

    private final FinanceiroService service;

    @Operation(summary = "Extrato: lançamentos filtrados, totais, pessoas e categorias. "
            + "Filtros opcionais: competencia (yyyy-MM), de/ate (data do lançamento), idPessoa, idCategoria, "
            + "tipo (ENTRADA|SAIDA), situacao (TODOS|REALIZADO|PREVISTO) e texto (descrição, categoria ou pessoa)")
    @GetMapping("/lancamentos")
    public ResponseEntity<ApiResponseBody<FinanceiroListaDto>> lancamentos(
            @RequestParam(required = false) String competencia,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(required = false) Long idPessoa,
            @RequestParam(required = false) Long idCategoria,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String situacao,
            @RequestParam(required = false) String texto) {
        return ok(service.listar(new FinanceiroFiltro(competencia, de, ate, idPessoa, idCategoria, tipo, situacao, texto)));
    }

    @Operation(summary = "Totais (entradas, saídas e saldo) com os mesmos filtros do extrato")
    @GetMapping("/resumo")
    public ResponseEntity<ApiResponseBody<FinanceiroResumoDto>> resumo(
            @RequestParam(required = false) String competencia,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(required = false) Long idPessoa,
            @RequestParam(required = false) Long idCategoria,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String situacao,
            @RequestParam(required = false) String texto) {
        return ok(service.resumir(new FinanceiroFiltro(competencia, de, ate, idPessoa, idCategoria, tipo, situacao, texto)));
    }

    @Operation(summary = "Lista as pessoas (de quem é cada lançamento)")
    @GetMapping("/pessoas")
    public ResponseEntity<ApiResponseBody<List<FinanceiroPessoaDto>>> pessoas() {
        return ok(service.listarPessoas());
    }

    @Operation(summary = "Lista as categorias de entrada e saída")
    @GetMapping("/categorias")
    public ResponseEntity<ApiResponseBody<List<FinanceiroCategoriaDto>>> categorias() {
        return ok(service.listarCategorias());
    }
}
