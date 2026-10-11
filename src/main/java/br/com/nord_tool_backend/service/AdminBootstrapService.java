package br.com.nord_tool_backend.service;

/**
 * Com NORD_ADMIN_EMAIL, NORD_ADMIN_NAME e NORD_ADMIN_PASSWORD definidas, garante um ADMIN com esse e-mail: cria se o
 * e-mail não existir e não altera um usuário já existente (nem a senha). Enquanto não há tela de cadastro de usuários,
 * é o jeito de ter um ADMIN em um ambiente que já tem outros usuários. Com as variáveis definidas, qualquer falha derruba o startup.
 */
public interface AdminBootstrapService {

    /** Devolve true se criou o ADMIN. */
    boolean executar(String email, String nome, String senha);
}
