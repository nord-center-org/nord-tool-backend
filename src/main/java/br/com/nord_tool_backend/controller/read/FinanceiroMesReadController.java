package br.com.nord_tool_backend.controller.read;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.FinanceiroResponse;
import br.com.nord_tool_backend.dto.FinanceiroConfiguracaoDto;
import br.com.nord_tool_backend.dto.FinanceiroFaturaLeituraDto;
import br.com.nord_tool_backend.dto.FinanceiroProjecaoMesDto;
import br.com.nord_tool_backend.dto.FinanceiroRecorrenciaDto;
import br.com.nord_tool_backend.service.FinanceiroProjecaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nord-tool/financeiro")
@Tag(name = "Financeiro", description = "Controle financeiro: lançamentos, pessoas e categorias")
public class FinanceiroMesReadController implements FinanceiroResponse {

    private final FinanceiroProjecaoService service;

    @Operation(summary = "A conta do mês (yyyy-MM): entradas, saídas, saldo anterior e saldo final, reais ou projetados. "
            + "Com idPessoa não há saldo anterior")
    @GetMapping("/mes/{competencia}")
    public ResponseEntity<ApiResponseBody<FinanceiroProjecaoMesDto>> mes(
            @PathVariable String competencia, @RequestParam(required = false) Long idPessoa) {
        return ok(service.obterMes(competencia, idPessoa));
    }

    @Operation(summary = "Configuração: meta de saldo, meses da média, dia da conferência e dia de fechamento da fatura")
    @GetMapping("/configuracao")
    public ResponseEntity<ApiResponseBody<FinanceiroConfiguracaoDto>> configuracao() {
        return ok(service.obterConfiguracao());
    }

    @Operation(summary = "Evolução do valor parcial de uma fatura (uma leitura por dia)")
    @GetMapping("/lancamentos/{id}/leituras")
    public ResponseEntity<ApiResponseBody<List<FinanceiroFaturaLeituraDto>>> leituras(@PathVariable Long id) {
        return ok(service.listarLeituras(id));
    }

    @Operation(summary = "Lista os lançamentos fixos (apartamento, evolução de obra, investimentos...)")
    @GetMapping("/recorrencias")
    public ResponseEntity<ApiResponseBody<List<FinanceiroRecorrenciaDto>>> recorrencias() {
        return ok(service.listarRecorrencias());
    }
}
