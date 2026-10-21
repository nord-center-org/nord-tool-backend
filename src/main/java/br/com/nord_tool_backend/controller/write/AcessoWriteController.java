package br.com.nord_tool_backend.controller.write;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.AcessoUsuarioDto;
import br.com.nord_tool_backend.form.AcessoUsuarioForm;
import br.com.nord_tool_backend.service.AcessoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nord-tool/administracao/acessos")
@Tag(name = "Acessos", description = "Perfis e acessos por usuário (só administradores)")
public class AcessoWriteController implements BaseResponse {

    private final AcessoService service;

    @Operation(summary = "Salva perfil, situação e nível por módulo do usuário (ele entra de novo com os acessos novos)")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseBody<AcessoUsuarioDto>> salvar(@PathVariable Long id, @Valid @RequestBody AcessoUsuarioForm form) {
        return ok(service.salvar(id, form));
    }
}
