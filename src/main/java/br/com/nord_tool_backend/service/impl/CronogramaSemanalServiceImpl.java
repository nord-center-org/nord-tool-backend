package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.exception.NegocioException;
import br.com.nord_tool_backend.domain.CronogramaSemanal;
import br.com.nord_tool_backend.dto.CronogramaSemanalDto;
import br.com.nord_tool_backend.dto.CronogramaSemanalExcecaoDto;
import br.com.nord_tool_backend.form.CronogramaSemanalForm;
import br.com.nord_tool_backend.dto.CronogramaInterpretacaoDto;
import br.com.nord_tool_backend.dto.CronogramaRascunhoDto;
import br.com.nord_tool_backend.form.CronogramaInterpretarForm;
import br.com.nord_tool_backend.repository.CronogramaSemanalRepository;
import br.com.nord_tool_backend.service.CronogramaSemanalService;
import br.com.nord_tool_backend.service.LugiaService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CronogramaSemanalServiceImpl implements CronogramaSemanalService {
    private final Logger log = LogManager.getLogger(CronogramaSemanalServiceImpl.class);

    private final CronogramaSemanalRepository cronogramaSemanalRepository;


    private final AutorizacaoService autorizacao;

    private final LugiaService lugia;

    private final br.com.nord_tool_backend.service.NotificacaoService notificacoes;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CronogramaSemanalDto salvarCronogramaSemanal(CronogramaSemanalForm cronogramaSemanalForm) {
        br.com.nord_tool_backend.security.UsuarioAutenticado ator = autorizacao.exigir(Modulo.CRONOGRAMA, Acao.ESCRITA);
        log.info("Iniciando método para salvar um Cronograma Semanal");
        CronogramaSemanal cronogramaSemanal = cronogramaSemanalForm.converterToDomain();
        if (Boolean.TRUE.equals(cronogramaSemanal.getInCronogramaFixo())) autorizacao.exigir(Modulo.CRONOGRAMA_ROTINA, Acao.ESCRITA);
        validarOrigemCronogramaSemanal(cronogramaSemanal);
        CronogramaSemanalDto cronogramaSemanalDto = cronogramaSemanalRepository.salvarCronogramaSemanal(cronogramaSemanal);
        if (cronogramaSemanalDto != null && cronogramaSemanalDto.getId() != null) notificacoes.demandaCriada(cronogramaSemanalDto.getId(), ator);
        log.info("Finalizando método que salva um Cronograma Semanal");
        return cronogramaSemanalDto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CronogramaSemanalDto alterarCronogramaSemanal(CronogramaSemanalForm cronogramaSemanalForm) {
        br.com.nord_tool_backend.security.UsuarioAutenticado ator = autorizacao.exigir(Modulo.CRONOGRAMA, Acao.ESCRITA);
        log.info("Iniciando método para alterar um Cronograma Semanal");
        CronogramaSemanal cronogramaSemanal = cronogramaSemanalForm.converterToDomain();
        // Mexer numa atividade fixa (ou transformar em fixa) é coisa da rotina.
        if (Boolean.TRUE.equals(cronogramaSemanal.getInCronogramaFixo()) || (cronogramaSemanal.getId() != null
                && Boolean.TRUE.equals(cronogramaSemanalRepository.buscarPorIdCronogramaSemanal(cronogramaSemanal.getId()).getInCronogramaFixo()))) {
            autorizacao.exigir(Modulo.CRONOGRAMA_ROTINA, Acao.ESCRITA);
        }
        validarOrigemCronogramaSemanal(cronogramaSemanal);
        CronogramaSemanalDto cronogramaSemanalDto = cronogramaSemanalRepository.alterarCronogramaSemanal(cronogramaSemanal);
        if (cronogramaSemanal.getId() != null) notificacoes.demandaAlterada(cronogramaSemanal.getId(), ator, null);
        log.info("Finalizando método que alterar um Cronograma Semanal");
        return cronogramaSemanalDto;
    }

    private void validarOrigemCronogramaSemanal(CronogramaSemanal cronogramaSemanal) {
        boolean fixo = Boolean.TRUE.equals(cronogramaSemanal.getInCronogramaFixo());
        if (fixo && cronogramaSemanal.getIdDiaSemana() == null) {
            throw new EntradaInvalidaException("Atividades fixas precisam de um dia da semana.");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletarCronogramaSemanal(Long id) {
        autorizacao.exigir(Modulo.CRONOGRAMA_EXCLUIR, Acao.ESCRITA);
        log.info("Iniciando método para deletar um Cronograma Semanal");
        this.cronogramaSemanalRepository.deletarCronogramaSemanal(id);
        log.info("Finalizando método que deleta um Cronograma Semanal");
    }

    static final java.util.Set<String> STATUS = java.util.Set.of("pendentes", "executar", "aguardar", "acompanhar", "finalizada");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CronogramaSemanalDto moverStatus(Long id, String status) {
        br.com.nord_tool_backend.security.UsuarioAutenticado ator = autorizacao.exigir(Modulo.CRONOGRAMA, Acao.ESCRITA);
        String s = status == null ? "" : status.trim().toLowerCase(Locale.ROOT);
        if (!STATUS.contains(s)) throw new EntradaInvalidaException("Status inválido: " + status);
        if (cronogramaSemanalRepository.moverStatus(id, s) == 0) {
            throw new br.com.nord_tool_backend.exception.NaoEncontradoException("Demanda não encontrada ou fora do seu alcance");
        }
        notificacoes.demandaAlterada(id, ator, "etapa:" + s);
        return CronogramaSemanalDto.converterToDto(cronogramaSemanalRepository.buscarPorIdCronogramaSemanal(id));
    }

    @Override
    public CronogramaSemanalDto buscarPorIdCronogramaSemanal(Long id) {
        autorizacao.exigir(Modulo.CRONOGRAMA, Acao.LEITURA);
        log.info("Iniciando método para buscar um Cronograma Semanal por id");
        CronogramaSemanal cronogramaSemanal = cronogramaSemanalRepository.buscarPorIdCronogramaSemanal(id);
        CronogramaSemanalDto cronogramaSemanalDto = CronogramaSemanalDto.converterToDto(cronogramaSemanal);
        log.info("Finalizando método que busca um Cronograma Semanal por id");
        return cronogramaSemanalDto;
    }

    @Override
    public List<CronogramaSemanalDto> listarCronogramaSemanal() {
        autorizacao.exigir(Modulo.CRONOGRAMA, Acao.LEITURA);
        log.info("Iniciando método para listar Cronograma Semanal");
        List<CronogramaSemanal> lsCronogramaSemanal = cronogramaSemanalRepository.listarCronogramaSemanal();
        List<CronogramaSemanalDto> lsCronogramaSemanalDto = lsCronogramaSemanal.stream().map(CronogramaSemanalDto::converterToDto).collect(Collectors.toList());
        log.info("Finalizando método que lista Cronograma Semanal");
        return lsCronogramaSemanalDto;
    }

    @Override
    public List<CronogramaSemanalExcecaoDto> listarExcecoes() {
        autorizacao.exigir(Modulo.CRONOGRAMA, Acao.LEITURA);
        return cronogramaSemanalRepository.listarExcecoes();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incluirExcecao(Long id, LocalDate data) {
        autorizacao.exigir(Modulo.CRONOGRAMA_ROTINA, Acao.ESCRITA);
        CronogramaSemanal item = cronogramaSemanalRepository.buscarPorIdCronogramaSemanal(id);
        exigirDono(item);
        if (!Boolean.TRUE.equals(item.getInCronogramaFixo())) {
            throw new EntradaInvalidaException("Só compromissos fixos aceitam exceção por dia.");
        }
        cronogramaSemanalRepository.incluirExcecao(id, data);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removerExcecao(Long id, LocalDate data) {
        autorizacao.exigir(Modulo.CRONOGRAMA_ROTINA, Acao.ESCRITA);
        exigirDono(cronogramaSemanalRepository.buscarPorIdCronogramaSemanal(id));
        cronogramaSemanalRepository.removerExcecao(id, data);
    }

    static final int MAX_COMPARTILHAMENTOS = 200;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CronogramaSemanalDto compartilhar(Long id, List<Long> idsUsuarios) {
        br.com.nord_tool_backend.security.UsuarioAutenticado u = autorizacao.exigir(Modulo.CRONOGRAMA_COMPARTILHAR, Acao.ESCRITA);
        CronogramaSemanal item = cronogramaSemanalRepository.buscarPorIdCronogramaSemanal(id);
        exigirDono(item);
        java.util.LinkedHashSet<Long> destinos = new java.util.LinkedHashSet<>();
        for (Long d : idsUsuarios == null ? new ArrayList<Long>() : idsUsuarios) {
            if (d != null && !d.equals(u.getId())) destinos.add(d);
        }
        if (destinos.size() > MAX_COMPARTILHAMENTOS) throw new EntradaInvalidaException("Compartilhe com no máximo " + MAX_COMPARTILHAMENTOS + " pessoas");
        if (!destinos.isEmpty() && cronogramaSemanalRepository.contarUsuariosAtivos(destinos) != destinos.size()) {
            throw new EntradaInvalidaException("Há pessoas inativas ou inexistentes na lista");
        }
        java.util.List<Long> antes = CronogramaSemanalDto.converterToDto(item).getIdsCompartilhados();
        cronogramaSemanalRepository.compartilhar(id, new ArrayList<>(destinos));
        java.util.Set<Long> novos = new java.util.LinkedHashSet<>(destinos);
        if (antes != null) novos.removeAll(antes);
        notificacoes.demandaCompartilhada(id, u, novos);
        return CronogramaSemanalDto.converterToDto(cronogramaSemanalRepository.buscarPorIdCronogramaSemanal(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<br.com.nord_tool_backend.dto.PessoaCronogramaDto> listarPessoas() {
        autorizacao.exigir(Modulo.CRONOGRAMA, Acao.LEITURA);
        return cronogramaSemanalRepository.listarPessoas();
    }

    // ---------------------------------------------------------------- demanda pelo chat da Lugia

    static final List<String> CATEGORIAS = Arrays.asList("Pessoal", "Acadêmica", "Atlética", "Musical", "Devocional", "Engenharia", "Programação");
    static final String CATEGORIA_PADRAO = "Engenharia";
    /** Ids de dia_semana: 1 = segunda … 5 = sexta, 7 = sábado (6 é "Sem agendamento" e não há domingo). */
    static final List<Integer> DIAS_VALIDOS = Arrays.asList(1, 2, 3, 4, 5, 7);
    static final int MAX_DEMANDAS = 10;
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter AGENDAMENTO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    @Override
    public CronogramaInterpretacaoDto interpretar(CronogramaInterpretarForm form) {
        autorizacao.exigir(Modulo.CRONOGRAMA, Acao.ESCRITA);
        LocalDate hoje = dataOuNull(form.getHoje());
        String resposta = lugia.responderUmaVez(sistemaInterpretar(), pedidoInterpretar(form, hoje == null ? LocalDate.now(FUSO) : hoje));
        return lerInterpretacao(resposta);
    }

    /** Fixo entre as chamadas (sem a data) para aproveitar o cache de prompt dos provedores. */
    static String sistemaInterpretar() {
        return String.join("\n",
                "Você monta demandas do Cronograma Semanal do NordTool a partir do que a pessoa escreve em português.",
                "Responda SOMENTE com um objeto JSON, sem texto fora dele e sem bloco de código, neste formato:",
                "{\"mensagem\": \"frase curta e simpática confirmando o que foi montado (ou perguntando o que faltou)\",",
                " \"demandas\": [{\"titulo\": \"título curto e claro, com inicial maiúscula\",",
                "   \"observacao\": \"detalhes úteis (local, pessoas, pauta) ou null\",",
                "   \"categoria\": \"uma de: " + String.join(", ", CATEGORIAS) + "\",",
                "   \"status\": \"pendentes | executar | aguardar | acompanhar | finalizada\",",
                "   \"tag\": \"palavra curta para agrupar (obra, cliente, projeto) ou null\",",
                "   \"prazo\": \"AAAA-MM-DD ou null\",",
                "   \"fixa\": true quando se repete toda semana, senão false,",
                "   \"dias\": [dias da semana da atividade fixa: 1=segunda, 2=terça, 3=quarta, 4=quinta, 5=sexta, 7=sábado],",
                "   \"horario\": \"HH:mm da atividade fixa ou null\",",
                "   \"agendamento\": \"AAAA-MM-DDTHH:mm quando é um compromisso numa data e hora, senão null\"}]}",
                "Regras: uma demanda por tarefa pedida; datas relativas (amanhã, sexta que vem) contam a partir de hoje;",
                "\"toda segunda e quarta\" é fixa com dias [1,3]; \"dia útil\" é [1,2,3,4,5]; domingo não existe no cronograma;",
                "na dúvida da categoria use " + CATEGORIA_PADRAO + " e do status use pendentes; não invente prazo nem horário.",
                "Se a pessoa estiver ajustando um pedido anterior, devolva a lista completa já ajustada.");
    }

    static String pedidoInterpretar(CronogramaInterpretarForm form, LocalDate hoje) {
        StringBuilder sb = new StringBuilder("Hoje é ")
                .append(hoje.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("pt", "BR")))
                .append(", ").append(hoje).append(".\n");
        List<String> anteriores = form.getAnteriores() == null ? new ArrayList<>() : form.getAnteriores();
        if (!anteriores.isEmpty()) {
            sb.append("Pedidos anteriores neste chat:\n");
            for (String a : anteriores) sb.append("- ").append(a).append("\n");
        }
        return sb.append("Pedido atual: ").append(form.getTexto().trim()).toString();
    }

    /** Lê o JSON da resposta (tolera texto ou bloco de código em volta) e deixa cada campo num valor aceito pela tela. */
    static CronogramaInterpretacaoDto lerInterpretacao(String resposta) {
        String texto = resposta == null ? "" : resposta;
        int ini = texto.indexOf('{');
        int fim = texto.lastIndexOf('}');
        JsonNode raiz;
        try {
            raiz = ini >= 0 && fim > ini ? JSON.readTree(texto.substring(ini, fim + 1)) : null;
        } catch (JsonProcessingException e) {
            raiz = null;
        }
        if (raiz == null || !raiz.isObject()) {
            throw new NegocioException("LUGIA_RESPOSTA_INVALIDA", "A Lugia não conseguiu montar a demanda. Tente escrever de outro jeito.");
        }
        List<CronogramaRascunhoDto> demandas = new ArrayList<>();
        for (JsonNode d : raiz.path("demandas")) {
            String titulo = textoOuNull(d.get("titulo"));
            if (titulo == null || demandas.size() == MAX_DEMANDAS) continue;
            TreeSet<Integer> dias = new TreeSet<>();
            for (JsonNode n : d.path("dias")) if (n.canConvertToInt() && DIAS_VALIDOS.contains(n.asInt())) dias.add(n.asInt());
            boolean fixa = d.path("fixa").asBoolean(false) && !dias.isEmpty();
            String status = textoOuNull(d.get("status"));
            String horario = horaOuNull(textoOuNull(d.get("horario")));
            LocalDate prazo = dataOuNull(textoOuNull(d.get("prazo")));
            demandas.add(CronogramaRascunhoDto.builder()
                    .nmCronogramaSemanal(limitar(titulo, 200))
                    .txObservacao(limitar(textoOuNull(d.get("observacao")), 2000))
                    .nmCategoria(categoria(textoOuNull(d.get("categoria"))))
                    .nmStatusCronograma(status != null && STATUS.contains(status.toLowerCase(Locale.ROOT)) ? status.toLowerCase(Locale.ROOT) : "pendentes")
                    .nmTag(limitar(textoOuNull(d.get("tag")), 60))
                    .dtPrazo(prazo == null ? null : prazo.toString())
                    .flFixo(fixa)
                    .diasSemana(fixa ? new ArrayList<>(dias) : new ArrayList<>())
                    .nmHorario(fixa ? horario : null)
                    .dtAgendamento(fixa ? null : agendamentoOuNull(textoOuNull(d.get("agendamento"))))
                    .build());
        }
        String mensagem = textoOuNull(raiz.get("mensagem"));
        if (mensagem == null) mensagem = demandas.isEmpty() ? "Não entendi o que programar. Pode detalhar?" : "Pronto, montei a demanda.";
        return new CronogramaInterpretacaoDto(limitar(mensagem, 500), demandas);
    }

    private static String textoOuNull(JsonNode n) {
        if (n == null || n.isNull() || !n.isValueNode()) return null;
        String t = n.asText().trim();
        return t.isEmpty() || "null".equalsIgnoreCase(t) ? null : t;
    }

    private static String limitar(String t, int max) {
        return t == null || t.length() <= max ? t : t.substring(0, max);
    }

    /** Categoria da lista sem ligar para maiúsculas e acentos; fora da lista, a padrão. */
    static String categoria(String valor) {
        if (valor == null) return CATEGORIA_PADRAO;
        String chave = semAcento(valor);
        return CATEGORIAS.stream().filter(c -> semAcento(c).equals(chave)).findFirst().orElse(CATEGORIA_PADRAO);
    }

    private static String semAcento(String t) {
        return Normalizer.normalize(t, Normalizer.Form.NFD).replaceAll("\\p{M}", "").trim().toLowerCase(Locale.ROOT);
    }

    static LocalDate dataOuNull(String t) {
        if (t == null) return null;
        try {
            return LocalDate.parse(t.length() > 10 ? t.substring(0, 10) : t);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String horaOuNull(String t) {
        if (t == null) return null;
        try {
            return LocalTime.parse(t.length() == 4 ? "0" + t : t.substring(0, Math.min(5, t.length()))).toString();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String agendamentoOuNull(String t) {
        if (t == null) return null;
        try {
            return LocalDateTime.parse(t.length() > 16 ? t.substring(0, 16) : t, AGENDAMENTO).format(AGENDAMENTO);
        } catch (DateTimeParseException e) {
            LocalDate data = dataOuNull(t);
            return data == null ? null : data.atTime(9, 0).format(AGENDAMENTO);
        }
    }

    /** Compartilhado comigo é só leitura: exceções e compartilhamento são do dono. */
    private static void exigirDono(CronogramaSemanal item) {
        if (Boolean.FALSE.equals(item.getInProprio())) {
            throw new br.com.nord_tool_backend.exception.AcessoNegadoException("Só quem criou o item pode alterá-lo");
        }
    }
}
