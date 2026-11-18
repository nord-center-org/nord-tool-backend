package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.dto.DemandaAvisoDto;
import br.com.nord_tool_backend.dto.NotificacaoCaixaDto;
import br.com.nord_tool_backend.dto.NotificacaoConfigDto;
import br.com.nord_tool_backend.dto.NotificacaoPreferenciaDto;
import br.com.nord_tool_backend.dto.PushInscricaoDto;
import br.com.nord_tool_backend.form.NotificacaoPreferenciaForm;
import br.com.nord_tool_backend.form.PushInscricaoForm;
import br.com.nord_tool_backend.repository.NotificacaoRepository;
import br.com.nord_tool_backend.security.UsuarioAutenticado;
import br.com.nord_tool_backend.service.AcessoService;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.service.NotificacaoService;
import br.com.nord_tool_backend.service.push.PushEnviador;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Optional;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Notificações no celular (Web Push). Cada pessoa ativa em cada aparelho e escolhe o que recebe: demanda nova,
 * demanda alterada (inclui mudar de etapa e compartilhar), o horário agendado chegando (com antecedência), o prazo
 * do dia e a resposta no SAC. Por padrão recebe também o que ela mesma fez (para ver no celular o que lançou no
 * computador). Os lembretes de horário e prazo não se repetem (notificacao_enviada).
 */
@Log4j2
@Service
public class NotificacaoServiceImpl implements NotificacaoService {

    static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    static final int HORA_PRAZO = 8;
    static final String TELA_CRONOGRAMA = "/organizacional/cronograma";
    static final String TELA_NOTIFICACOES = "/configuracoes#notificacoes";
    /** Alterações seguidas da mesma pessoa na mesma área, dentro deste tempo, viram um aviso só. */
    static final int MINUTOS_AGRUPAR = 10;
    static final int LIMITE_CAIXA = 50;
    static final long CACHE_ADMINS_MS = 60_000;
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DIA_HORA = DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm");
    private static final Map<String, String> ETAPA = Map.of("pendentes", "Pendentes", "executar", "Executar", "aguardar", "Aguardar",
            "acompanhar", "Acompanhar", "finalizada", "Finalizada");

    private final NotificacaoRepository repository;
    private final AutorizacaoService autorizacao;
    private final PushEnviador enviador;
    private final ObjectMapper mapper;
    private final Clock relogio;
    private final AcessoService acessos;
    /** Administradores lidos há pouco (a lista muda raramente e cada alteração no sistema consultaria). */
    private volatile Set<Long> administradores = Collections.emptySet();
    private volatile long administradoresLidosEm = Long.MIN_VALUE;

    @Autowired
    public NotificacaoServiceImpl(NotificacaoRepository repository, AutorizacaoService autorizacao, PushEnviador enviador, ObjectMapper mapper,
                                  AcessoService acessos) {
        this(repository, autorizacao, enviador, mapper, acessos, Clock.system(FUSO));
    }

    NotificacaoServiceImpl(NotificacaoRepository repository, AutorizacaoService autorizacao, PushEnviador enviador, ObjectMapper mapper,
                           AcessoService acessos, Clock relogio) {
        this.repository = repository;
        this.autorizacao = autorizacao;
        this.enviador = enviador;
        this.mapper = mapper;
        this.acessos = acessos;
        this.relogio = relogio;
    }

    // ---------------------------------------------------------------- aparelho e preferências

