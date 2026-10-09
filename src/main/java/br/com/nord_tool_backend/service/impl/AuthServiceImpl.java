package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.exception.NordException;
import br.com.nord_tool_backend.exception.NaoAutenticadoException;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.domain.PerfilPermissao;
import br.com.nord_tool_backend.domain.Usuario;
import br.com.nord_tool_backend.dto.LoginResponseDto;
import br.com.nord_tool_backend.dto.UsuarioDto;
import br.com.nord_tool_backend.form.AlterarSenhaForm;
import br.com.nord_tool_backend.form.LoginForm;
import br.com.nord_tool_backend.repository.UsuarioRepository;
import br.com.nord_tool_backend.security.JwtService;
import br.com.nord_tool_backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    static final int MAX_FALHAS = 5;
    static final int MINUTOS_BLOQUEIO = 15;
    static final String MSG_CREDENCIAIS = "E-mail ou senha inválidos";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Clock clock;

    // Hash descartável para igualar o tempo de resposta quando o e-mail não existe.
    private String hashFalso;

    @Override
    @Transactional(rollbackFor = Exception.class, noRollbackFor = NordException.class)
    public LoginResponseDto login(LoginForm form) {
        Usuario usuario = usuarioRepository.buscarPorEmail(form.getEmail().trim()).orElse(null);
        if (usuario == null) {
            passwordEncoder.matches(form.getSenha(), hashFalso());
            throw erro(MSG_CREDENCIAIS);
        }

        LocalDateTime agora = LocalDateTime.now(clock);
        if (usuario.getDhBloqueadoAte() != null && usuario.getDhBloqueadoAte().isAfter(agora)) {
            long minutos = Math.max(1, Duration.between(agora, usuario.getDhBloqueadoAte()).plusSeconds(59).toMinutes());
            throw erro("Conta bloqueada por excesso de tentativas. Tente novamente em " + minutos + " minuto(s).");
        }

        boolean senhaOk = passwordEncoder.matches(form.getSenha(), usuario.getNmSenhaHash());
        if (!senhaOk || !Boolean.TRUE.equals(usuario.getInAtivo())) {
            if (!senhaOk) registrarFalha(usuario, agora);
            throw erro(MSG_CREDENCIAIS);
        }

        usuarioRepository.registrarLoginSucesso(usuario.getId());
        return montarResposta(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponseDto refresh(Long idUsuario) {
        return montarResposta(buscarAtivo(idUsuario));
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioDto me(Long idUsuario) {
        Usuario usuario = buscarAtivo(idUsuario);
        return toDto(usuario, usuarioRepository.listarPermissoes(usuario.getIdPerfil()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void alterarSenha(Long idUsuario, AlterarSenhaForm form) {
        Usuario usuario = buscarAtivo(idUsuario);
        if (!passwordEncoder.matches(form.getSenhaAtual(), usuario.getNmSenhaHash())) {
            throw new EntradaInvalidaException("Senha atual incorreta");
        }
        if (form.getNovaSenha().equals(form.getSenhaAtual())) {
            throw new EntradaInvalidaException("A nova senha deve ser diferente da atual");
        }
        usuarioRepository.alterarSenha(usuario.getId(), passwordEncoder.encode(form.getNovaSenha()));
    }

    private void registrarFalha(Usuario usuario, LocalDateTime agora) {
        int falhas = (usuario.getNrFalhasLogin() == null ? 0 : usuario.getNrFalhasLogin()) + 1;
        if (falhas >= MAX_FALHAS) {
            usuarioRepository.registrarFalha(usuario.getId(), 0, agora.plusMinutes(MINUTOS_BLOQUEIO));
        } else {
            usuarioRepository.registrarFalha(usuario.getId(), falhas, null);
        }
    }

    private Usuario buscarAtivo(Long idUsuario) {
        return usuarioRepository.buscarPorId(idUsuario)
                .filter(u -> Boolean.TRUE.equals(u.getInAtivo()))
                .orElseThrow(() -> new NaoAutenticadoException("Sessão inválida"));
    }

    private LoginResponseDto montarResposta(Usuario usuario) {
        List<PerfilPermissao> permissoes = usuarioRepository.listarPermissoes(usuario.getIdPerfil());
        Instant agora = clock.instant();
        List<String> claims = permissoes.stream()
                .map(p -> p.getCdModulo() + ":" + p.getCdAcao())
                .collect(Collectors.toList());
        String token = jwtService.gerar(usuario.getId(), usuario.getNmEmail(), usuario.getCdPerfil(), claims, agora);
        return new LoginResponseDto(token, jwtService.calcularExpiracao(agora).toString(),
                jwtService.getInactivityMinutes(), toDto(usuario, permissoes));
    }

    private UsuarioDto toDto(Usuario usuario, List<PerfilPermissao> permissoes) {
        return new UsuarioDto(usuario.getId(), usuario.getNmNome(), usuario.getNmEmail(), usuario.getCdPerfil(), permissoes);
    }

    private NordException erro(String mensagem) {
        return new NaoAutenticadoException(mensagem);
    }

    private synchronized String hashFalso() {
        if (hashFalso == null) hashFalso = passwordEncoder.encode("senha-descartavel-" + System.nanoTime());
        return hashFalso;
    }
}
