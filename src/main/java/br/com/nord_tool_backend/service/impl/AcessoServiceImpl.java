package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.domain.Perfil;
import br.com.nord_tool_backend.domain.PerfilPermissao;
import br.com.nord_tool_backend.domain.Usuario;
import br.com.nord_tool_backend.dto.AcessoPerfilDto;
import br.com.nord_tool_backend.dto.AcessoUsuarioDto;
import br.com.nord_tool_backend.dto.AcessosDto;
import br.com.nord_tool_backend.dto.ColaboradorLegadoDto;
import br.com.nord_tool_backend.form.DadosUsuarioForm;
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
    private final br.com.nord_tool_backend.service.EmailService email;

    public AcessoServiceImpl(AcessoRepository repository, AutorizacaoService autorizacao, SessaoService sessao,
                             br.com.nord_tool_backend.service.EmailService email) {
        this.repository = repository;
        this.autorizacao = autorizacao;
        this.sessao = sessao;
        this.email = email;
    }

    @Override
    @Transactional(readOnly = true)
    public AcessosDto listar() {
        autorizacao.exigir(Modulo.ADMINISTRACAO, Acao.ADMIN);
        Map<Long, List<PerfilPermissao>> doPerfil = repository.permissoesPorPerfil();
        Map<Integer, List<PerfilPermissao>> doCargo = repository.permissoesPorCargo();
        Map<Long, List<PerfilPermissao>> doUsuario = repository.excecoesPorUsuario();
        List<AcessoUsuarioDto> usuarios = repository.listarUsuarios().stream()
                .map(u -> dto(u, efetivas(base(doPerfil, doCargo, u.getIdPerfil(), u.getIdCargo()), doUsuario.get(u.getId())),
                        efetivas(base(doPerfil, doCargo, u.getIdPerfil(), u.getIdCargo()), null)))
                .collect(Collectors.toList());
        List<AcessoPerfilDto> perfis = repository.listarPerfis().stream()
                .map(p -> new AcessoPerfilDto(p.getCdPerfil(), p.getNmPerfil(),
                        texto(efetivas(doPerfil.get(p.getIdPerfil()), null))))
                .collect(Collectors.toList());
        List<String> modulos = Stream.of(Modulo.values()).map(Modulo::name).collect(Collectors.toList());
        List<br.com.nord_tool_backend.dto.AcessoCargoDto> cargos = repository.listarCargos().stream()
                .map(c -> new br.com.nord_tool_backend.dto.AcessoCargoDto(c.getId().intValue(), c.getNmCargo(),
                        texto(efetivas(doCargo.getOrDefault(c.getId().intValue(), Collections.emptyList()), null))))
                .collect(Collectors.toList());
        return new AcessosDto(usuarios, perfis, modulos, cargos);
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
        Map<Integer, List<PerfilPermissao>> doCargo = repository.permissoesPorCargo();
        Map<Modulo, Acao> doNovoPerfil = efetivas(base(doPerfil, doCargo, perfil.getIdPerfil(), alvo.getIdCargo()), null);
        List<PerfilPermissao> excecoes = excecoes(form.getAcessos(), doNovoPerfil);
        Map<Modulo, Acao> novas = efetivas(base(doPerfil, doCargo, perfil.getIdPerfil(), alvo.getIdCargo()), excecoes);
        boolean ativo = Boolean.TRUE.equals(form.getInAtivo());

        if (idUsuario.equals(atual.getId())) {
            if (!ativo) throw new NegocioException(CD_ULTIMO_ADMIN, "Você não pode desativar o seu próprio usuário");
            if (!ehAdmin(novas)) throw new NegocioException(CD_ULTIMO_ADMIN, "Você não pode tirar o seu próprio acesso de administrador");
        }
        Map<Long, List<PerfilPermissao>> doUsuario = repository.excecoesPorUsuario();
        boolean sobraAdmin = usuarios.stream().anyMatch(u -> u.getId().equals(idUsuario)
                ? ativo && ehAdmin(novas)
                : Boolean.TRUE.equals(u.getInAtivo()) && ehAdmin(efetivas(base(doPerfil, doCargo, u.getIdPerfil(), u.getIdCargo()), doUsuario.get(u.getId()))));
        if (!sobraAdmin) throw new NegocioException(CD_ULTIMO_ADMIN, "É preciso manter ao menos um administrador ativo");

        Map<Modulo, Acao> anteriores = efetivas(base(doPerfil, doCargo, alvo.getIdPerfil(), alvo.getIdCargo()), doUsuario.get(idUsuario));
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AcessoUsuarioDto salvarDados(Long idUsuario, DadosUsuarioForm form) {
        autorizacao.exigir(Modulo.ADMINISTRACAO, Acao.ADMIN);
        Usuario u = usuario(idUsuario);
        String celular = form.getNrCelular() == null ? "" : form.getNrCelular().trim();
        repository.alterarDados(idUsuario, form.getNmNome().trim(), celular.isEmpty() ? null : celular, form.getIdEmpresa(), form.getIdCargo());
        if (!Objects.equals(u.getIdCargo(), form.getIdCargo())) {
            // O cargo dá acessos: com cargo novo a pessoa entra de novo já com o pacote dele.
            repository.renovarSessao(idUsuario);
            sessao.invalidar(idUsuario);
        }
        Usuario atualizado = usuario(idUsuario);
        Map<Long, List<PerfilPermissao>> doPerfil = repository.permissoesPorPerfil();
        Map<Integer, List<PerfilPermissao>> doCargo = repository.permissoesPorCargo();
        return dto(atualizado, efetivas(base(doPerfil, doCargo, atualizado.getIdPerfil(), atualizado.getIdCargo()), repository.excecoesPorUsuario().get(idUsuario)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ColaboradorLegadoDto> listarColaboradoresLegados() {
        autorizacao.exigir(Modulo.ADMINISTRACAO, Acao.ADMIN);
        return repository.listarColaboradoresLegados();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ColaboradorLegadoDto vincularColaborador(Long idUserLegado, Long idUsuario) {
        autorizacao.exigir(Modulo.ADMINISTRACAO, Acao.ADMIN);
        ColaboradorLegadoDto legado = repository.listarColaboradoresLegados().stream()
                .filter(c -> c.getIdUser().equals(idUserLegado)).findFirst()
                .orElseThrow(() -> new NaoEncontradoException("Colaborador antigo não encontrado"));
        repository.desvincularColaborador(idUserLegado);
        if (idUsuario != null) {
            usuario(idUsuario);
            repository.vincularColaborador(idUserLegado, idUsuario);
        }
        return repository.listarColaboradoresLegados().stream()
                .filter(c -> c.getIdUser().equals(legado.getIdUser())).findFirst().orElse(legado);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AcessoUsuarioDto decidirCadastro(Long idUsuario, br.com.nord_tool_backend.form.DecisaoCadastroForm form) {
        autorizacao.exigir(Modulo.ADMINISTRACAO, Acao.ADMIN);
        Usuario u = usuario(idUsuario);
        if (!"PENDENTE".equals(u.getCdSituacaoCadastro())) {
            throw new NegocioException("CADASTRO_JA_DECIDIDO", "Este cadastro já foi analisado");
        }
        boolean aprovar = Boolean.TRUE.equals(form.getAprovar());
        List<Long> fases = form.getIdsFases() == null ? Collections.emptyList()
                : new ArrayList<>(new java.util.LinkedHashSet<>(form.getIdsFases()));
        if (aprovar && !fases.isEmpty() && repository.contarFases(fases) != fases.size()) {
            throw new EntradaInvalidaException("Há fases inexistentes na lista");
        }
        repository.decidirCadastro(idUsuario, aprovar ? "APROVADO" : "RECUSADO", aprovar);
        if (aprovar) repository.adicionarFases(idUsuario, fases);
        sessao.invalidar(idUsuario);
        if (aprovar) {
            email.enviar(u.getNmEmail(), "NordTool — cadastro aprovado",
                    "Olá, " + u.getNmNome() + ".\n\nSeu cadastro no NordTool foi aprovado. Já pode entrar com o seu e-mail e senha.");
        }
        Usuario atualizado = usuario(idUsuario);
        Map<Long, List<PerfilPermissao>> doPerfil = repository.permissoesPorPerfil();
        Map<Integer, List<PerfilPermissao>> doCargo = repository.permissoesPorCargo();
        return dto(atualizado, efetivas(base(doPerfil, doCargo, atualizado.getIdPerfil(), atualizado.getIdCargo()), repository.excecoesPorUsuario().get(idUsuario)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public br.com.nord_tool_backend.dto.AcessoCargoDto salvarAcessosCargo(Integer idCargo, Map<String, String> acessos) {
        autorizacao.exigir(Modulo.ADMINISTRACAO, Acao.ADMIN);
        br.com.nord_tool_backend.domain.Cargo cargo = repository.listarCargos().stream().filter(c -> c.getId() != null && c.getId().intValue() == idCargo).findFirst()
                .orElseThrow(() -> new NaoEncontradoException("Cargo não encontrado"));
        List<PerfilPermissao> lista = new ArrayList<>();
        for (Map.Entry<String, String> e : acessos.entrySet()) {
            Modulo modulo = Modulo.de(e.getKey()).orElseThrow(() -> new EntradaInvalidaException("Módulo inválido: " + e.getKey()));
            Acao acao = acaoValida(e.getValue());
            if (acao != Acao.NENHUM) lista.add(new PerfilPermissao(modulo.name(), acao.name()));
        }
        lista.sort((a, b) -> a.getCdModulo().compareTo(b.getCdModulo()));
        repository.substituirAcessosCargo(idCargo, lista);
        // Quem tem o cargo entra de novo já com o pacote novo.
        for (Long id : repository.renovarSessoesDoCargo(idCargo)) sessao.invalidar(id);
        return new br.com.nord_tool_backend.dto.AcessoCargoDto(idCargo, cargo.getNmCargo(), texto(efetivas(lista, null)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AcessoUsuarioDto definirSuperior(Long idUsuario, Long idSuperior) {
        autorizacao.exigir(Modulo.ADMINISTRACAO, Acao.ADMIN);
        List<Usuario> todos = repository.listarUsuarios();
        Usuario u = todos.stream().filter(x -> x.getId().equals(idUsuario)).findFirst()
                .orElseThrow(() -> new NaoEncontradoException("Usuário não encontrado"));
        if (idSuperior != null) {
            if (idSuperior.equals(idUsuario)) throw new EntradaInvalidaException("A pessoa não pode ser superior dela mesma");
            Map<Long, Long> superiorDe = new java.util.HashMap<>();
            todos.forEach(x -> superiorDe.put(x.getId(), x.getIdSuperior()));
            if (!superiorDe.containsKey(idSuperior)) throw new NaoEncontradoException("Superior não encontrado");
            // Subindo a partir do novo superior não pode chegar de volta nesta pessoa (evita ciclo).
            Long atual = idSuperior;
            for (int passos = 0; atual != null && passos <= todos.size(); passos++) {
                if (atual.equals(idUsuario)) throw new EntradaInvalidaException("Isso criaria um ciclo: o superior escolhido está abaixo desta pessoa");
                atual = superiorDe.get(atual);
            }
        }
        repository.definirSuperior(idUsuario, idSuperior);
        u.setIdSuperior(idSuperior);
        return dto(u, Collections.emptyMap());
    }

    private Usuario usuario(Long id) {
        return repository.listarUsuarios().stream().filter(x -> x.getId().equals(id)).findFirst()
                .orElseThrow(() -> new NaoEncontradoException("Usuário não encontrado"));
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

    /**
     * Padrão de um usuário: o que o cargo dá (por módulo) vem antes do que o perfil dá, então o cargo prevalece
     * sobre o perfil no mesmo módulo e as exceções do usuário prevalecem sobre os dois.
     */
    static List<PerfilPermissao> base(Map<Long, List<PerfilPermissao>> doPerfil, Map<Integer, List<PerfilPermissao>> doCargo,
                                      Long idPerfil, Integer idCargo) {
        List<PerfilPermissao> lista = new ArrayList<>();
        if (idCargo != null) lista.addAll(doCargo.getOrDefault(idCargo, Collections.emptyList()));
        lista.addAll(doPerfil.getOrDefault(idPerfil, Collections.emptyList()));
        return lista;
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
        return dto(u, acessos, null);
    }

    private static AcessoUsuarioDto dto(Usuario u, Map<Modulo, Acao> acessos, Map<Modulo, Acao> padrao) {
        AcessoUsuarioDto d = new AcessoUsuarioDto(u.getId(), u.getNmNome(), u.getNmEmail(), u.getCdPerfil(),
                Boolean.TRUE.equals(u.getInAtivo()), u.getDhUltimoLogin(), u.getNrCelular(), u.getIdEmpresa(),
                u.getNmEmpresa(), u.getIdCargo(), u.getNmCargo(),
                u.getCdSituacaoCadastro() == null ? "APROVADO" : u.getCdSituacaoCadastro(),
                u.getIdObraSolicitada(), u.getNmObraSolicitada(), texto(acessos));
        d.setIdSuperior(u.getIdSuperior());
        d.setAcessosPadrao(padrao == null ? null : texto(padrao));
        return d;
    }
}
