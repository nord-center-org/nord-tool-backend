package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.dto.DemandaAvisoDto;
import br.com.nord_tool_backend.dto.NotificacaoConfigDto;
import br.com.nord_tool_backend.dto.NotificacaoPreferenciaDto;
import br.com.nord_tool_backend.dto.PushInscricaoDto;
import br.com.nord_tool_backend.form.NotificacaoPreferenciaForm;
import br.com.nord_tool_backend.form.PushInscricaoForm;
import br.com.nord_tool_backend.repository.NotificacaoRepository;
import br.com.nord_tool_backend.security.UsuarioAutenticado;
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
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DIA_HORA = DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm");
    private static final Map<String, String> ETAPA = Map.of("pendentes", "Pendentes", "executar", "Executar", "aguardar", "Aguardar",
            "acompanhar", "Acompanhar", "finalizada", "Finalizada");

    private final NotificacaoRepository repository;
    private final AutorizacaoService autorizacao;
    private final PushEnviador enviador;
    private final ObjectMapper mapper;
    private final Clock relogio;

    @Autowired
    public NotificacaoServiceImpl(NotificacaoRepository repository, AutorizacaoService autorizacao, PushEnviador enviador, ObjectMapper mapper) {
        this(repository, autorizacao, enviador, mapper, Clock.system(FUSO));
    }

    NotificacaoServiceImpl(NotificacaoRepository repository, AutorizacaoService autorizacao, PushEnviador enviador, ObjectMapper mapper, Clock relogio) {
        this.repository = repository;
        this.autorizacao = autorizacao;
        this.enviador = enviador;
        this.mapper = mapper;
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
        repository.salvarPreferencia(p);
        return p;
    }

    @Override
    public int testar() {
        UsuarioAutenticado u = autorizacao.exigirAutenticado();
        List<PushInscricaoDto> aparelhos = repository.inscricoes(List.of(u.getId()));
        enviador.enviar(aparelhos, conteudo("NordTool", "Pronto! As notificações estão chegando neste aparelho.", "/configuracoes#notificacoes", "teste"));
        return aparelhos.size();
    }

    // ---------------------------------------------------------------- eventos do cronograma e do SAC

    @Override
    public void demandaCriada(Long idDemanda, UsuarioAutenticado ator) {
        emSegundoPlano(() -> repository.demanda(idDemanda).ifPresent(d -> {
            String nome = repository.nomeDoUsuario(ator.getId());
            avisar(envolvidos(d), ator, NotificacaoPreferenciaDto::isInDemandaNova,
                    conteudo("Nova demanda", primeiroNome(nome) + " criou: " + d.getNmCronogramaSemanal() + quando(d), TELA_CRONOGRAMA, "demanda-" + d.getId()));
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
                    conteudo("Demanda alterada", texto, TELA_CRONOGRAMA, "demanda-" + d.getId()));
        }));
    }

    @Override
    public void demandaCompartilhada(Long idDemanda, UsuarioAutenticado ator, Collection<Long> novos) {
        if (novos == null || novos.isEmpty()) return;
        emSegundoPlano(() -> repository.demanda(idDemanda).ifPresent(d -> {
            String nome = primeiroNome(repository.nomeDoUsuario(ator.getId()));
            avisar(new LinkedHashSet<>(novos), ator, NotificacaoPreferenciaDto::isInDemandaNova,
                    conteudo("Demanda compartilhada com você", nome + " compartilhou: " + d.getNmCronogramaSemanal() + quando(d), TELA_CRONOGRAMA, "demanda-" + d.getId()));
        }));
    }

    @Override
    public void sacRespondido(Long idDono, Long idChamado, String titulo, String status, UsuarioAutenticado ator) {
        emSegundoPlano(() -> {
            String nome = primeiroNome(repository.nomeDoUsuario(ator.getId()));
            avisar(Set.of(idDono), ator, NotificacaoPreferenciaDto::isInSac,
                    conteudo("Seu chamado foi respondido", nome + " · " + rotuloSac(status) + " · " + titulo, "/sac?chamado=" + idChamado, "sac-" + idChamado));
        });
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
        if (agora.getMinute() == 0) seguro(() -> repository.limparEnviadas(agora.minusDays(30)));
    }

    private void lembrarHorario(DemandaAvisoDto d, LocalDateTime agora) {
        LocalDateTime hora = d.getDtAgendamento().withSecond(0).withNano(0);
        Set<Long> pessoas = envolvidos(d);
        for (NotificacaoPreferenciaDto p : repository.preferencias(pessoas)) {
            if (!p.isInHorario() || agora.isBefore(hora.minusMinutes(p.getNrAntecedenciaMin()))) continue;
            if (!repository.registrarEnvio(p.getIdUsuario(), "horario:" + d.getId() + ":" + hora)) continue;
            long faltam = java.time.Duration.between(agora, hora).toMinutes();
            String titulo = faltam <= 0 ? "Agora: " + d.getNmCronogramaSemanal() : "Daqui a " + faltam + " min: " + d.getNmCronogramaSemanal();
            enviador.enviar(repository.inscricoes(List.of(p.getIdUsuario())),
                    conteudo(titulo, "Agendado para " + hora.format(HORA), TELA_CRONOGRAMA, "horario-" + d.getId()));
        }
    }

    private void lembrarPrazo(DemandaAvisoDto d, LocalDate hoje) {
        for (NotificacaoPreferenciaDto p : repository.preferencias(envolvidos(d))) {
            if (!p.isInPrazo() || !repository.registrarEnvio(p.getIdUsuario(), "prazo:" + d.getId() + ":" + hoje)) continue;
            enviador.enviar(repository.inscricoes(List.of(p.getIdUsuario())),
                    conteudo("Prazo hoje", d.getNmCronogramaSemanal() + " vence hoje", TELA_CRONOGRAMA, "prazo-" + d.getId()));
        }
    }

    // ---------------------------------------------------------------- apoio

    /** Manda para quem tem aparelho e quer este tipo de aviso; quem fez a ação só se pediu "o que eu mesmo fiz". */
    void avisar(Set<Long> pessoas, UsuarioAutenticado ator, Predicate<NotificacaoPreferenciaDto> quer, String conteudo) {
        if (pessoas.isEmpty()) return;
        List<Long> destino = repository.preferencias(pessoas).stream()
                .filter(quer)
                .filter(p -> !Objects.equals(p.getIdUsuario(), ator.getId()) || p.isInMinhasAcoes())
                .map(NotificacaoPreferenciaDto::getIdUsuario)
                .collect(Collectors.toList());
        if (destino.isEmpty()) return;
        enviador.enviar(repository.inscricoes(destino), conteudo);
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
