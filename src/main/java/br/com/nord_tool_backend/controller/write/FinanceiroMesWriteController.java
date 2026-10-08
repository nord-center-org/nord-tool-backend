package br.com.nord_tool_backend.controller.write;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.FinanceiroResponse;
import br.com.nord_tool_backend.dto.FinanceiroConfiguracaoDto;
import br.com.nord_tool_backend.dto.FinanceiroFechamentoDto;
import br.com.nord_tool_backend.dto.FinanceiroGeracaoDto;
import br.com.nord_tool_backend.dto.FinanceiroProjecaoMesDto;
import br.com.nord_tool_backend.dto.FinanceiroRecorrenciaDto;
import br.com.nord_tool_backend.form.FinanceiroConfiguracaoForm;
import br.com.nord_tool_backend.form.FinanceiroRecorrenciaForm;
import br.com.nord_tool_backend.form.FinanceiroSaldoInicialForm;
import br.com.nord_tool_backend.security.UsuarioAutenticado;
import br.com.nord_tool_backend.service.FinanceiroProjecaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nord-tool/financeiro")
@Tag(name = "Financeiro", description = "Controle financeiro: lançamentos, pessoas e categorias")
public class FinanceiroMesWriteController implements FinanceiroResponse {

    private final FinanceiroProjecaoService service;

    @Operation(summary = "Define o saldo inicial do mês (o primeiro mês ou uma correção); vazio volta a usar o saldo do mês anterior")
    @PutMapping("/mes/{competencia}/saldo-inicial")
    public ResponseEntity<ApiResponseBody<FinanceiroProjecaoMesDto>> saldoInicial(
            @PathVariable String competencia, @Valid @RequestBody FinanceiroSaldoInicialForm form) {
        return ok(service.definirSaldoInicial(competencia, form));
    }

    @Operation(summary = "Fecha o mês: grava o saldo final real, trava os lançamentos e gera os fixos do mês seguinte")
    @PostMapping("/mes/{competencia}/fechar")
    public ResponseEntity<ApiResponseBody<FinanceiroFechamentoDto>> fechar(
            @PathVariable String competencia, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ok(service.fechar(competencia, usuario == null ? null : usuario.getId()));
    }

    @Operation(summary = "Reabre o último mês fechado")
    @PostMapping("/mes/{competencia}/reabrir")
    public ResponseEntity<ApiResponseBody<FinanceiroProjecaoMesDto>> reabrir(@PathVariable String competencia) {
        return ok(service.reabrir(competencia));
    }

    @Operation(summary = "Atualiza a configuração (meta de saldo, meses da média, dia da conferência e da fatura)")
    @PutMapping("/configuracao")
    public ResponseEntity<ApiResponseBody<FinanceiroConfiguracaoDto>> configuracao(@Valid @RequestBody FinanceiroConfiguracaoForm form) {
        return ok(service.atualizarConfiguracao(form));
    }

    @Operation(summary = "Cria um lançamento fixo (recorrência)")
    @PostMapping("/recorrencias")
    public ResponseEntity<ApiResponseBody<FinanceiroRecorrenciaDto>> criarRecorrencia(@Valid @RequestBody FinanceiroRecorrenciaForm form) {
        return created(service.criarRecorrencia(form));
    }

    @Operation(summary = "Substitui os dados da recorrência; exige nrVersao (divergente → 409)")
    @PutMapping("/recorrencias/{id}")
    public ResponseEntity<ApiResponseBody<FinanceiroRecorrenciaDto>> atualizarRecorrencia(
            @PathVariable Long id, @Valid @RequestBody FinanceiroRecorrenciaForm form) {
        return ok(service.atualizarRecorrencia(id, form));
    }

    @Operation(summary = "Cria os lançamentos das recorrências vigentes no mês que ainda não existem (não duplica)")
    @PostMapping("/recorrencias/gerar/{competencia}")
    public ResponseEntity<ApiResponseBody<FinanceiroGeracaoDto>> gerar(
            @PathVariable String competencia, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ok(service.gerarRecorrencias(competencia, usuario == null ? null : usuario.getId()));
    }
}
