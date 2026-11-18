package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.exception.NegocioException;
import br.com.nord_tool_backend.domain.PerfilPermissao;
import br.com.nord_tool_backend.domain.Usuario;
import br.com.nord_tool_backend.dto.LoginResponseDto;
import br.com.nord_tool_backend.dto.UsuarioDto;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.exception.NaoAutenticadoException;
import br.com.nord_tool_backend.exception.NordException;
import br.com.nord_tool_backend.form.AlterarSenhaForm;
import br.com.nord_tool_backend.form.LoginForm;
import br.com.nord_tool_backend.repository.UsuarioRepository;
import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.JwtService;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.security.PermissaoCalculator;
import br.com.nord_tool_backend.security.TokenEmitido;
import br.com.nord_tool_backend.security.UsuarioAutenticado;
import br.com.nord_tool_backend.service.AuthService;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.service.LimiteTentativasService;
import br.com.nord_tool_backend.service.SessaoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    static final int MAX_FALHAS = 5;
    static final int MAX_MINUTOS_BLOQUEIO = 15;
    static final int MAX_TENTATIVAS_IP = 10;
    static final int MAX_TENTATIVAS_EMAIL = 5;
    static final String MSG_CREDENCIAIS = "E-mail ou senha inválidos";
    static final String MSG_SESSAO_ENCERRADA = "Sessão encerrada. Faça login novamente.";
    static final String CD_CADASTRO_PENDENTE = "CADASTRO_PENDENTE";
    static final String MSG_CADASTRO_PENDENTE = "Seu cadastro está aguardando a aprovação do administrador da obra.";
    static final String CD_TROCAR_SENHA = "TROCAR_SENHA";
    static final String MSG_TROCAR_SENHA = "Sua senha foi redefinida pelo administrador. Crie a sua senha nova para entrar.";
    static final String CD_PROVISORIA_VENCIDA = "SENHA_PROVISORIA_VENCIDA";
    static final String MSG_PROVISORIA_VENCIDA = "A senha provisória venceu. Peça ao administrador uma nova.";
    static final int MAX_TENTATIVAS_EMAIL_CONTA = 5;
    /** App Android: o aparelho vale enquanto for usado (60 dias sem abrir, pede a senha de novo). */
    static final int DIAS_APARELHO = 60;
    static final int MAX_APARELHOS = 10;
    static final int MAX_TENTATIVAS_APARELHO_IP = 30;
    static final String MSG_APARELHO = "Este aparelho foi desconectado. Entre de novo com a sua senha.";
    static final String MSG_SEM_SESSAO_CONTINUA = "O acesso \"Manter conectado no app\" não está liberado para você. Entre com a sua senha.";
    private static final java.security.SecureRandom ALEATORIO = new java.security.SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SessaoService sessaoService;
    private final LimiteTentativasService limiteTentativas;
    private final AutorizacaoService autorizacao;
    private final Clock clock;
    private final br.com.nord_tool_backend.repository.AppAparelhoRepository aparelhos;

    // Hash descartável para igualar o tempo de resposta quando a senha não é comparada com um hash real.
    private String hashFalso;

    /**
     * Toda recusa responde a mesma mensagem e executa o BCrypt uma vez (e-mail inexistente, conta bloqueada,
     * conta inativa ou senha errada), para não revelar qual foi o motivo pela resposta nem pelo tempo.
     */
    @Override
    @Transactional(rollbackFor = Exception.class, noRollbackFor = NordException.class)
    public LoginResponseDto login(LoginForm form, String ip) {
        Usuario usuario = autenticar(form.getEmail(), form.getSenha(), ip);
        // Senha resetada pelo admin: a provisória confere, mas antes de entrar a pessoa cria a dela.
        if (Boolean.TRUE.equals(usuario.getInTrocarSenha())) {
            exigirProvisoriaValida(usuario);
            throw new NegocioException(CD_TROCAR_SENHA, MSG_TROCAR_SENHA);
        }
        usuarioRepository.registrarLoginSucesso(usuario.getId());
        Instant instante = clock.instant();
        return responder(usuario, instante, instante);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, noRollbackFor = NordException.class)
    public LoginResponseDto definirSenha(br.com.nord_tool_backend.form.DefinirSenhaForm form, String ip) {
        Usuario usuario = autenticar(form.getEmail(), form.getSenhaProvisoria(), ip);
        if (!Boolean.TRUE.equals(usuario.getInTrocarSenha())) {
            throw new EntradaInvalidaException("Esta conta não está aguardando uma senha nova. Entre normalmente.");
        }
        exigirProvisoriaValida(usuario);
        if (normalizarProvisoria(form.getNovaSenha()).equals(normalizarProvisoria(form.getSenhaProvisoria()))) {
            throw new EntradaInvalidaException("A senha nova deve ser diferente da provisória");
        }
        PoliticaSenha.validar(form.getNovaSenha()).ifPresent(motivo -> {
            throw new EntradaInvalidaException("Senha inválida: " + motivo);
        });
        usuarioRepository.alterarSenha(usuario.getId(), passwordEncoder.encode(form.getNovaSenha()));
        sessaoService.invalidar(usuario.getId());
        usuarioRepository.registrarLoginSucesso(usuario.getId());
        Instant instante = clock.instant();
        return responder(buscarAtivo(usuario.getId()), instante, instante);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponseDto alterarEmail(br.com.nord_tool_backend.form.AlterarEmailForm form) {
        UsuarioAutenticado atual = autorizacao.exigirAutenticado();
        limiteTentativas.registrar("alterar-email:" + atual.getId(), MAX_TENTATIVAS_EMAIL_CONTA);
        Usuario usuario = buscarAtivo(atual.getId());
        if (PoliticaSenha.excedeLimiteBcrypt(form.getSenhaAtual())
                || !passwordEncoder.matches(form.getSenhaAtual(), usuario.getNmSenhaHash())) {
            throw new EntradaInvalidaException("Senha atual incorreta");
        }
        String email = form.getNmEmail().trim();
        if (!email.equals(usuario.getNmEmail())) {
            if (usuarioRepository.emailEmUso(email, usuario.getId())) {
                throw new EntradaInvalidaException("Este e-mail já está em uso por outra pessoa");
            }
            usuarioRepository.alterarEmail(usuario.getId(), email);
        }
        return responder(buscarAtivo(usuario.getId()), clock.instant(), atual.getInicioSessao());
    }

    /**
     * Confere e-mail e senha com as mesmas defesas do login (limites, bloqueio progressivo, mesma mensagem e
     * mesmo tempo para toda recusa) e devolve o usuário ativo e aprovado.
     */
    private Usuario autenticar(String emailBruto, String senha, String ip) {
        String email = emailBruto.trim();
        limiteTentativas.registrar("ip:" + ip, MAX_TENTATIVAS_IP);
        limiteTentativas.registrar("email:" + email.toLowerCase(Locale.ROOT), MAX_TENTATIVAS_EMAIL);

        Usuario usuario = PoliticaSenha.excedeLimiteBcrypt(senha)
                ? null : usuarioRepository.buscarPorEmail(email).orElse(null);
        if (usuario == null) {
            passwordEncoder.matches(senha, hashFalso());
            throw credenciaisInvalidas();
        }

        LocalDateTime agora = LocalDateTime.now(clock);
        // A provisória é gerada em maiúsculas: aceita digitada em minúsculas ou com espaços.
        String conferida = Boolean.TRUE.equals(usuario.getInTrocarSenha()) ? normalizarProvisoria(senha) : senha;
        boolean senhaOk = passwordEncoder.matches(conferida, usuario.getNmSenhaHash());
        if (usuario.getDhBloqueadoAte() != null && usuario.getDhBloqueadoAte().isAfter(agora)) {
            log.info("Login recusado: conta temporariamente bloqueada ({})", usuario.getId());
            throw credenciaisInvalidas();
        }
        if (!senhaOk) {
            registrarFalha(usuario.getId(), agora);
            throw credenciaisInvalidas();
        }
        if (!Boolean.TRUE.equals(usuario.getInAtivo()) || "RECUSADO".equals(usuario.getCdSituacaoCadastro())) {
            log.info("Login recusado: conta inativa ({})", usuario.getId());
            throw credenciaisInvalidas();
        }
        // Só depois da senha certa: quem se cadastrou sabe que está aguardando a aprovação.
        if ("PENDENTE".equals(usuario.getCdSituacaoCadastro())) {
            throw new NegocioException(CD_CADASTRO_PENDENTE, MSG_CADASTRO_PENDENTE);
        }
        return usuario;
    }

    private void exigirProvisoriaValida(Usuario usuario) {
        LocalDateTime ate = usuario.getDhSenhaProvisoriaAte();
        if (ate != null && !ate.isAfter(LocalDateTime.now(clock))) {
            throw new NegocioException(CD_PROVISORIA_VENCIDA, MSG_PROVISORIA_VENCIDA);
        }
    }

    static String normalizarProvisoria(String senha) {
        return senha.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponseDto refresh() {
        UsuarioAutenticado atual = autorizacao.exigirAutenticado();
        Usuario usuario = buscarAtivo(atual.getId());
        Instant agora = clock.instant();
        if (SessaoServiceImpl.versao(usuario) != atual.getVersaoSessao()
                || agora.isAfter(atual.getInicioSessao().plus(jwtService.getDuracaoMaximaSessao()))) {
            throw new NaoAutenticadoException(MSG_SESSAO_ENCERRADA);
        }
        return responder(usuario, agora, atual.getInicioSessao());
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioDto me() {
        Usuario usuario = buscarAtivo(autorizacao.exigirAutenticado().getId());
        return toDto(usuario, permissoes(usuario));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponseDto alterarSenha(AlterarSenhaForm form) {
        UsuarioAutenticado atual = autorizacao.exigirAutenticado();
        Usuario usuario = buscarAtivo(atual.getId());
        if (!passwordEncoder.matches(form.getSenhaAtual(), usuario.getNmSenhaHash())) {
            throw new EntradaInvalidaException("Senha atual incorreta");
        }
        if (form.getNovaSenha().equals(form.getSenhaAtual())) {
            throw new EntradaInvalidaException("A nova senha deve ser diferente da atual");
        }
        PoliticaSenha.validar(form.getNovaSenha()).ifPresent(motivo -> {
            throw new EntradaInvalidaException("Senha inválida: " + motivo);
        });
        usuarioRepository.alterarSenha(usuario.getId(), passwordEncoder.encode(form.getNovaSenha()));
        sessaoService.invalidar(usuario.getId());
        // A troca incrementa a versão da sessão: os outros tokens param de valer e esta sessão recebe um novo.
        return responder(buscarAtivo(usuario.getId()), clock.instant(), atual.getInicioSessao());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public br.com.nord_tool_backend.dto.AppAparelhoTokenDto registrarAparelho(br.com.nord_tool_backend.form.AppAparelhoForm form) {
        UsuarioAutenticado atual = autorizacao.exigir(Modulo.SESSAO_CONTINUA, Acao.LEITURA);
        Usuario usuario = buscarAtivo(atual.getId());
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        String token = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String nome = form == null || form.getNmAparelho() == null || form.getNmAparelho().isBlank() ? null : form.getNmAparelho().trim();
        aparelhos.criar(usuario.getId(), hash(token), nome, SessaoServiceImpl.versao(usuario));
        aparelhos.limitar(usuario.getId(), MAX_APARELHOS);
        return new br.com.nord_tool_backend.dto.AppAparelhoTokenDto(token);
    }

    /**
     * Abre uma sessão nova (o limite absoluto conta a partir daqui) para o dono do aparelho, se a conta continua ativa
     * e a versão da sessão é a mesma de quando o aparelho foi registrado: trocar a senha ou o administrador encerrar as
     * sessões desconecta o aparelho.
     */
    @Override
    @Transactional(rollbackFor = Exception.class, noRollbackFor = NordException.class)
    public LoginResponseDto entrarComAparelho(br.com.nord_tool_backend.form.AppAparelhoEntrarForm form, String ip) {
        limiteTentativas.registrar("aparelho-ip:" + ip, MAX_TENTATIVAS_APARELHO_IP);
        br.com.nord_tool_backend.dto.AppAparelhoDto aparelho = aparelhos.porHash(hash(form.getTxToken().trim()), DIAS_APARELHO)
                .orElseThrow(() -> new NaoAutenticadoException("APARELHO_DESCONECTADO", MSG_APARELHO));
        Usuario usuario = usuarioRepository.buscarPorId(aparelho.getIdUsuario())
                .filter(u -> Boolean.TRUE.equals(u.getInAtivo()))
                .filter(u -> "APROVADO".equals(u.getCdSituacaoCadastro()) || u.getCdSituacaoCadastro() == null)
                .filter(u -> !Boolean.TRUE.equals(u.getInTrocarSenha()))
                .filter(u -> SessaoServiceImpl.versao(u) == (aparelho.getNrVersaoSessao() == null ? 0 : aparelho.getNrVersaoSessao()))
                .orElseThrow(() -> new NaoAutenticadoException("APARELHO_DESCONECTADO", MSG_APARELHO));
        // O administrador pode tirar o "Manter conectado" (Acessos): o aparelho cai e o app volta a pedir a senha.
        if (!permissoes(usuario).getOrDefault(Modulo.SESSAO_CONTINUA, Acao.NENHUM).atende(Acao.LEITURA)) {
            aparelhos.revogar(hash(form.getTxToken().trim()));
            throw new NaoAutenticadoException("APARELHO_DESCONECTADO", MSG_SEM_SESSAO_CONTINUA);
        }
        aparelhos.marcarUso(aparelho.getId());
        Instant agora = clock.instant();
        return responder(usuario, agora, agora);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sairDoAparelho(br.com.nord_tool_backend.form.AppAparelhoEntrarForm form) {
        aparelhos.revogar(hash(form.getTxToken().trim()));
    }

    static String hash(String token) {
        try {
            byte[] d = java.security.MessageDigest.getInstance("SHA-256").digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte b : d) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }

    /** Bloqueio progressivo a partir da 5ª falha: 1, 2, 4, 8 e no máximo 15 minutos. */
    private void registrarFalha(Long idUsuario, LocalDateTime agora) {
        int falhas = usuarioRepository.incrementarFalhas(idUsuario);
        if (falhas >= MAX_FALHAS) {
            long minutos = Math.min(MAX_MINUTOS_BLOQUEIO, 1L << Math.min(falhas - MAX_FALHAS, 4));
            usuarioRepository.bloquearAte(idUsuario, agora.plusMinutes(minutos));
        }
    }

    private Usuario buscarAtivo(Long idUsuario) {
        return usuarioRepository.buscarPorId(idUsuario)
                .filter(u -> Boolean.TRUE.equals(u.getInAtivo()))
                .orElseThrow(() -> new NaoAutenticadoException(MSG_SESSAO_ENCERRADA));
    }

    private Map<Modulo, Acao> permissoes(Usuario usuario) {
        // O cargo (por módulo) vem antes do perfil: dá os acessos mínimos de quem tem aquele cargo.
        List<PerfilPermissao> base = new java.util.ArrayList<>(usuarioRepository.listarPermissoesCargo(usuario.getIdCargo()));
        base.addAll(usuarioRepository.listarPermissoes(usuario.getIdPerfil()));
        return PermissaoCalculator.resolver(base, usuarioRepository.listarPermissoesUsuario(usuario.getId()));
    }

    private LoginResponseDto responder(Usuario usuario, Instant agora, Instant inicioSessao) {
        Map<Modulo, Acao> permissoes = permissoes(usuario);
        UsuarioAutenticado principal = new UsuarioAutenticado(usuario.getId(), usuario.getNmEmail(), usuario.getCdPerfil(),
                permissoes, SessaoServiceImpl.versao(usuario), inicioSessao);
        TokenEmitido emitido = jwtService.emitir(principal, agora);
        return new LoginResponseDto(emitido.getToken(), emitido.getExpiraEm().toString(),
                jwtService.getDuracaoToken().toMinutes(),
                inicioSessao.plus(jwtService.getDuracaoMaximaSessao()).toString(),
                toDto(usuario, permissoes));
    }

    private static UsuarioDto toDto(Usuario usuario, Map<Modulo, Acao> permissoes) {
        List<PerfilPermissao> lista = permissoes.entrySet().stream()
                .map(e -> new PerfilPermissao(e.getKey().name(), e.getValue().name()))
                .collect(Collectors.toList());
        return new UsuarioDto(usuario.getId(), usuario.getNmNome(), usuario.getNmEmail(), usuario.getCdPerfil(), lista);
    }

    private static NaoAutenticadoException credenciaisInvalidas() {
        return new NaoAutenticadoException("CREDENCIAIS_INVALIDAS", MSG_CREDENCIAIS);
    }

    private synchronized String hashFalso() {
        if (hashFalso == null) hashFalso = passwordEncoder.encode("senha-descartavel-" + System.nanoTime());
        return hashFalso;
    }
}
