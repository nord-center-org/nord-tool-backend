package br.com.nord_tool_backend.controller.write;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.PermissaoDto;
import br.com.nord_tool_backend.form.PermissaoForm;
import br.com.nord_tool_backend.service.PermissaoService;
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
@RequestMapping("/api/v1/nord-tool/permissoes")
public class PermissaoWriteController implements BaseResponse {
    private final PermissaoService permissaoService;

    @PostMapping
    public ResponseEntity<ApiResponseBody<PermissaoDto>> criarPermissao(@Valid @RequestBody PermissaoForm permissaoForm) {
        return created(permissaoService.salvarPermissao(permissaoForm));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseBody<PermissaoDto>> alterarPermissao(
            @PathVariable Long id, @Valid @RequestBody PermissaoForm permissaoForm) {
        return ok(permissaoService.alterarPermissao(id, permissaoForm));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseBody<Void>> deletarPermissao(@PathVariable Long id) {
        permissaoService.deletarPermissao(id);
        return noContent();
    }
}
