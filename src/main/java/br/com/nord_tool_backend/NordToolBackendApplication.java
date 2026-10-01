package br.com.nord_tool_backend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class NordToolBackendApplication {

    private static final Logger log = LoggerFactory.getLogger(NordToolBackendApplication.class);

    public static void main(String[] args) {SpringApplication.run(NordToolBackendApplication.class, args);

        log.info("Heap Máxima: {} MB", Runtime.getRuntime().maxMemory() / 1024 / 1024);
    }
}
