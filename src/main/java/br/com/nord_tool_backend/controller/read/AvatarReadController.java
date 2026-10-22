package br.com.nord_tool_backend.controller.read;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.AvatarDto;
import br.com.nord_tool_backend.service.AvatarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nord-tool/avatares")
@Tag(name = "Avatares", description = "Avatar do usuário logado e da equipe")
public class AvatarReadController implements BaseResponse {

    private final AvatarService service;

    @Operation(summary = "Avatar do usuário logado (dsConfig nulo = padrão)")
    @GetMapping("/meu")
    public ResponseEntity<ApiResponseBody<AvatarDto>> meu() {
        return ok(service.meu());
    }

    @Operation(summary = "Avatares do usuário, do superior, dos colegas e da equipe abaixo dele")
    @GetMapping("/equipe")
    public ResponseEntity<ApiResponseBody<List<AvatarDto>>> equipe() {
        return ok(service.equipe());
    }
}
