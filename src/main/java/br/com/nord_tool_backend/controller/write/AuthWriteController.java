package br.com.nord_tool_backend.controller.write;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.LoginResponseDto;
import br.com.nord_tool_backend.form.AlterarSenhaForm;
import br.com.nord_tool_backend.form.LoginForm;
import br.com.nord_tool_backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nord-tool/auth")
@Tag(name = "Autenticação", description = "Login e sessão (JWT)")
public class AuthWriteController implements BaseResponse {

    private final AuthService authService;
    private final br.com.nord_tool_backend.service.CadastroService cadastroService;

    @PostMapping("/login")
    @Operation(summary = "Autentica por e-mail e senha e devolve o JWT")
    public ResponseEntity<ApiResponseBody<LoginResponseDto>> login(@Valid @RequestBody LoginForm form,
                                                                   HttpServletRequest request) {
        // Atrás de proxy, o IP real vem de X-Forwarded-For via server.forward-headers-strategy (perfil railway).
        return ok(authService.login(form, request.getRemoteAddr()));
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/cadastro")
    @Operation(summary = "Cria a conta (fica aguardando a aprovação do admin, sem nenhum acesso)")
    public ResponseEntity<ApiResponseBody<br.com.nord_tool_backend.dto.MensagemDto>> cadastrar(
            @Valid @RequestBody br.com.nord_tool_backend.form.CadastroForm form, HttpServletRequest request) {
        return created(cadastroService.cadastrar(form, request.getRemoteAddr()));
    }

    @PostMapping("/esqueci-senha")
    @Operation(summary = "Envia por e-mail um link para criar nova senha (resposta igual exista ou não o e-mail)")
    public ResponseEntity<ApiResponseBody<br.com.nord_tool_backend.dto.MensagemDto>> esqueciSenha(
            @Valid @RequestBody br.com.nord_tool_backend.form.EsqueciSenhaForm form, HttpServletRequest request) {
        return ok(cadastroService.esqueciSenha(form, request.getRemoteAddr()));
    }

    @PostMapping("/redefinir-senha")
    @Operation(summary = "Cria a nova senha com o link do e-mail (uso único, 30 minutos)")
    public ResponseEntity<ApiResponseBody<br.com.nord_tool_backend.dto.MensagemDto>> redefinirSenha(
            @Valid @RequestBody br.com.nord_tool_backend.form.RedefinirSenhaForm form, HttpServletRequest request) {
        return ok(cadastroService.redefinirSenha(form, request.getRemoteAddr()));
    }

    @PostMapping("/definir-senha")
    @Operation(summary = "Depois do reset pelo admin: e-mail + senha provisória + senha nova; já devolve o JWT")
    public ResponseEntity<ApiResponseBody<LoginResponseDto>> definirSenha(
            @Valid @RequestBody br.com.nord_tool_backend.form.DefinirSenhaForm form, HttpServletRequest request) {
        return ok(authService.definirSenha(form, request.getRemoteAddr()));
    }

    @PostMapping("/alterar-email")
    @Operation(summary = "Troca o próprio e-mail (confirma com a senha atual) e devolve token novo")
    public ResponseEntity<ApiResponseBody<LoginResponseDto>> alterarEmail(
            @Valid @RequestBody br.com.nord_tool_backend.form.AlterarEmailForm form) {
        return ok(authService.alterarEmail(form));
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/aparelho")
    @Operation(summary = "App Android: registra o aparelho para a sessão não cair (devolve o token uma única vez)")
    public ResponseEntity<ApiResponseBody<br.com.nord_tool_backend.dto.AppAparelhoTokenDto>> registrarAparelho(
            @Valid @RequestBody(required = false) br.com.nord_tool_backend.form.AppAparelhoForm form) {
        return created(authService.registrarAparelho(form));
    }

    @PostMapping("/aparelho/entrar")
    @Operation(summary = "App Android: abre uma sessão nova com o token do aparelho, sem senha")
    public ResponseEntity<ApiResponseBody<LoginResponseDto>> entrarComAparelho(
            @Valid @RequestBody br.com.nord_tool_backend.form.AppAparelhoEntrarForm form, HttpServletRequest request) {
        return ok(authService.entrarComAparelho(form, request.getRemoteAddr()));
    }

    @PostMapping("/aparelho/sair")
    @Operation(summary = "App Android: desconecta o aparelho (o token deixa de valer)")
    public ResponseEntity<ApiResponseBody<Void>> sairDoAparelho(@Valid @RequestBody br.com.nord_tool_backend.form.AppAparelhoEntrarForm form) {
        authService.sairDoAparelho(form);
        return ok(null);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renova o token (versão de sessão e limite absoluto desde o login)")
    public ResponseEntity<ApiResponseBody<LoginResponseDto>> refresh() {
        return ok(authService.refresh());
    }

    @PostMapping("/alterar-senha")
    @Operation(summary = "Altera a senha (10 caracteres a 72 bytes), encerra as outras sessões e devolve token novo")
    public ResponseEntity<ApiResponseBody<LoginResponseDto>> alterarSenha(@Valid @RequestBody AlterarSenhaForm form) {
        return ok(authService.alterarSenha(form));
    }
}
