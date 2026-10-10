package br.com.nord_tool_backend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
// Sem o usuário em memória padrão do Spring Boot: a autenticação é só por JWT (e ele imprimiria uma senha no log).
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class NordToolBackendApplication {

    private static final Logger log = LoggerFactory.getLogger(NordToolBackendApplication.class);

    public static void main(String[] args) {SpringApplication.run(NordToolBackendApplication.class, args);

        log.info("Heap Máxima: {} MB", Runtime.getRuntime().maxMemory() / 1024 / 1024);
    }
}
