package br.com.nord_tool_backend.service;

import br.com.nord_tool_backend.dto.MensagemDto;
import br.com.nord_tool_backend.form.CadastroForm;
import br.com.nord_tool_backend.form.EsqueciSenhaForm;
import br.com.nord_tool_backend.form.RedefinirSenhaForm;

/** Rotas públicas da tela de login: criar conta, pedir e usar o link de nova senha. */
public interface CadastroService {
    MensagemDto cadastrar(CadastroForm form, String ip);
    MensagemDto esqueciSenha(EsqueciSenhaForm form, String ip);
    MensagemDto redefinirSenha(RedefinirSenhaForm form, String ip);
}
