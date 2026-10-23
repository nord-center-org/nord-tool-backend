package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.domain.Obra;
import br.com.nord_tool_backend.domain.Usuario;
import br.com.nord_tool_backend.dto.MensagemDto;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.form.CadastroForm;
import br.com.nord_tool_backend.form.EsqueciSenhaForm;
import br.com.nord_tool_backend.form.RedefinirSenhaForm;
import br.com.nord_tool_backend.repository.CadastroRepository;
import br.com.nord_tool_backend.repository.UsuarioRepository;
import br.com.nord_tool_backend.service.CadastroService;
import br.com.nord_tool_backend.service.EmailService;
import br.com.nord_tool_backend.service.LimiteTentativasService;
import br.com.nord_tool_backend.service.SessaoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;

/**
 * Cadastro pelo próprio usuário (fica PENDENTE até o admin aprovar) e recuperação de senha por e-mail.
 * As respostas não revelam se um e-mail existe: cadastro repetido e pedido de senha respondem sempre o mesmo.
 */
@Slf4j
@Service
public class CadastroServiceImpl implements CadastroService {

    static final int MINUTOS_VALIDADE_LINK = 30;
    static final int MAX_CADASTROS_IP = 5;
    static final int MAX_PEDIDOS_IP = 5;
    static final int MAX_PEDIDOS_EMAIL = 3;
    static final int MAX_REDEFINICOES_IP = 10;
    static final String MSG_CADASTRO = "Cadastro enviado. Assim que o administrador da obra aprovar, você poderá entrar.";
    static final String MSG_ESQUECI = "Se o e-mail estiver cadastrado, enviamos um link para criar uma nova senha. Ele vale 30 minutos.";
    static final String MSG_REDEFINIDA = "Senha alterada. Entre com a nova senha.";
    static final String MSG_LINK_INVALIDO = "Este link é inválido ou expirou. Peça um novo em \"Esqueci a senha\".";

    private final CadastroRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final LimiteTentativasService limite;
    private final EmailService email;
    private final SessaoService sessao;
    private final Clock clock;
    private final String urlApp;
    private final SecureRandom aleatorio = new SecureRandom();

    public CadastroServiceImpl(CadastroRepository repository, UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                               LimiteTentativasService limite, EmailService email, SessaoService sessao, Clock clock,
                               @Value("${nord-tool.app-url:}") String urlApp) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.limite = limite;
        this.email = email;
        this.sessao = sessao;
        this.clock = clock;
        this.urlApp = urlApp == null ? "" : urlApp.trim().replaceAll("/+$", "");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MensagemDto cadastrar(CadastroForm form, String ip) {
        limite.registrar("cadastro-ip:" + ip, MAX_CADASTROS_IP);
        PoliticaSenha.validar(form.getSenha()).ifPresent(motivo -> {
            throw new EntradaInvalidaException("Senha inválida: " + motivo);
        });
        // O código da obra é opcional: se vier, precisa existir; se não vier, o admin define a obra na aprovação.
        String codigo = form.getCdObra() == null ? "" : form.getCdObra().trim();
        Obra obra = codigo.isEmpty() ? null : repository.buscarObraAtivaPorCodigo(codigo)
                .orElseThrow(() -> new EntradaInvalidaException("Código da obra inválido. Confira com o responsável da obra ou deixe em branco."));
        String mail = form.getNmEmail().trim().toLowerCase(Locale.ROOT);
        if (repository.emailExiste(mail)) {
            // Mesma resposta de um cadastro novo: não revela que o e-mail já existe. A pessoa é avisada por e-mail.
            email.enviar(mail, "NordTool — tentativa de cadastro",
                    "Alguém tentou criar uma conta no NordTool com este e-mail, que já está cadastrado.\n"
                            + "Se foi você e esqueceu a senha, use \"Esqueci a senha\" na tela de login.");
            return new MensagemDto(MSG_CADASTRO);
        }
        String celular = form.getNrCelular() == null || form.getNrCelular().isBlank() ? null : form.getNrCelular().trim();
        Long idObra = obra == null ? null : obra.getId();
        repository.inserirPendente(form.getNmNome().trim(), mail, passwordEncoder.encode(form.getSenha()), celular, idObra);
        log.info("Novo cadastro pendente (obra {})", idObra == null ? "a definir" : idObra);
        return new MensagemDto(MSG_CADASTRO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MensagemDto esqueciSenha(EsqueciSenhaForm form, String ip) {
        String mail = form.getNmEmail().trim().toLowerCase(Locale.ROOT);
        limite.registrar("esqueci-ip:" + ip, MAX_PEDIDOS_IP);
        limite.registrar("esqueci-email:" + mail, MAX_PEDIDOS_EMAIL);
        Optional<Usuario> achado = usuarioRepository.buscarPorEmail(mail)
                .filter(u -> Boolean.TRUE.equals(u.getInAtivo()) && !"RECUSADO".equals(u.getCdSituacaoCadastro()));
        if (achado.isEmpty()) return new MensagemDto(MSG_ESQUECI);
        if (urlApp.isEmpty()) {
            log.warn("Pedido de nova senha ignorado: NORD_APP_URL não configurada");
            return new MensagemDto(MSG_ESQUECI);
        }
        Usuario u = achado.get();
        String token = gerarToken();
        repository.invalidarTokens(u.getId());
        repository.inserirToken(u.getId(), sha256(token), LocalDateTime.now(clock).plusMinutes(MINUTOS_VALIDADE_LINK));
        email.enviar(u.getNmEmail(), "NordTool — criar nova senha",
                "Olá, " + u.getNmNome() + ".\n\n"
                        + "Para criar uma nova senha no NordTool, abra o link abaixo (vale " + MINUTOS_VALIDADE_LINK + " minutos e só pode ser usado uma vez):\n\n"
                        + urlApp + "/redefinir-senha?token=" + token + "\n\n"
                        + "Se você não pediu, ignore este e-mail: sua senha continua a mesma.");
        return new MensagemDto(MSG_ESQUECI);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MensagemDto redefinirSenha(RedefinirSenhaForm form, String ip) {
        limite.registrar("redefinir-ip:" + ip, MAX_REDEFINICOES_IP);
        CadastroRepository.TokenRedefinicao t = repository.buscarToken(sha256(form.getToken().trim()))
                .filter(x -> x.uso == null && x.expira.isAfter(LocalDateTime.now(clock)))
                .orElseThrow(() -> new EntradaInvalidaException("LINK_INVALIDO", MSG_LINK_INVALIDO));
        PoliticaSenha.validar(form.getNovaSenha()).ifPresent(motivo -> {
            throw new EntradaInvalidaException("Senha inválida: " + motivo);
        });
        Usuario u = usuarioRepository.buscarPorId(t.idUsuario).filter(x -> Boolean.TRUE.equals(x.getInAtivo()))
                .orElseThrow(() -> new EntradaInvalidaException("LINK_INVALIDO", MSG_LINK_INVALIDO));
        if (!repository.usarToken(t.id)) throw new EntradaInvalidaException("LINK_INVALIDO", MSG_LINK_INVALIDO);
        repository.invalidarTokens(u.getId());
        // Troca o hash e sobe a versão da sessão: quem estava logado com a senha antiga é desconectado.
        usuarioRepository.alterarSenha(u.getId(), passwordEncoder.encode(form.getNovaSenha()));
        sessao.invalidar(u.getId());
        return new MensagemDto(MSG_REDEFINIDA);
    }

    private String gerarToken() {
        byte[] bytes = new byte[32];
        aleatorio.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String sha256(String texto) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : h) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
