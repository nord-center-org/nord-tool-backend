package br.com.nord_tool_backend.security;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.NordHttpEnum;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private static final String API = "/api/v1/nord-tool";

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityProperties props,
                                                   JwtAuthenticationFilter jwtFilter, Environment env) throws Exception {
        http.csrf().disable()
                .cors().and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling()
                .authenticationEntryPoint((req, res, ex) ->
                        escreverErro(res, HttpServletResponse.SC_UNAUTHORIZED, NordHttpEnum.HTTP_401, "Autenticação necessária"))
                .accessDeniedHandler((req, res, ex) ->
                        escreverErro(res, HttpServletResponse.SC_FORBIDDEN, NordHttpEnum.HTTP_401, "Acesso negado"));

        if (!props.isEnabled()) {
            // Segurança desligada: comportamento anterior (tudo liberado).
            http.authorizeRequests().anyRequest().permitAll();
            return http.build();
        }

        http.authorizeRequests()
                .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .antMatchers(HttpMethod.POST, API + "/auth/login").permitAll()
                .antMatchers(HttpMethod.GET, API + "/health", "/nord-tool/health").permitAll();
        if (env.acceptsProfiles(Profiles.of("local"))) {
            http.authorizeRequests().antMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll();
        }
        // Financeiro: dados pessoais. LEITURA do módulo consulta, ESCRITA altera ("*" vale para todos os módulos).
        // Regra por URL, na cadeia de filtros: recusa com 403 antes de ler o corpo ou validar qualquer coisa.
        http.authorizeRequests()
                .antMatchers(HttpMethod.GET, API + "/financeiro/**").access("@acessoModulo.leitura(authentication, 'FINANCEIRO')")
                .antMatchers(API + "/financeiro/**").access("@acessoModulo.escrita(authentication, 'FINANCEIRO')");
        http.authorizeRequests().anyRequest().authenticated();
        return http.build();
    }

    private static void escreverErro(HttpServletResponse res, int status, NordHttpEnum tipo, String mensagem)
            throws java.io.IOException {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        res.getWriter().write(mapper.writeValueAsString(new ApiResponseBody<String>(tipo, mensagem, null)));
    }
}