    @Override
    public NotificacaoConfigDto config() {
        autorizacao.exigirAutenticado();
        return new NotificacaoConfigDto(true, enviador.chaves().publica);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void inscrever(PushInscricaoForm form) {
        UsuarioAutenticado u = autorizacao.exigirAutenticado();
        String aparelho = form.getAparelho() == null || form.getAparelho().isBlank() ? null : form.getAparelho().trim();
        repository.inscrever(u.getId(), form.getEndpoint().trim(), form.getP256dh().trim(), form.getAuth().trim(), aparelho);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelar(String endpoint) {
        UsuarioAutenticado u = autorizacao.exigirAutenticado();
        repository.cancelar(u.getId(), endpoint.trim());
    }

    @Override
    public List<PushInscricaoDto> meusAparelhos() {
        UsuarioAutenticado u = autorizacao.exigirAutenticado();
        return repository.inscricoes(List.of(u.getId()));
    }

    @Override
    public NotificacaoPreferenciaDto preferencias() {
        UsuarioAutenticado u = autorizacao.exigirAutenticado();
        return repository.preferencia(u.getId()).orElseGet(() -> {
            NotificacaoPreferenciaDto p = new NotificacaoPreferenciaDto();
            p.setIdUsuario(u.getId());
            return p;
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NotificacaoPreferenciaDto salvarPreferencias(NotificacaoPreferenciaForm f) {
        UsuarioAutenticado u = autorizacao.exigirAutenticado();
        NotificacaoPreferenciaDto p = new NotificacaoPreferenciaDto();
        p.setIdUsuario(u.getId());
        p.setInDemandaNova(f.isInDemandaNova());
        p.setInDemandaAlterada(f.isInDemandaAlterada());
        p.setInHorario(f.isInHorario());
        p.setNrAntecedenciaMin(f.getNrAntecedenciaMin());
        p.setInPrazo(f.isInPrazo());
        p.setInSac(f.isInSac());
        p.setInMinhasAcoes(f.isInMinhasAcoes());
        p.setInAtividade(f.getInAtividade() == null || f.getInAtividade());
        repository.salvarPreferencia(p);
        return p;
    }

    @Override
    public int testar() {
        UsuarioAutenticado u = autorizacao.exigirAutenticado();
        List<PushInscricaoDto> aparelhos = repository.inscricoes(List.of(u.getId()));
        String texto = "Pronto! As notificações estão chegando neste aparelho.";
        repository.inserirCaixa(u.getId(), "teste", null, "NordTool", texto, TELA_NOTIFICACOES);
        enviador.enviar(aparelhos, conteudo("NordTool", texto, TELA_NOTIFICACOES, "teste"));
        return aparelhos.size();
    }

    // ---------------------------------------------------------------- eventos do cronograma e do SAC

    @Override
    public void demandaCriada(Long idDemanda, UsuarioAutenticado ator) {
        emSegundoPlano(() -> repository.demanda(idDemanda).ifPresent(d -> {
            String nome = repository.nomeDoUsuario(ator.getId());
            avisar(envolvidos(d), ator, NotificacaoPreferenciaDto::isInDemandaNova,
                    new Aviso("demanda", "Nova demanda", primeiroNome(nome) + " criou: " + d.getNmCronogramaSemanal() + quando(d), TELA_CRONOGRAMA, "demanda-" + d.getId()));
        }));
    }

    @Override
    public void demandaAlterada(Long idDemanda, UsuarioAutenticado ator, String oQue) {
        emSegundoPlano(() -> repository.demanda(idDemanda).ifPresent(d -> {
            String nome = primeiroNome(repository.nomeDoUsuario(ator.getId()));
            String texto = oQue != null && oQue.startsWith("etapa:")
                    ? nome + " moveu “" + d.getNmCronogramaSemanal() + "” para " + ETAPA.getOrDefault(oQue.substring(6), oQue.substring(6))
                    : nome + " alterou: " + d.getNmCronogramaSemanal() + quando(d);
            avisar(envolvidos(d), ator, NotificacaoPreferenciaDto::isInDemandaAlterada,
                    new Aviso("demanda", "Demanda alterada", texto, TELA_CRONOGRAMA, "demanda-" + d.getId()));
        }));
    }

    @Override
    public void demandaCompartilhada(Long idDemanda, UsuarioAutenticado ator, Collection<Long> novos) {
        if (novos == null || novos.isEmpty()) return;
        emSegundoPlano(() -> repository.demanda(idDemanda).ifPresent(d -> {
            String nome = primeiroNome(repository.nomeDoUsuario(ator.getId()));
            avisar(new LinkedHashSet<>(novos), ator, NotificacaoPreferenciaDto::isInDemandaNova,
                    new Aviso("demanda", "Demanda compartilhada com você", nome + " compartilhou: " + d.getNmCronogramaSemanal() + quando(d), TELA_CRONOGRAMA, "demanda-" + d.getId()));
        }));
    }

    @Override
    public void sacRespondido(Long idDono, Long idChamado, String titulo, String status, UsuarioAutenticado ator) {
        emSegundoPlano(() -> {
            String nome = primeiroNome(repository.nomeDoUsuario(ator.getId()));
            avisar(Set.of(idDono), ator, NotificacaoPreferenciaDto::isInSac,
                    new Aviso("sac", "Seu chamado foi respondido", nome + " · " + rotuloSac(status) + " · " + titulo, "/sac?chamado=" + idChamado, "sac-" + idChamado));
        });
    }

    // ---------------------------------------------------------------- caixa (app Android) e alterações

    @Override
    public NotificacaoCaixaDto caixa(Long depois) {
        UsuarioAutenticado u = autorizacao.exigirAutenticado();
        long ultimo = repository.ultimoDaCaixa(u.getId());
        if (depois == null || depois < 0) return new NotificacaoCaixaDto(ultimo, Collections.emptyList());
        // Caixa mais nova que o app (banco recriado): recomeça do fim sem repetir avisos.
        if (depois > ultimo) return new NotificacaoCaixaDto(ultimo, Collections.emptyList());
        return new NotificacaoCaixaDto(ultimo, repository.caixa(u.getId(), depois, LIMITE_CAIXA));
    }

    @Override
    public void atividade(UsuarioAutenticado autor, String metodo, String caminho) {
        if (autor == null || autor.getId() == null || metodo == null || caminho == null) return;
        Optional<String> area = areaDoCaminho(caminho);
        if (area.isEmpty()) return;
        String verbo = verbo(metodo);
        if (verbo == null) return;
        emSegundoPlano(() -> avisarAtividade(autor, metodo.toUpperCase(Locale.ROOT), caminho, area.get(), verbo));
    }

    private void avisarAtividade(UsuarioAutenticado autor, String metodo, String caminho, String area, String verbo) {
        repository.registrarAtividade(autor.getId(), metodo, corte(caminho, 300), area);
        Set<Long> admins = new LinkedHashSet<>(administradores());
        admins.remove(autor.getId());
        if (admins.isEmpty()) return;
        List<Long> destino = repository.preferencias(admins).stream()
                .filter(NotificacaoPreferenciaDto::isInAtividade)
                .map(NotificacaoPreferenciaDto::getIdUsuario)
                .collect(Collectors.toList());
        if (destino.isEmpty()) return;
        String nome = repository.nomeDoUsuario(autor.getId());
        String quem = nome == null || nome.isBlank() ? "Alguém" : nome.trim();
        String titulo = corte(quem + " · " + area, 120);
        String agrupar = "atividade:" + autor.getId() + ":" + area;
        String tag = "atividade-" + autor.getId() + "-" + area.hashCode();
        LocalDateTime desde = LocalDateTime.now(relogio).minusMinutes(MINUTOS_AGRUPAR);
        String uma = verbo + " um registro";
        String varias = "{n} alterações seguidas · a última: " + verbo.toLowerCase(Locale.ROOT);
        for (Long id : destino) {
            seguro(() -> {
                Optional<Integer> juntou = repository.agruparCaixa(id, agrupar, desde, titulo, varias, null);
                if (juntou.isEmpty()) repository.inserirCaixa(id, "atividade", agrupar, titulo, uma, null);
                String corpo = juntou.map(n -> varias.replace("{n}", String.valueOf(n))).orElse(uma);
                enviador.enviar(repository.inscricoes(List.of(id)), conteudo(titulo, corpo, "/", tag));
            });
        }
    }

    private Set<Long> administradores() {
        long agora = relogio.millis();
        if (administradoresLidosEm == Long.MIN_VALUE || agora - administradoresLidosEm > CACHE_ADMINS_MS) {
            administradores = acessos.administradoresAtivos();
            administradoresLidosEm = agora;
        }
        return administradores;
    }

    static String verbo(String metodo) {
        switch (metodo == null ? "" : metodo.toUpperCase(Locale.ROOT)) {
            case "POST": return "Criou";
            case "PUT":
            case "PATCH": return "Alterou";
            case "DELETE": return "Excluiu";
            default: return null;
        }
    }

    private static final String API = "/api/v1/nord-tool/";
    /** Do mais específico ao mais geral; área vazia = não é alteração que interessa (login, avisos, IA, preferências de tela). */
    private static final String[][] AREAS = {
            {"auth", ""}, {"notificacoes", ""}, {"app", ""}, {"menu-favoritos", ""}, {"lugia", ""}, {"cache", ""}, {"davi-job", ""},
            {"cronogramaSemanal/interpretar", ""}, {"sac/chamados/vistos", ""}, {"repertorio/cifras-recebidas", ""},
            {"administracao/acessos", "Acessos e usuários"}, {"administracao", "Obras e fases"},
            {"apartamentoVistoria/termos-reprova", "Termo de reprova"}, {"apartamentoVistoria", "Vistorias"},
            {"avatares", "Avatar"}, {"blocos-notas", "Bloco de notas"}, {"caixinha", "Caixinha"}, {"cargos", "Cargos"},
            {"casamento", "Casamento"}, {"colaboradores", "Colaboradores"}, {"controleChaves", "Controle de chaves"},
            {"cronogramaSemanal/equipes", "Equipes do cronograma"}, {"cronogramaSemanal", "Cronograma"}, {"cubicagens", "Cubicagem"},
            {"empresas", "Empresas"}, {"escadinha", "Escadinha"}, {"ferramentas", "Ferramentas"}, {"financeiro", "Financeiro"},
            {"integracoes", "Integrações"}, {"mapeamentos", "Conferências"}, {"medicoes", "Medições"}, {"conta/dados", "Meus dados"},
            {"permissoes", "Permissões"}, {"placas", "Placas"}, {"relatorios-fotograficos", "Relatório fotográfico"},
            {"repertorio", "Repertório"}, {"sac", "SAC"}, {"treinos", "Treinos"},
    };

    static Optional<String> areaDoCaminho(String caminho) {
        if (caminho == null || !caminho.startsWith(API)) return Optional.empty();
        String resto = caminho.substring(API.length());
        for (String[] a : AREAS) {
            if (resto.equals(a[0]) || resto.startsWith(a[0] + "/")) return a[1].isEmpty() ? Optional.empty() : Optional.of(a[1]);
        }
        String primeiro = resto.split("/", 2)[0];
        return primeiro.isEmpty() ? Optional.empty() : Optional.of(primeiro);
    }

    // ---------------------------------------------------------------- lembretes

    @Override
    public void lembretes() {
        LocalDateTime agora = LocalDateTime.now(relogio).withSecond(0).withNano(0);
        // Horário agendado: até 2 h à frente (a maior antecedência) e até 5 min depois (servidor que reiniciou).
        for (DemandaAvisoDto d : repository.demandasAgendadas(agora.minusMinutes(5), agora.plusMinutes(121))) {
            seguro(() -> lembrarHorario(d, agora));
        }
        if (agora.getHour() >= HORA_PRAZO) {
            LocalDate hoje = agora.toLocalDate();
            for (DemandaAvisoDto d : repository.demandasComPrazo(hoje)) {
                seguro(() -> lembrarPrazo(d, hoje));
            }
        }
        if (agora.getMinute() == 0) {
            seguro(() -> repository.limparEnviadas(agora.minusDays(30)));
            seguro(() -> repository.limparCaixa(agora.minusDays(30)));
            seguro(() -> repository.limparAtividades(agora.minusDays(90)));
        }
    }

    private void lembrarHorario(DemandaAvisoDto d, LocalDateTime agora) {
        LocalDateTime hora = d.getDtAgendamento().withSecond(0).withNano(0);
        Set<Long> pessoas = envolvidos(d);
        for (NotificacaoPreferenciaDto p : repository.preferencias(pessoas)) {
            if (!p.isInHorario() || agora.isBefore(hora.minusMinutes(p.getNrAntecedenciaMin()))) continue;
            if (!repository.registrarEnvio(p.getIdUsuario(), "horario:" + d.getId() + ":" + hora)) continue;
            long faltam = java.time.Duration.between(agora, hora).toMinutes();
            String titulo = faltam <= 0 ? "Agora: " + d.getNmCronogramaSemanal() : "Daqui a " + faltam + " min: " + d.getNmCronogramaSemanal();
            entregar(List.of(p.getIdUsuario()), new Aviso("horario", titulo, "Agendado para " + hora.format(HORA), TELA_CRONOGRAMA, "horario-" + d.getId()));
        }
    }

    private void lembrarPrazo(DemandaAvisoDto d, LocalDate hoje) {
        for (NotificacaoPreferenciaDto p : repository.preferencias(envolvidos(d))) {
            if (!p.isInPrazo() || !repository.registrarEnvio(p.getIdUsuario(), "prazo:" + d.getId() + ":" + hoje)) continue;
            entregar(List.of(p.getIdUsuario()), new Aviso("prazo", "Prazo hoje", d.getNmCronogramaSemanal() + " vence hoje", TELA_CRONOGRAMA, "prazo-" + d.getId()));
        }
    }

    // ---------------------------------------------------------------- apoio

    /** Manda para quem tem aparelho e quer este tipo de aviso; quem fez a ação só se pediu "o que eu mesmo fiz". */
    void avisar(Set<Long> pessoas, UsuarioAutenticado ator, Predicate<NotificacaoPreferenciaDto> quer, Aviso aviso) {
        if (pessoas.isEmpty()) return;
        List<Long> destino = repository.preferencias(pessoas).stream()
                .filter(quer)
                .filter(p -> !Objects.equals(p.getIdUsuario(), ator.getId()) || p.isInMinhasAcoes())
                .map(NotificacaoPreferenciaDto::getIdUsuario)
                .collect(Collectors.toList());
        entregar(destino, aviso);
    }

    /** Um aviso: vai para a caixa de cada pessoa (app Android) e para os aparelhos com Web Push. */
    static final class Aviso {
        final String tipo;
        final String titulo;
        final String corpo;
        final String url;
        final String tag;

        Aviso(String tipo, String titulo, String corpo, String url, String tag) {
            this.tipo = tipo;
            this.titulo = corte(titulo, 120);
            this.corpo = corte(corpo, 300);
            this.url = url;
            this.tag = tag;
        }
    }

    void entregar(List<Long> destino, Aviso aviso) {
        if (destino.isEmpty()) return;
        for (Long id : destino) {
            seguro(() -> repository.inserirCaixa(id, aviso.tipo, null, aviso.titulo, aviso.corpo, aviso.url));
        }
        enviador.enviar(repository.inscricoes(destino), conteudo(aviso.titulo, aviso.corpo, aviso.url, aviso.tag));
    }

    static Set<Long> envolvidos(DemandaAvisoDto d) {
        Set<Long> ids = new LinkedHashSet<>();
        if (d.getTxEnvolvidos() != null) {
            Arrays.stream(d.getTxEnvolvidos().split(",")).map(String::trim).filter(s -> s.matches("\\d+")).map(Long::valueOf).forEach(ids::add);
        }
        return ids;
    }

    private static String quando(DemandaAvisoDto d) {
        return d.getDtAgendamento() == null ? "" : " · " + d.getDtAgendamento().format(DIA_HORA);
    }

    private static String primeiroNome(String nome) {
        return nome == null || nome.isBlank() ? "Alguém" : nome.trim().split("\\s+")[0];
    }

    private static String rotuloSac(String status) {
        switch (status == null ? "" : status) {
            case "EM_ANDAMENTO": return "Em andamento";
            case "RESOLVIDO": return "Resolvido";
            case "RECUSADO": return "Recusado";
            default: return "Aberto";
        }
    }

    String conteudo(String titulo, String corpo, String url, String tag) {
        ObjectNode n = mapper.createObjectNode();
        n.put("title", corte(titulo, 80));
        n.put("body", corte(corpo, 220));
        n.put("url", url);
        n.put("tag", tag);
        return n.toString();
    }

    private static String corte(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    /** Evento de uma tela: roda depois do commit, fora da transação de quem chamou, e nunca lança. */
    private void emSegundoPlano(Runnable r) {
        try {
            enviador.emSegundoPlano(() -> seguro(r));
        } catch (RuntimeException e) {
            log.warn("Notificações: aviso não agendado: {}", e.getMessage());
        }
    }

    private static void seguro(Runnable r) {
        try {
            r.run();
        } catch (RuntimeException e) {
            log.warn("Notificações: aviso não enviado: {}", e.getMessage());
        }
    }
}
