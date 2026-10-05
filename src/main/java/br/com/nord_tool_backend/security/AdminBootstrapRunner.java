package br.com.nord_tool_backend.security;

import br.com.nord_tool_backend.domain.Usuario;
import br.com.nord_tool_backend.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cria o primeiro usuário ADMIN quando a tabela {@code usuario} está vazia e as variáveis
 * NORD_ADMIN_EMAIL, NORD_ADMIN_NAME e NORD_ADMIN_PASSWORD estão definidas.
 * A senha nunca é logada. Remova as variáveis depois do primeiro start.
 */
@Component @Slf4j
public class AdminBootstrapRunner implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String nome;
    private final String senha;

    public AdminBootstrapRunner(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                                @Value("${nord-tool.admin.email:}") String email,
                                @Value("${nord-tool.admin.name:}") String nome,
                                @Value("${nord-tool.admin.password:}") String senha) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.nome = nome;
        this.senha = senha;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(String... args) {
        if (vazio(email) || vazio(nome) || vazio(senha)) return;
        try {
            if (usuarioRepository.contar() > 0) return;
            Long idPerfil = usuarioRepository.buscarIdPerfil("ADMIN").orElse(null);
            if (idPerfil == null) {
                log.warn("Bootstrap do admin ignorado: perfil ADMIN não existe (aplique db/scripts/001_auth.sql).");
                return;
            }
            Usuario admin = new Usuario();
            admin.setNmEmail(email.trim());
            admin.setNmNome(nome.trim());
            admin.setNmSenhaHash(passwordEncoder.encode(senha));
            admin.setIdPerfil(idPerfil);
            usuarioRepository.inserir(admin);
            log.info("Usuário ADMIN inicial criado ({}). Remova as variáveis NORD_ADMIN_*.", email.trim());
        } catch (RuntimeException ex) {
            // Não derruba a aplicação (ex.: script SQL ainda não aplicado).
            log.warn("Bootstrap do admin não executado: {}", ex.getMessage());
        }
    }

    private static boolean vazio(String valor) {
        return valor == null || valor.trim().isEmpty();
    }
}
