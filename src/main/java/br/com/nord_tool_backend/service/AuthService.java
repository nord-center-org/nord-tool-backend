package br.com.nord_tool_backend.service;

import br.com.nord_tool_backend.dto.LoginResponseDto;
import br.com.nord_tool_backend.dto.UsuarioDto;
import br.com.nord_tool_backend.form.AlterarSenhaForm;
import br.com.nord_tool_backend.form.LoginForm;

public interface AuthService {
    LoginResponseDto login(LoginForm form);
    LoginResponseDto refresh(Long idUsuario);
    UsuarioDto me(Long idUsuario);
    void alterarSenha(Long idUsuario, AlterarSenhaForm form);
}
