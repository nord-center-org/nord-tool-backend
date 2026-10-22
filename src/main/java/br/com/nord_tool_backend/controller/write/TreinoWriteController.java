package br.com.nord_tool_backend.controller.write;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.TreinoDto;
import br.com.nord_tool_backend.dto.TreinoSessaoDto;
import br.com.nord_tool_backend.form.TreinoForm;
import br.com.nord_tool_backend.form.TreinoSessaoForm;
import br.com.nord_tool_backend.service.TreinoService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nord-tool/treinos")
@Tag(name = "Treinos", description = "Fichas de treino e sessões (dados do usuário logado)")
public class TreinoWriteController implements BaseResponse {

    private final TreinoService service;

    @Operation(summary = "Cria uma ficha de treino com os exercícios")
    @PostMapping
    public ResponseEntity<ApiResponseBody<TreinoDto>> criar(@Valid @RequestBody TreinoForm form) {
        return created(service.criar(form));
    }

    @Operation(summary = "Altera a ficha e substitui os exercícios; exige nrVersao")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseBody<TreinoDto>> alterar(@PathVariable Long id, @Valid @RequestBody TreinoForm form) {
        return ok(service.alterar(id, form));
    }

    @Operation(summary = "Exclui a ficha (o histórico de sessões fica, sem o vínculo)")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseBody<Void>> excluir(@PathVariable Long id, @RequestParam(value = "nrVersao", required = false) Integer nrVersao) {
        service.excluir(id, nrVersao);
        return noContent();
    }

    @Operation(summary = "Registra um treino feito; atualizarCargas leva a carga usada para a ficha")
    @PostMapping("/sessoes")
    public ResponseEntity<ApiResponseBody<TreinoSessaoDto>> registrar(@Valid @RequestBody TreinoSessaoForm form) {
        return created(service.registrarSessao(form));
    }

    @Operation(summary = "Exclui uma sessão registrada")
    @DeleteMapping("/sessoes/{id}")
    public ResponseEntity<ApiResponseBody<Void>> excluirSessao(@PathVariable Long id) {
        service.excluirSessao(id);
        return noContent();
    }
}
