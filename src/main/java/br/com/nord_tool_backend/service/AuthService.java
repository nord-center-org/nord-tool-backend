package br.com.nord_tool_backend.service;

import br.com.nord_tool_backend.dto.LoginResponseDto;
import br.com.nord_tool_backend.dto.UsuarioDto;
import br.com.nord_tool_backend.form.AlterarSenhaForm;
import br.com.nord_tool_backend.form.LoginForm;

public interface AuthService {

    /** Autentica por e-mail e senha; {@code ip} alimenta o limite de tentativas por origem. */
    LoginResponseDto login(LoginForm form, String ip);

    /** Renova o token do usuário atual, respeitando versão da sessão e limite absoluto desde o login. */
    LoginResponseDto refresh();

    UsuarioDto me();

    /** Troca a senha, encerra as demais sessões e devolve um token novo para a sessão atual. */
    LoginResponseDto alterarSenha(AlterarSenhaForm form);
    /** Depois do reset pelo admin: confere e-mail + senha provisória, grava a senha nova e já entra. */
    LoginResponseDto definirSenha(br.com.nord_tool_backend.form.DefinirSenhaForm form, String ip);
    /** Troca o próprio e-mail (com a senha atual) e devolve um token novo. */
    LoginResponseDto alterarEmail(br.com.nord_tool_backend.form.AlterarEmailForm form);

    /** App Android: registra este aparelho para a sessão não cair; devolve o token (só desta vez). */
    br.com.nord_tool_backend.dto.AppAparelhoTokenDto registrarAparelho(br.com.nord_tool_backend.form.AppAparelhoForm form);

    /** App Android: abre uma sessão nova com o token do aparelho, sem pedir a senha. */
    LoginResponseDto entrarComAparelho(br.com.nord_tool_backend.form.AppAparelhoEntrarForm form, String ip);

    /** App Android: saiu da conta no aparelho; o token deixa de valer. */
    void sairDoAparelho(br.com.nord_tool_backend.form.AppAparelhoEntrarForm form);
}
