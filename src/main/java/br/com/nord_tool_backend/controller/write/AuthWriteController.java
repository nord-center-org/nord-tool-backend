package br.com.nord_tool_backend.controller.write;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.controller.response.NordHttpEnum;
import br.com.nord_tool_backend.dto.LoginResponseDto;
import br.com.nord_tool_backend.excepetion.ValidacaoException;
import br.com.nord_tool_backend.form.AlterarSenhaForm;
import br.com.nord_tool_backend.form.LoginForm;
import br.com.nord_tool_backend.security.UsuarioAutenticado;
import br.com.nord_tool_backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nord-tool/auth")
@Tag(name = "Autenticação", description = "Login e sessão (JWT)")
public class AuthWriteController implements BaseResponse {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Autentica por e-mail e senha e devolve o JWT")
    public ResponseEntity<ApiResponseBody<LoginResponseDto>> login(@Valid @RequestBody LoginForm form) {
        return ok(authService.login(form));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renova o token (expiração por inatividade)")
    public ResponseEntity<ApiResponseBody<LoginResponseDto>> refresh(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ok(authService.refresh(exigir(usuario).getId()));
    }

    @PostMapping("/alterar-senha")
    @Operation(summary = "Altera a senha do usuário logado (mínimo 10 caracteres)")
    public ResponseEntity<ApiResponseBody<Void>> alterarSenha(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                              @Valid @RequestBody AlterarSenhaForm form) {
        authService.alterarSenha(exigir(usuario).getId(), form);
        return noContent();
    }

    static UsuarioAutenticado exigir(UsuarioAutenticado usuario) {
        if (usuario == null) {
            throw new ValidacaoException(NordHttpEnum.HTTP_401, "Autenticação necessária", null);
        }
        return usuario;
    }
}
