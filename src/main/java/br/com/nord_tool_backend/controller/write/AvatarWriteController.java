package br.com.nord_tool_backend.controller.write;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.AvatarDto;
import br.com.nord_tool_backend.form.AvatarForm;
import br.com.nord_tool_backend.service.AvatarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nord-tool/avatares")
@Tag(name = "Avatares", description = "Avatar do usuário logado e da equipe")
public class AvatarWriteController implements BaseResponse {

    private final AvatarService service;

    @Operation(summary = "Salva o avatar do usuário logado")
    @PutMapping("/meu")
    public ResponseEntity<ApiResponseBody<AvatarDto>> salvar(@Valid @RequestBody AvatarForm form) {
        return ok(service.salvar(form));
    }
}
