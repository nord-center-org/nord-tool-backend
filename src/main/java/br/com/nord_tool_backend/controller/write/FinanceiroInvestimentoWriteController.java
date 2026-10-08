package br.com.nord_tool_backend.controller.write;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.FinanceiroResponse;
import br.com.nord_tool_backend.dto.FinanceiroAtivoDto;
import br.com.nord_tool_backend.form.FinanceiroAtivoForm;
import br.com.nord_tool_backend.form.FinanceiroOperacaoForm;
import br.com.nord_tool_backend.form.FinanceiroProventoForm;
import br.com.nord_tool_backend.service.FinanceiroInvestimentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Collections;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nord-tool/financeiro/investimentos")
@Tag(name = "Financeiro", description = "Controle financeiro: lançamentos, pessoas e categorias")
public class FinanceiroInvestimentoWriteController implements FinanceiroResponse {

    private final FinanceiroInvestimentoService service;

    @Operation(summary = "Adiciona um fundo à carteira de uma pessoa (ticker e pessoa obrigatórios)")
    @PostMapping("/ativos")
    public ResponseEntity<ApiResponseBody<FinanceiroAtivoDto>> criar(@Valid @RequestBody FinanceiroAtivoForm form) {
        return created(service.criarAtivo(form));
    }

    @Operation(summary = "Renomeia ou arquiva um fundo; exige nrVersao (divergente → 409)")
    @PutMapping("/ativos/{id}")
    public ResponseEntity<ApiResponseBody<FinanceiroAtivoDto>> atualizar(@PathVariable Long id, @Valid @RequestBody FinanceiroAtivoForm form) {
        return ok(service.atualizarAtivo(id, form));
    }

    @Operation(summary = "Registra uma compra ou venda de cotas. cdRequisicao UUID obrigatório; repetido não duplica")
    @PostMapping("/ativos/{id}/operacoes")
    public ResponseEntity<ApiResponseBody<FinanceiroAtivoDto>> operacao(@PathVariable Long id, @Valid @RequestBody FinanceiroOperacaoForm form) {
        return created(service.registrarOperacao(id, form));
    }

    @Operation(summary = "Exclui uma operação (recusa se deixar uma venda sem cotas)")
    @DeleteMapping("/operacoes/{id}")
    public ResponseEntity<ApiResponseBody<FinanceiroAtivoDto>> excluirOperacao(@PathVariable Long id) {
        return ok(service.excluirOperacao(id));
    }

    @Operation(summary = "Registra (ou corrige, pela data-com) um provento por cota")
    @PostMapping("/ativos/{id}/proventos")
    public ResponseEntity<ApiResponseBody<FinanceiroAtivoDto>> provento(@PathVariable Long id, @Valid @RequestBody FinanceiroProventoForm form) {
        return created(service.registrarProvento(id, form));
    }

    @Operation(summary = "Exclui um provento")
    @DeleteMapping("/proventos/{id}")
    public ResponseEntity<ApiResponseBody<FinanceiroAtivoDto>> excluirProvento(@PathVariable Long id) {
        return ok(service.excluirProvento(id));
    }

    @Operation(summary = "Importa os proventos do provedor de cotações (não sobrescreve os digitados)")
    @PostMapping("/ativos/{id}/proventos/sincronizar")
    public ResponseEntity<ApiResponseBody<Map<String, Integer>>> sincronizar(@PathVariable Long id) {
        return ok(Collections.singletonMap("importados", service.sincronizarProventos(id)));
    }
}
