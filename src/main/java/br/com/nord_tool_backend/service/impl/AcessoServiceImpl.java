package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.domain.Perfil;
import br.com.nord_tool_backend.domain.PerfilPermissao;
import br.com.nord_tool_backend.domain.Usuario;
import br.com.nord_tool_backend.dto.AcessoPerfilDto;
import br.com.nord_tool_backend.dto.AcessoUsuarioDto;
import br.com.nord_tool_backend.dto.AcessosDto;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.exception.NaoEncontradoException;
import br.com.nord_tool_backend.exception.NegocioException;
import br.com.nord_tool_backend.form.AcessoUsuarioForm;
import br.com.nord_tool_backend.repository.AcessoRepository;
import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.security.PermissaoCalculator;
import br.com.nord_tool_backend.security.UsuarioAutenticado;
import br.com.nord_tool_backend.service.AcessoService;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.service.SessaoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Tela de Acessos: o admin escolhe o perfil, a situação e o nível de cada módulo de um usuário.
 * Só o que difere do perfil vira exceção em usuario_permissao. Nunca deixa o sistema sem um admin ativo.
 */
@Service
public class AcessoServiceImpl implements AcessoService {

    static final String CD_ULTIMO_ADMIN = "ULTIMO_ADMIN";

    private final AcessoRepository repository;
    private final AutorizacaoService autorizacao;
    private final SessaoService sessao;

    public AcessoServiceImpl(AcessoRepository repository, AutorizacaoService autorizacao, SessaoService sessao) {
        this.repository = repository;
        this.autorizacao = autorizacao;
        this.sessao = sessao;
    }

