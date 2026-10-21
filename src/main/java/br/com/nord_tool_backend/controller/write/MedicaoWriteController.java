package br.com.nord_tool_backend.controller.write;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.MedicaoDto;
import br.com.nord_tool_backend.form.MedicaoForm;
import br.com.nord_tool_backend.form.MedicaoStatusForm;
import br.com.nord_tool_backend.service.MedicaoService;
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
@RequestMapping("/api/v1/nord-tool/medicoes")
@Tag(name = "Medições", description = "Controle de medições de contratos")
public class MedicaoWriteController implements BaseResponse {

    private final MedicaoService service;

    @Operation(summary = "Cria uma medição (status vazio = PENDENTE; contrato + nº da medição não repete)")
    @PostMapping
    public ResponseEntity<ApiResponseBody<MedicaoDto>> criar(@Valid @RequestBody MedicaoForm form) {
        return created(service.criar(form));
    }

    @Operation(summary = "Edita uma medição; exige nrVersao (divergente → 409)")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseBody<MedicaoDto>> alterar(@PathVariable Long id, @Valid @RequestBody MedicaoForm form) {
        return ok(service.alterar(id, form));
    }

    @Operation(summary = "Troca só o status da medição; exige nrVersao (divergente → 409)")
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponseBody<MedicaoDto>> alterarStatus(@PathVariable Long id, @Valid @RequestBody MedicaoStatusForm form) {
        return ok(service.alterarStatus(id, form));
    }

    @Operation(summary = "Exclui a medição (nrVersao divergente → 409)")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseBody<Void>> excluir(@PathVariable Long id,
            @RequestParam(value = "nrVersao", required = false) Integer nrVersao) {
        service.excluir(id, nrVersao);
        return noContent();
    }
}
