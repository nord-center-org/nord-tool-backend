package br.com.nord_tool_backend.controller.read;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.FinanceiroResponse;
import br.com.nord_tool_backend.dto.FinanceiroInvestimentoDto;
import br.com.nord_tool_backend.service.FinanceiroInvestimentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nord-tool/financeiro/investimentos")
@Tag(name = "Financeiro", description = "Controle financeiro: lançamentos, pessoas e categorias")
public class FinanceiroInvestimentoReadController implements FinanceiroResponse {

    private final FinanceiroInvestimentoService service;

    @Operation(summary = "Fundos imobiliários com posição, cotação ao vivo (cache de 60 s), resultado e proventos a receber. "
            + "Sem cotação do provedor vale a última guardada")
    @GetMapping
    public ResponseEntity<ApiResponseBody<FinanceiroInvestimentoDto>> listar(@RequestParam(required = false) Long idPessoa) {
        return ok(service.listar(idPessoa));
    }
}
