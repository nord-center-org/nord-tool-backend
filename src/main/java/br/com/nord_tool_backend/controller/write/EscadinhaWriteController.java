package br.com.nord_tool_backend.controller.write;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.EscadinhaDto;
import br.com.nord_tool_backend.dto.EscadinhaTorreDto;
import br.com.nord_tool_backend.form.EscadinhaModeloForm;
import br.com.nord_tool_backend.form.EscadinhaTorreForm;
import br.com.nord_tool_backend.service.EscadinhaService;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nord-tool/escadinha")
@Tag(name = "Escadinha", description = "Prédio 3D, apartamentos, lados A/B e ciclos por pavimento")
public class EscadinhaWriteController implements BaseResponse {

    private final EscadinhaService service;

    @Operation(summary = "Cria uma torre na obra escolhida")
    @PostMapping("/torres")
    public ResponseEntity<ApiResponseBody<EscadinhaTorreDto>> criar(@Valid @RequestBody EscadinhaTorreForm form) {
        return created(service.criarTorre(form));
    }

    @Operation(summary = "Salva a torre (croqui, apartamentos, lados, ciclos); exige nrVersao (divergente → 409)")
    @PutMapping("/torres/{id}")
    public ResponseEntity<ApiResponseBody<EscadinhaTorreDto>> alterar(@PathVariable Long id, @Valid @RequestBody EscadinhaTorreForm form) {
        return ok(service.alterarTorre(id, form));
    }

    @Operation(summary = "Exclui a torre")
    @DeleteMapping("/torres/{id}")
    public ResponseEntity<ApiResponseBody<Void>> excluir(@PathVariable Long id) {
        service.excluirTorre(id);
        return noContent();
    }

    @Operation(summary = "Salva o modelo de ciclos da obra (atividades padrão)")
    @PutMapping("/modelo")
    public ResponseEntity<ApiResponseBody<EscadinhaDto>> salvarModelo(@Valid @RequestBody EscadinhaModeloForm form) {
        return ok(service.salvarModelo(form));
    }
}
