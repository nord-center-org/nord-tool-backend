package br.com.nord_tool_backend.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIXO = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String cabecalho = request.getHeader("Authorization");
        if (cabecalho != null && cabecalho.startsWith(PREFIXO)) {
            jwtService.validar(cabecalho.substring(PREFIXO.length()).trim()).ifPresent(usuario -> {
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(usuario, null, autoridades(usuario));
                SecurityContextHolder.getContext().setAuthentication(auth);
            });
        }
        chain.doFilter(request, response);
    }

    /**
     * ROLE_{perfil} e MODULO_{modulo} (ex.: MODULO_CASAMENTO). O módulo "*" vira MODULO_*;
     * a hierarquia futura deve tratar "*" como curinga ao autorizar por módulo.
     */
    static List<GrantedAuthority> autoridades(UsuarioAutenticado usuario) {
        List<GrantedAuthority> lista = new ArrayList<>();
        if (usuario.getPerfil() != null) {
            lista.add(new SimpleGrantedAuthority("ROLE_" + usuario.getPerfil()));
        }
        for (String permissao : usuario.getPermissoes()) {
            String modulo = permissao.contains(":") ? permissao.substring(0, permissao.indexOf(':')) : permissao;
            lista.add(new SimpleGrantedAuthority("MODULO_" + modulo.toUpperCase()));
        }
        return lista;
    }
}
