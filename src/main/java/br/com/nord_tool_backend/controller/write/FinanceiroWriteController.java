package br.com.nord_tool_backend.controller.write;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.FinanceiroResponse;
import br.com.nord_tool_backend.dto.FinanceiroCategoriaDto;
import br.com.nord_tool_backend.dto.FinanceiroLancamentoDto;
import br.com.nord_tool_backend.dto.FinanceiroPessoaDto;
import br.com.nord_tool_backend.form.FinanceiroCategoriaForm;
import br.com.nord_tool_backend.form.FinanceiroLancamentoForm;
import br.com.nord_tool_backend.form.FinanceiroPessoaForm;
import br.com.nord_tool_backend.form.FinanceiroRealizadoForm;
import br.com.nord_tool_backend.security.UsuarioAutenticado;
import br.com.nord_tool_backend.service.FinanceiroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nord-tool/financeiro")
@Tag(name = "Financeiro", description = "Controle financeiro: lançamentos, pessoas e categorias")
public class FinanceiroWriteController implements FinanceiroResponse {

    private final FinanceiroService service;

    @Operation(summary = "Cria um lançamento (ou N parcelas mensais com qtParcelas). cdRequisicao UUID obrigatório; "
            + "repetido devolve o já criado. O autor é o usuário logado")
    @PostMapping("/lancamentos")
    public ResponseEntity<ApiResponseBody<List<FinanceiroLancamentoDto>>> criar(
            @Valid @RequestBody FinanceiroLancamentoForm form, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return created(service.criar(form, usuario == null ? null : usuario.getId()));
    }

    @Operation(summary = "Edita um lançamento; exige nrVersao (divergente → 409)")
    @PutMapping("/lancamentos/{id}")
    public ResponseEntity<ApiResponseBody<FinanceiroLancamentoDto>> alterar(
            @PathVariable Long id, @Valid @RequestBody FinanceiroLancamentoForm form) {
        return ok(service.alterar(id, form));
    }

    @Operation(summary = "Marca como recebido/pago (ou volta para previsto); exige nrVersao (divergente → 409)")
    @PutMapping("/lancamentos/{id}/realizado")
    public ResponseEntity<ApiResponseBody<FinanceiroLancamentoDto>> realizado(
            @PathVariable Long id, @Valid @RequestBody FinanceiroRealizadoForm form) {
        return ok(service.marcarRealizado(id, form));
    }

    @Operation(summary = "Exclui o lançamento (nrVersao divergente → 409)")
    @DeleteMapping("/lancamentos/{id}")
    public ResponseEntity<ApiResponseBody<Void>> excluir(
            @PathVariable Long id, @RequestParam(value = "nrVersao", required = false) Integer nrVersao) {
        service.excluir(id, nrVersao);
        return noContent();
    }

    @Operation(summary = "Cria uma pessoa")
    @PostMapping("/pessoas")
    public ResponseEntity<ApiResponseBody<FinanceiroPessoaDto>> criarPessoa(@Valid @RequestBody FinanceiroPessoaForm form) {
        return created(service.criarPessoa(form));
    }

    @Operation(summary = "Atualiza nome, ordem, vínculo com o login e/ou ativo de uma pessoa")
    @PutMapping("/pessoas/{id}")
    public ResponseEntity<ApiResponseBody<FinanceiroPessoaDto>> atualizarPessoa(
            @PathVariable Long id, @Valid @RequestBody FinanceiroPessoaForm form) {
        return ok(service.atualizarPessoa(id, form));
    }

    @Operation(summary = "Cria uma categoria (nome, tipo e projeção obrigatórios)")
    @PostMapping("/categorias")
    public ResponseEntity<ApiResponseBody<FinanceiroCategoriaDto>> criarCategoria(@Valid @RequestBody FinanceiroCategoriaForm form) {
        return created(service.criarCategoria(form));
    }

    @Operation(summary = "Atualiza uma categoria (o tipo só muda enquanto não houver lançamentos)")
    @PutMapping("/categorias/{id}")
    public ResponseEntity<ApiResponseBody<FinanceiroCategoriaDto>> atualizarCategoria(
            @PathVariable Long id, @Valid @RequestBody FinanceiroCategoriaForm form) {
        return ok(service.atualizarCategoria(id, form));
    }
}