    @Override
    @Transactional(readOnly = true)
    public AcessosDto listar() {
        autorizacao.exigir(Modulo.ADMINISTRACAO, Acao.ADMIN);
        Map<Long, List<PerfilPermissao>> doPerfil = repository.permissoesPorPerfil();
        Map<Long, List<PerfilPermissao>> doUsuario = repository.excecoesPorUsuario();
        List<AcessoUsuarioDto> usuarios = repository.listarUsuarios().stream()
                .map(u -> dto(u, efetivas(doPerfil.get(u.getIdPerfil()), doUsuario.get(u.getId()))))
                .collect(Collectors.toList());
        List<AcessoPerfilDto> perfis = repository.listarPerfis().stream()
                .map(p -> new AcessoPerfilDto(p.getCdPerfil(), p.getNmPerfil(),
                        texto(efetivas(doPerfil.get(p.getIdPerfil()), null))))
                .collect(Collectors.toList());
        List<String> modulos = Stream.of(Modulo.values()).map(Modulo::name).collect(Collectors.toList());
        return new AcessosDto(usuarios, perfis, modulos);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AcessoUsuarioDto salvar(Long idUsuario, AcessoUsuarioForm form) {
        UsuarioAutenticado atual = autorizacao.exigir(Modulo.ADMINISTRACAO, Acao.ADMIN);
        List<Usuario> usuarios = repository.listarUsuarios();
        Usuario alvo = usuarios.stream().filter(u -> u.getId().equals(idUsuario)).findFirst()
                .orElseThrow(() -> new NaoEncontradoException("Usuário não encontrado"));
        Perfil perfil = repository.listarPerfis().stream()
                .filter(p -> p.getCdPerfil().equalsIgnoreCase(form.getCdPerfil().trim())).findFirst()
                .orElseThrow(() -> new EntradaInvalidaException("Perfil inválido"));

        Map<Long, List<PerfilPermissao>> doPerfil = repository.permissoesPorPerfil();
        Map<Modulo, Acao> doNovoPerfil = efetivas(doPerfil.get(perfil.getIdPerfil()), null);
        List<PerfilPermissao> excecoes = excecoes(form.getAcessos(), doNovoPerfil);
        Map<Modulo, Acao> novas = efetivas(doPerfil.get(perfil.getIdPerfil()), excecoes);
        boolean ativo = Boolean.TRUE.equals(form.getInAtivo());

        if (idUsuario.equals(atual.getId())) {
            if (!ativo) throw new NegocioException(CD_ULTIMO_ADMIN, "Você não pode desativar o seu próprio usuário");
            if (!ehAdmin(novas)) throw new NegocioException(CD_ULTIMO_ADMIN, "Você não pode tirar o seu próprio acesso de administrador");
        }
        Map<Long, List<PerfilPermissao>> doUsuario = repository.excecoesPorUsuario();
        boolean sobraAdmin = usuarios.stream().anyMatch(u -> u.getId().equals(idUsuario)
                ? ativo && ehAdmin(novas)
                : Boolean.TRUE.equals(u.getInAtivo()) && ehAdmin(efetivas(doPerfil.get(u.getIdPerfil()), doUsuario.get(u.getId()))));
        if (!sobraAdmin) throw new NegocioException(CD_ULTIMO_ADMIN, "É preciso manter ao menos um administrador ativo");

        Map<Modulo, Acao> anteriores = efetivas(doPerfil.get(alvo.getIdPerfil()), doUsuario.get(idUsuario));
        boolean mudou = !Objects.equals(alvo.getIdPerfil(), perfil.getIdPerfil())
                || Boolean.TRUE.equals(alvo.getInAtivo()) != ativo
                || !anteriores.equals(novas)
                || !mesmasExcecoes(doUsuario.get(idUsuario), excecoes);
        if (mudou) {
            repository.alterarPerfilESituacao(idUsuario, perfil.getIdPerfil(), ativo);
            repository.substituirExcecoes(idUsuario, excecoes);
            sessao.invalidar(idUsuario);
        }
        alvo.setIdPerfil(perfil.getIdPerfil());
        alvo.setCdPerfil(perfil.getCdPerfil());
        alvo.setInAtivo(ativo);
        return dto(alvo, novas);
    }

    // ---------- apoio ----------

    /** Só o que difere do perfil vira exceção. Módulo ausente no formulário mantém o nível do perfil. */
    private static List<PerfilPermissao> excecoes(Map<String, String> desejado, Map<Modulo, Acao> doPerfil) {
        List<PerfilPermissao> lista = new ArrayList<>();
        for (Map.Entry<String, String> e : desejado.entrySet()) {
            Modulo modulo = Modulo.de(e.getKey())
                    .orElseThrow(() -> new EntradaInvalidaException("Módulo inválido: " + e.getKey()));
            Acao acao = acaoValida(e.getValue());
            if (acao != doPerfil.getOrDefault(modulo, Acao.NENHUM)) {
                lista.add(new PerfilPermissao(modulo.name(), acao.name()));
            }
        }
        lista.sort((a, b) -> a.getCdModulo().compareTo(b.getCdModulo()));
        return lista;
    }

    private static Acao acaoValida(String codigo) {
        String chave = codigo == null ? "" : codigo.trim();
        return Stream.of(Acao.values()).filter(a -> a.name().equalsIgnoreCase(chave)).findFirst()
                .orElseThrow(() -> new EntradaInvalidaException("Nível de acesso inválido: " + codigo));
    }

    private static boolean mesmasExcecoes(List<PerfilPermissao> atuais, List<PerfilPermissao> novas) {
        List<PerfilPermissao> a = new ArrayList<>(Optional.ofNullable(atuais).orElse(Collections.emptyList()));
        a.sort((x, y) -> x.getCdModulo().compareTo(y.getCdModulo()));
        return a.equals(novas);
    }

    private static Map<Modulo, Acao> efetivas(List<PerfilPermissao> doPerfil, List<PerfilPermissao> doUsuario) {
        return PermissaoCalculator.resolver(doPerfil, doUsuario);
    }

    private static boolean ehAdmin(Map<Modulo, Acao> acessos) {
        return acessos.getOrDefault(Modulo.ADMINISTRACAO, Acao.NENHUM).atende(Acao.ADMIN);
    }

    private static Map<String, String> texto(Map<Modulo, Acao> acessos) {
        Map<String, String> m = new LinkedHashMap<>();
        acessos.forEach((k, v) -> m.put(k.name(), v.name()));
        return m;
    }

    private static AcessoUsuarioDto dto(Usuario u, Map<Modulo, Acao> acessos) {
        return new AcessoUsuarioDto(u.getId(), u.getNmNome(), u.getNmEmail(), u.getCdPerfil(),
                Boolean.TRUE.equals(u.getInAtivo()), u.getDhUltimoLogin(), texto(acessos));
    }
}
