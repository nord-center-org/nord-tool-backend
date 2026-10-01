package br.com.nord_tool_backend.controller.write;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.EmpresaDto;
import br.com.nord_tool_backend.form.EmpresaForm;
import br.com.nord_tool_backend.service.EmpresaService;
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
@RequestMapping("/api/empresas")
public class EmpresaWriteController implements BaseResponse {
    private final EmpresaService empresaService;

    @PostMapping
    public ResponseEntity<ApiResponseBody<EmpresaDto>> criarEmpresa(@Valid @RequestBody EmpresaForm empresaForm) {
        return created(empresaService.salvarEmpresa(empresaForm));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseBody<EmpresaDto>> alterarEmpresa(
            @PathVariable Long id, @Valid @RequestBody EmpresaForm empresaForm) {
        return ok(empresaService.alterarEmpresa(id, empresaForm));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseBody<Void>> deletarEmpresa(@PathVariable Long id) {
        empresaService.deletarEmpresa(id);
        return noContent();
    }
}
