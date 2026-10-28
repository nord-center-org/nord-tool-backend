package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.domain.CronogramaSemanal;
import br.com.nord_tool_backend.domain.IntegracaoToken;
import br.com.nord_tool_backend.domain.PerfilPermissao;
import br.com.nord_tool_backend.domain.Usuario;
import br.com.nord_tool_backend.dto.CronogramaSemanalExcecaoDto;
import br.com.nord_tool_backend.dto.IntegracaoTokenDto;
import br.com.nord_tool_backend.dto.RelatorioCronogramaDto;
import br.com.nord_tool_backend.dto.RelatorioCronogramaDto.DiaDto;
import br.com.nord_tool_backend.dto.RelatorioCronogramaDto.ItemDto;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.exception.NaoEncontradoException;
import br.com.nord_tool_backend.form.IntegracaoTokenForm;
import br.com.nord_tool_backend.repository.IntegracaoRepository;
import br.com.nord_tool_backend.repository.UsuarioRepository;
import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.security.PermissaoCalculator;
import br.com.nord_tool_backend.security.UsuarioAutenticado;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.service.IntegracaoService;
import br.com.nord_tool_backend.service.LimiteTentativasService;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class IntegracaoServiceImpl implements IntegracaoService {

    static final String ESCOPO_CRONOGRAMA = "CRONOGRAMA_LEITURA";
    static final String PREFIXO_CHAVE = "ntk_";
    static final int MAX_LINKS = 5;
    static final int MAX_LEITURAS_POR_MINUTO = 30;
    static final int MAX_DIAS = 14;
    static final String FINALIZADA = "finalizada";
    private static final Pattern FORMATO_CHAVE = Pattern.compile("^ntk_[A-Za-z0-9_-]{43}$");
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String[] DIAS = {"segunda-feira", "terça-feira", "quarta-feira", "quinta-feira", "sexta-feira", "sábado", "domingo"};
    private static final Map<String, String> STATUS = Map.of(
            "pendentes", "Pendente", "executar", "Executar", "aguardar", "Aguardando retorno",
            "acompanhar", "Acompanhando", FINALIZADA, "Finalizada");

    private final IntegracaoRepository repository;
    private final UsuarioRepository usuarios;
    private final AutorizacaoService autorizacao;
    private final LimiteTentativasService limite;
    private final Clock clock;
    private final SecureRandom aleatorio = new SecureRandom();

    public IntegracaoServiceImpl(IntegracaoRepository repository, UsuarioRepository usuarios, AutorizacaoService autorizacao,
                                 LimiteTentativasService limite, Clock clock) {
        this.repository = repository;
        this.usuarios = usuarios;
        this.autorizacao = autorizacao;
        this.limite = limite;
        this.clock = clock;
    }

    @Override
    public List<IntegracaoTokenDto> listarTokens() {
        UsuarioAutenticado u = autorizacao.exigir(Modulo.CRONOGRAMA, Acao.LEITURA);
        return repository.listar(u.getId()).stream().map(IntegracaoTokenDto::de).collect(Collectors.toList());
    }

    @Override
    public IntegracaoTokenDto criarToken(IntegracaoTokenForm form) {
        UsuarioAutenticado u = autorizacao.exigir(Modulo.CRONOGRAMA, Acao.LEITURA);
        String rotulo = form.getNmRotulo() == null ? "" : form.getNmRotulo().trim();
        if (rotulo.isEmpty()) throw new EntradaInvalidaException("Dê um nome ao link (ex.: Gemini)");
        if (repository.contarAtivos(u.getId()) >= MAX_LINKS) {
            throw new EntradaInvalidaException("Você já tem " + MAX_LINKS + " links ativos. Revogue um antes de criar outro.");
        }
        byte[] bytes = new byte[32];
        aleatorio.nextBytes(bytes);
        String chave = PREFIXO_CHAVE + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String prefixo = chave.substring(0, 10);
        Long id = repository.criar(u.getId(), ESCOPO_CRONOGRAMA, rotulo, hash(chave), prefixo);
        IntegracaoTokenDto d = new IntegracaoTokenDto();
        d.setIdToken(id);
        d.setCdEscopo(ESCOPO_CRONOGRAMA);
        d.setNmRotulo(rotulo);
        d.setTxPrefixo(prefixo);
        d.setChave(chave);
        return d;
    }

    @Override
    public void revogarToken(Long idToken) {
        UsuarioAutenticado u = autorizacao.exigirAutenticado();
        if (repository.revogar(u.getId(), idToken) == 0) throw new NaoEncontradoException("Link não encontrado");
    }

    @Override
    public RelatorioCronogramaDto relatorioCronograma(String chave, int dias) {
        IntegracaoToken token = validar(chave);
        Usuario dono = usuarios.buscarPorId(token.getIdUsuario()).orElseThrow(IntegracaoServiceImpl::naoEncontrado);
        Map<Modulo, Acao> permissoes = permissoes(dono);
        if (!Boolean.TRUE.equals(dono.getInAtivo()) || !"APROVADO".equals(dono.getCdSituacaoCadastro())
                || !permissoes.getOrDefault(Modulo.CRONOGRAMA, Acao.NENHUM).atende(Acao.LEITURA)) {
            throw naoEncontrado();
        }
        boolean verSemDono = permissoes.getOrDefault(Modulo.CRONOGRAMA, Acao.NENHUM).atende(Acao.ADMIN);
        List<CronogramaSemanal> itens = repository.cronogramaDoUsuario(dono.getId(), verSemDono);
        Set<String> ignorados = new HashSet<>();
        for (CronogramaSemanalExcecaoDto e : repository.excecoesDoUsuario(dono.getId(), verSemDono)) {
            ignorados.add(e.getIdCronogramaSemanal() + "|" + e.getDtExcecao());
        }
        repository.marcarUso(token.getIdToken());
        return montar(primeiroNome(dono.getNmNome()), itens, ignorados, Math.max(1, Math.min(MAX_DIAS, dias)));
    }

    @Override
    public String relatorioCronogramaTexto(String chave, int dias) {
        return texto(relatorioCronograma(chave, dias));
    }

    /** Monta o relatório a partir dos itens: mesma regra da tela (fixos no dia da semana, agendados e prazos na data). */
    RelatorioCronogramaDto montar(String pessoa, List<CronogramaSemanal> itens, Set<String> ignorados, int dias) {
        LocalDateTime agora = LocalDateTime.now(clock.withZone(FUSO));
        LocalDate hoje = agora.toLocalDate();
        List<DiaDto> lista = new ArrayList<>();
        for (int i = 0; i < dias; i++) lista.add(dia(hoje.plusDays(i), itens, ignorados));
        List<ItemDto> atrasadas = itens.stream()
                .filter(c -> !Boolean.TRUE.equals(c.getInCronogramaFixo()) && !finalizada(c)
                        && c.getDtPrazo() != null && c.getDtPrazo().toLocalDate().isBefore(hoje))
                .sorted(Comparator.comparing(CronogramaSemanal::getDtPrazo))
                .map(c -> item(c, "prazo", "")).collect(Collectors.toList());
        List<ItemDto> semData = itens.stream()
                .filter(c -> !Boolean.TRUE.equals(c.getInCronogramaFixo()) && !finalizada(c)
                        && c.getDtPrazo() == null && c.getDtAgendamento() == null)
                .map(c -> item(c, "sem data", "")).collect(Collectors.toList());
        return new RelatorioCronogramaDto(agora.format(DATA_HORA), pessoa, lista.get(0),
                atrasadas, lista.subList(1, lista.size()), semData);
    }

    private DiaDto dia(LocalDate data, List<CronogramaSemanal> itens, Set<String> ignorados) {
        int diaSemana = data.getDayOfWeek().getValue();
        String chave = data.toString();
        List<ItemDto> doDia = new ArrayList<>();
        for (CronogramaSemanal c : itens) {
            if (Boolean.TRUE.equals(c.getInCronogramaFixo())) {
                if (c.getIdDiaSemana() != null && c.getIdDiaSemana() == diaSemana && !ignorados.contains(c.getId() + "|" + chave)) {
                    doDia.add(item(c, "fixo", c.getNmHorario()));
                }
            } else if (c.getDtAgendamento() != null && c.getDtAgendamento().toLocalDate().equals(data)) {
                doDia.add(item(c, "agendado", String.format("%02d:%02d", c.getDtAgendamento().getHour(), c.getDtAgendamento().getMinute())));
            } else if (c.getDtPrazo() != null && c.getDtPrazo().toLocalDate().equals(data)) {
                doDia.add(item(c, "prazo", ""));
            }
        }
        doDia.sort(Comparator.comparing(i -> i.getHorario() == null || i.getHorario().isEmpty() ? "99" : i.getHorario()));
        return new DiaDto(data.format(DATA), DIAS[diaSemana - 1], doDia);
    }

    private static ItemDto item(CronogramaSemanal c, String tipo, String horario) {
        String obs = c.getTxObservacao() == null ? null : c.getTxObservacao().trim();
        if (obs != null && obs.length() > 200) obs = obs.substring(0, 200) + "…";
        return new ItemDto(c.getNmCronogramaSemanal(), tipo, horario == null ? "" : horario.trim(), c.getNmCronogramaCategoria(),
                STATUS.getOrDefault(c.getNmStatusCronograma(), c.getNmStatusCronograma()),
                c.getDtPrazo() == null ? null : c.getDtPrazo().toLocalDate().format(DATA), c.getNmTag(), obs == null || obs.isEmpty() ? null : obs);
    }

    /** Texto simples em português, organizado por seção. */
    static String texto(RelatorioCronogramaDto r) {
        StringBuilder s = new StringBuilder();
        s.append("NordTool · Cronograma de ").append(r.getPessoa()).append('\n');
        s.append("Gerado em ").append(r.getGeradoEm()).append(" (horário de Brasília)\n\n");
        s.append("HOJE — ").append(r.getHoje().getDiaSemana()).append(", ").append(r.getHoje().getData()).append('\n');
        linhas(s, r.getHoje().getItens(), "Nenhuma tarefa para hoje.");
        s.append("\nATRASADAS (prazo vencido e não finalizadas)\n");
        linhas(s, r.getAtrasadas(), "Nenhuma tarefa atrasada.");
        s.append("\nPRÓXIMOS DIAS\n");
        for (DiaDto d : r.getProximosDias()) {
            s.append(d.getDiaSemana()).append(", ").append(d.getData()).append('\n');
            linhas(s, d.getItens(), "Sem tarefas.");
        }
        s.append("\nSEM DATA (pendentes)\n");
        linhas(s, r.getSemData(), "Nenhuma.");
        return s.toString();
    }

    private static void linhas(StringBuilder s, List<ItemDto> itens, String vazio) {
        if (itens.isEmpty()) {
            s.append("- ").append(vazio).append('\n');
            return;
        }
        for (ItemDto i : itens) {
            s.append("- ");
            if (!i.getHorario().isEmpty()) s.append(i.getHorario()).append(" · ");
            if ("fixo".equals(i.getTipo())) s.append("[Rotina] ");
            if ("prazo".equals(i.getTipo()) && i.getPrazo() != null) s.append("[Prazo ").append(i.getPrazo()).append("] ");
            s.append(i.getTitulo());
            List<String> extras = new ArrayList<>();
            if (i.getCategoria() != null && !i.getCategoria().isEmpty()) extras.add(i.getCategoria());
            if (i.getStatus() != null && !"fixo".equals(i.getTipo())) extras.add(i.getStatus());
            if (i.getTag() != null && !i.getTag().isEmpty()) extras.add("#" + i.getTag());
            if (!extras.isEmpty()) s.append(" (").append(String.join(" · ", extras)).append(')');
            if (i.getObservacao() != null) s.append(" — ").append(i.getObservacao().replace('\n', ' '));
            s.append('\n');
        }
    }

    private IntegracaoToken validar(String chave) {
        if (chave == null || !FORMATO_CHAVE.matcher(chave).matches()) throw naoEncontrado();
        String h = hash(chave);
        limite.registrar("integracao:" + h.substring(0, 16), MAX_LEITURAS_POR_MINUTO);
        IntegracaoToken t = repository.buscarAtivoPorHash(h).orElseThrow(IntegracaoServiceImpl::naoEncontrado);
        if (!ESCOPO_CRONOGRAMA.equals(t.getCdEscopo())) throw naoEncontrado();
        return t;
    }

    private Map<Modulo, Acao> permissoes(Usuario u) {
        List<PerfilPermissao> base = new ArrayList<>(usuarios.listarPermissoesCargo(u.getIdCargo()));
        base.addAll(usuarios.listarPermissoes(u.getIdPerfil()));
        return PermissaoCalculator.resolver(base, usuarios.listarPermissoesUsuario(u.getId()));
    }

    private static boolean finalizada(CronogramaSemanal c) {
        return FINALIZADA.equalsIgnoreCase(c.getNmStatusCronograma());
    }

    private static String primeiroNome(String nome) {
        return nome == null || nome.isBlank() ? "você" : nome.trim().split("\\s+")[0];
    }

    static String hash(String chave) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(chave.getBytes(StandardCharsets.UTF_8));
            StringBuilder s = new StringBuilder();
            for (byte b : d) s.append(String.format("%02x", b));
            return s.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static NaoEncontradoException naoEncontrado() {
        return new NaoEncontradoException("Link não encontrado ou revogado");
    }
}
