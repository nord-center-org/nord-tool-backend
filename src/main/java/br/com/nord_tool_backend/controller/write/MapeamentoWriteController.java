package br.com.nord_tool_backend.controller.write;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.MapeamentoDto;
import br.com.nord_tool_backend.form.MapeamentoForm;
import br.com.nord_tool_backend.form.MapeamentoStatusForm;
import br.com.nord_tool_backend.service.MapeamentoService;
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
@RequestMapping("/api/v1/nord-tool/mapeamentos")
@Tag(name = "Mapeamentos", description = "Contagens de campo e quantitativos")
public class MapeamentoWriteController implements BaseResponse {

    private final MapeamentoService service;

    @Operation(summary = "Cria um mapeamento (cdTipo CONTAGEM ou QUANTITATIVO) na fase do contexto")
    @PostMapping
    public ResponseEntity<ApiResponseBody<MapeamentoDto>> criar(@Valid @RequestBody MapeamentoForm form) {
        return created(service.criar(form));
    }

    @Operation(summary = "Altera cabeçalho e, se enviadas, substitui as linhas; exige nrVersao")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseBody<MapeamentoDto>> alterar(@PathVariable Long id, @Valid @RequestBody MapeamentoForm form) {
        return ok(service.alterar(id, form));
    }

    @Operation(summary = "Conclui (CONCLUIDO) ou reabre (RASCUNHO); exige nrVersao")
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponseBody<MapeamentoDto>> status(@PathVariable Long id, @Valid @RequestBody MapeamentoStatusForm form) {
        return ok(service.alterarStatus(id, form));
    }

    @Operation(summary = "Exclui o mapeamento (nrVersao divergente → 409)")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseBody<Void>> excluir(@PathVariable Long id,
            @RequestParam(value = "nrVersao", required = false) Integer nrVersao) {
        service.excluir(id, nrVersao);
        return noContent();
    }
}
