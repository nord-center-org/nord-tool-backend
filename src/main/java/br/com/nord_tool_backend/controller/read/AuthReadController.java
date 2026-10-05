package br.com.nord_tool_backend.controller.read;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.controller.response.NordHttpEnum;
import br.com.nord_tool_backend.dto.UsuarioDto;
import br.com.nord_tool_backend.excepetion.ValidacaoException;
import br.com.nord_tool_backend.security.UsuarioAutenticado;
import br.com.nord_tool_backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nord-tool/auth")
@Tag(name = "Autenticação", description = "Login e sessão (JWT)")
public class AuthReadController implements BaseResponse {

    private final AuthService authService;

    @GetMapping("/me")
    @Operation(summary = "Dados do usuário logado")
    public ResponseEntity<ApiResponseBody<UsuarioDto>> me(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (usuario == null) {
            throw new ValidacaoException(NordHttpEnum.HTTP_401, "Autenticação necessária", null);
        }
        return ok(authService.me(usuario.getId()));
    }
}
