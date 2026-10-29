package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.dto.LugiaConversaDto;
import br.com.nord_tool_backend.dto.LugiaMensagemDto;
import br.com.nord_tool_backend.dto.LugiaModeloDto;
import br.com.nord_tool_backend.dto.LugiaModelosDto;
import br.com.nord_tool_backend.dto.LugiaParteDto;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.exception.NaoEncontradoException;
import br.com.nord_tool_backend.exception.NegocioException;
import br.com.nord_tool_backend.form.LugiaAnexoForm;
import br.com.nord_tool_backend.form.LugiaChatForm;
import br.com.nord_tool_backend.form.LugiaTituloForm;
import br.com.nord_tool_backend.repository.LugiaRepository;
import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.security.UsuarioAutenticado;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.service.LugiaService;
import br.com.nord_tool_backend.service.lugia.LugiaAgenteClient;
import br.com.nord_tool_backend.service.lugia.LugiaFalhaException;
import br.com.nord_tool_backend.service.lugia.LugiaIaProvider;
import br.com.nord_tool_backend.service.lugia.LugiaProperties;
import br.com.nord_tool_backend.service.lugia.LugiaResposta;
import br.com.nord_tool_backend.service.lugia.LugiaSaida;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Lugia. Conversas são do usuário logado; o histórico fica num formato neutro para trocar de provedor no meio
 * da conversa. O modo "agente" (executar no NordTool) exige ADMIN no módulo LUGIA. O modelo "automatico" percorre
 * os provedores gratuitos e passa para o próximo quando um deles atinge o limite antes de começar a responder.
 */
@Service @Slf4j
public class LugiaServiceImpl implements LugiaService {

    static final String MODO_AGENTE = "agente";
    static final String PROVEDOR_AGENTE = "agente";
    static final String AUTOMATICO = "automatico";
    /** Pausa de um modelo que falhou sem o provedor dizer por quanto tempo esperar. */
    static final long PAUSA_PADRAO_SEGUNDOS = 60;
    static final long PAUSA_MAXIMA_SEGUNDOS = 24 * 60 * 60;
    static final int MAX_ANEXOS = 5;
    static final List<String> MIMES_IMAGEM = Arrays.asList("image/png", "image/jpeg", "image/webp", "image/gif");
    private static final Pattern BASE64 = Pattern.compile("^[A-Za-z0-9+/]+={0,2}$");
    private static final Pattern PREFIXO_DATA_URL = Pattern.compile("^data:[^;]+;base64,");

    private final LugiaRepository repository;
    private final AutorizacaoService autorizacao;
    private final List<LugiaIaProvider> provedores;
    private final LugiaAgenteClient agente;
    private final LugiaProperties props;
    private final Clock clock;
    /** Modelo (provedor:modelo) → até quando fica no fim da fila do modo Automático. Só em memória: some no restart. */
    private final Map<String, Instant> pausados = new ConcurrentHashMap<>();

    public LugiaServiceImpl(LugiaRepository repository, AutorizacaoService autorizacao, List<LugiaIaProvider> provedores,
                            LugiaAgenteClient agente, LugiaProperties props, Clock clock) {
        this.repository = repository;
        this.autorizacao = autorizacao;
        this.provedores = provedores;
        this.agente = agente;
        this.props = props;
        this.clock = clock;
    }

    @Override
    public LugiaModelosDto modelos() {
        UsuarioAutenticado u = autorizacao.exigir(Modulo.LUGIA, Acao.LEITURA);
        List<LugiaModeloDto> modelos = new ArrayList<>();
        if (!cadeiaAutomatico().isEmpty()) {
            modelos.add(new LugiaModeloDto(AUTOMATICO, AUTOMATICO, "Automático · troca sozinho entre as IAs gratuitas"));
        }
        for (LugiaIaProvider p : provedores) {
            if (!p.disponivel()) continue;
            for (String m : p.modelos()) modelos.add(new LugiaModeloDto(p.id(), m, p.nome() + " · " + m));
        }
        Acao nivel = u.acao(Modulo.LUGIA);
        return new LugiaModelosDto(modelos, nivel.name(), nivel.atende(Acao.ADMIN) && agente.disponivel());
    }

    @Override
    public List<LugiaConversaDto> conversas() {
        UsuarioAutenticado u = autorizacao.exigir(Modulo.LUGIA, Acao.LEITURA);
        return repository.listarConversas(u.getId());
    }

    @Override
    public List<LugiaMensagemDto> mensagens(Long idConversa) {
        UsuarioAutenticado u = autorizacao.exigir(Modulo.LUGIA, Acao.LEITURA);
        conversaDoUsuario(u, idConversa);
        return repository.mensagens(idConversa).stream()
                .map(m -> new LugiaMensagemDto(m.getPapel(), m.getProvedor(), paraTela(m.getPartes()), null))
                .collect(Collectors.toList());
    }

    @Override
    public void renomear(Long idConversa, LugiaTituloForm form) {
        UsuarioAutenticado u = autorizacao.exigir(Modulo.LUGIA, Acao.LEITURA);
        if (!repository.renomear(u.getId(), idConversa, form.getTitulo().trim())) throw naoEncontrada();
    }

    @Override
    public void excluir(Long idConversa) {
        UsuarioAutenticado u = autorizacao.exigir(Modulo.LUGIA, Acao.LEITURA);
        if (!repository.excluir(u.getId(), idConversa)) throw naoEncontrada();
    }

    @Override
    public void conversar(LugiaChatForm form, LugiaSaida saida) {
        boolean modoAgente = MODO_AGENTE.equals(form.getModo());
        UsuarioAutenticado u = autorizacao.exigir(Modulo.LUGIA, modoAgente ? Acao.ADMIN : Acao.LEITURA);

        boolean automatico = !modoAgente && AUTOMATICO.equals(form.getModelo());
        List<Opcao> opcoes = Collections.emptyList();
        if (modoAgente) {
            if (!agente.disponivel()) {
                throw new NegocioException("LUGIA_AGENTE_INDISPONIVEL", "O agente ainda não foi configurado no servidor (ANTHROPIC_API_KEY e LUGIA_GITHUB_TOKEN).");
            }
        } else {
            opcoes = automatico ? cadeiaAutomatico()
                    : provedorDo(form.getModelo()).map(p -> Collections.singletonList(new Opcao(p, form.getModelo()))).orElse(Collections.emptyList());
            if (opcoes.isEmpty()) throw new EntradaInvalidaException("Modelo indisponível.");
        }

        List<LugiaParteDto> partes = partes(form);
        String texto = form.getTexto() == null ? "" : form.getTexto();

        LugiaConversaDto conversa;
        List<LugiaMensagemDto> historico = new ArrayList<>();
        if (form.getIdConversa() != null) {
            conversa = conversaDoUsuario(u, form.getIdConversa());
            historico.addAll(repository.mensagens(conversa.getId()));
        } else {
            conversa = new LugiaConversaDto();
            conversa.setTitulo(tituloInicial(texto, partes));
            conversa.setId(repository.criarConversa(u.getId(), conversa.getTitulo()));
        }
        LugiaMensagemDto daVez = new LugiaMensagemDto(LugiaMensagemDto.USUARIO, null, partes, null);
        repository.adicionarMensagem(conversa.getId(), daVez, null, null, null);
        historico.add(daVez);
        if (automatico) opcoes = emOrdemDeTentativa(opcoes, temAnexo(historico));

        saida.abrir();
        saida.conversa(conversa.getId());
        StringBuilder recebido = new StringBuilder();
        String provedorId = modoAgente ? PROVEDOR_AGENTE : opcoes.get(0).provedor.id();
        String modelo = modoAgente ? props.getModeloAgente() : opcoes.get(0).modelo;
        try {
            LugiaResposta resposta;
            if (modoAgente) {
                String sessao = conversa.getSessaoAgente();
                if (sessao == null || sessao.isEmpty()) {
                    sessao = agente.criarSessao(conversa.getTitulo());
                    repository.definirSessaoAgente(conversa.getId(), sessao);
                }
                resposta = agente.executar(sessao, partes, new SaidaQueAcumula(saida, recebido));
            } else {
                resposta = null;
                for (int i = 0; resposta == null; i++) {
                    Opcao opcao = opcoes.get(i);
                    provedorId = opcao.provedor.id();
                    modelo = opcao.modelo;
                    if (automatico) saida.provedor(provedorId, modelo);
                    try {
                        resposta = opcao.provedor.conversar(modelo, sistema(), historico, delta -> {
                            recebido.append(delta);
                            saida.texto(delta);
                        });
                    } catch (LugiaFalhaException e) {
                        // Só troca antes do primeiro pedaço: depois disso a pessoa já está lendo esta resposta.
                        if (recebido.length() > 0 || Thread.currentThread().isInterrupted()) throw e;
                        pausar(opcao, e);
                        if (!automatico) throw e;
                        if (i == opcoes.size() - 1) {
                            throw new LugiaFalhaException("Todas as IAs gratuitas estão no limite agora. Tente de novo em alguns minutos ou escolha outro modelo.", e);
                        }
                        log.info("Lugia automático: {} ({}) falhou, tentando o próximo", opcao.chave(), e.getMessage());
                        LugiaIaProvider proximo = opcoes.get(i + 1).provedor;
                        saida.ferramenta(opcao.provedor.nome() + " indisponível agora; passando para "
                                + (proximo == opcao.provedor ? "outro modelo do " + proximo.nome() : proximo.nome()));
                    }
                }
            }
            gravarResposta(conversa.getId(), provedorId, modelo, resposta.getTexto(), resposta.getNativo(),
                    resposta.getTokensEntrada(), resposta.getTokensSaida());
            saida.fim(resposta.getMotivo());
        } catch (LugiaFalhaException e) {
            // Guarda o que já chegou: a pessoa vê a resposta parcial ao reabrir a conversa.
            gravarResposta(conversa.getId(), provedorId, modelo, recebido.toString(), null, null, null);
            saida.erro(e.getMessage());
        }
    }

    private void gravarResposta(Long idConversa, String provedor, String modelo, String texto, String nativo,
                                Integer tokensEntrada, Integer tokensSaida) {
        if ((texto == null || texto.isEmpty()) && nativo == null) return;
        List<LugiaParteDto> partes = new ArrayList<>();
        partes.add(LugiaParteDto.texto(texto == null ? "" : texto));
        repository.adicionarMensagem(idConversa, new LugiaMensagemDto(LugiaMensagemDto.ASSISTENTE, provedor, partes, nativo),
                modelo, tokensEntrada, tokensSaida);
    }

    /** Estável entre as mensagens (sem data/hora) para aproveitar o cache de prompt dos provedores. */
    static String sistema() {
        return String.join("\n",
                "Você é a Lugia, assistente de IA integrada ao NordTool, sistema de gestão de obras e entregas da Nord.",
                "Responda em português do Brasil, de forma direta e útil. Use Markdown quando ajudar a leitura.",
                "Quando não souber algo sobre os dados do NordTool, diga isso em vez de inventar.",
                "Imagens anexadas costumam ser prints de telas do NordTool.");
    }

    /**
     * Modelos gratuitos disponíveis, na ordem de LUGIA_CADEIA_AUTOMATICO ou, sem ela, na ordem dos provedores.
     * Ids da cadeia que não estiverem disponíveis (sem chave, fora da lista do provedor) são ignorados.
     */
    List<Opcao> cadeiaAutomatico() {
        List<Opcao> gratuitos = new ArrayList<>();
        for (LugiaIaProvider p : provedores) {
            if (!p.disponivel() || !p.gratuito()) continue;
            for (String m : p.modelos()) gratuitos.add(new Opcao(p, m));
        }
        List<String> ordem = props.getCadeiaAutomatico();
        if (ordem.isEmpty()) return gratuitos;
        List<Opcao> cadeia = new ArrayList<>();
        for (String id : ordem) {
            gratuitos.stream().filter(o -> o.modelo.equals(id) && !cadeia.contains(o)).findFirst().ifPresent(cadeia::add);
        }
        return cadeia;
    }

    /**
     * Pausados vão para o fim (ainda são tentados se todos estiverem pausados). Com anexo na conversa, quem lê
     * imagem e PDF vem antes. A ordem configurada vale dentro de cada grupo.
     */
    List<Opcao> emOrdemDeTentativa(List<Opcao> cadeia, boolean comAnexo) {
        Instant agora = clock.instant();
        List<Opcao> ordem = new ArrayList<>(cadeia);
        ordem.sort(Comparator.<Opcao>comparingInt(o -> pausados.getOrDefault(o.chave(), Instant.MIN).isAfter(agora) ? 1 : 0)
                .thenComparingInt(o -> comAnexo && !o.provedor.leAnexos() ? 1 : 0));
        return ordem;
    }

    private void pausar(Opcao opcao, LugiaFalhaException e) {
        long segundos = e.getPausaSegundos() > 0 ? Math.min(e.getPausaSegundos(), PAUSA_MAXIMA_SEGUNDOS) : PAUSA_PADRAO_SEGUNDOS;
        pausados.put(opcao.chave(), clock.instant().plusSeconds(segundos));
    }

    private static boolean temAnexo(List<LugiaMensagemDto> historico) {
        return historico.stream().filter(m -> !LugiaMensagemDto.ASSISTENTE.equals(m.getPapel()))
                .flatMap(m -> m.getPartes().stream()).anyMatch(p -> !LugiaParteDto.TEXTO.equals(p.getTipo()));
    }

    private Optional<LugiaIaProvider> provedorDo(String modelo) {
        if (modelo == null) return Optional.empty();
        return provedores.stream().filter(p -> p.disponivel() && p.modelos().contains(modelo)).findFirst();
    }

    /** Anexos validados (imagem ou PDF, até 5, tamanho máximo) seguidos do texto. */
    List<LugiaParteDto> partes(LugiaChatForm form) {
        List<LugiaAnexoForm> anexos = form.getAnexos() == null ? new ArrayList<>() : form.getAnexos();
        if (anexos.size() > MAX_ANEXOS) throw new EntradaInvalidaException("Envie no máximo " + MAX_ANEXOS + " anexos por mensagem.");
        List<LugiaParteDto> partes = new ArrayList<>();
        for (LugiaAnexoForm a : anexos) {
            String rotulo = a.getNome() == null ? "anexo" : a.getNome();
            String base64 = a.getBase64() == null ? "" : PREFIXO_DATA_URL.matcher(a.getBase64()).replaceFirst("");
            if (base64.isEmpty() || !BASE64.matcher(base64).matches()) throw new EntradaInvalidaException(rotulo + ": conteúdo inválido.");
            if (bytesBase64(base64) > props.getAnexoMaxBytes()) {
                throw new EntradaInvalidaException(rotulo + ": maior que " + props.getAnexoMaxBytes() / 1024 / 1024 + " MB.");
            }
            if (MIMES_IMAGEM.contains(a.getMime())) {
                partes.add(new LugiaParteDto(LugiaParteDto.IMAGEM, null, a.getMime(), base64, a.getNome()));
            } else if ("application/pdf".equals(a.getMime())) {
                partes.add(new LugiaParteDto(LugiaParteDto.PDF, null, null, base64, a.getNome()));
            } else {
                throw new EntradaInvalidaException(rotulo + ": tipo não suportado (use PNG, JPEG, WEBP, GIF ou PDF).");
            }
        }
        String texto = form.getTexto() == null ? "" : form.getTexto();
        if (!texto.trim().isEmpty()) partes.add(LugiaParteDto.texto(texto));
        if (partes.isEmpty()) throw new EntradaInvalidaException("Escreva uma mensagem ou anexe um arquivo.");
        return partes;
    }

    static long bytesBase64(String base64) {
        int preenchimento = base64.endsWith("==") ? 2 : base64.endsWith("=") ? 1 : 0;
        return (long) base64.length() * 3 / 4 - preenchimento;
    }

    static String tituloInicial(String texto, List<LugiaParteDto> partes) {
        String limpo = texto.replaceAll("\\s+", " ").trim();
        if (!limpo.isEmpty()) return limpo.length() > 60 ? limpo.substring(0, 57) + "..." : limpo;
        return partes.stream().filter(p -> p.getNome() != null).map(LugiaParteDto::getNome).findFirst().orElse("Nova conversa");
    }

    /** Para a tela: tira o base64 dos anexos (pesado) e mantém só o que identifica o arquivo. */
    static List<LugiaParteDto> paraTela(List<LugiaParteDto> partes) {
        return partes.stream()
                .map(p -> LugiaParteDto.TEXTO.equals(p.getTipo()) ? p : new LugiaParteDto(p.getTipo(), null, p.getMime(), null, p.getNome()))
                .collect(Collectors.toList());
    }

    private LugiaConversaDto conversaDoUsuario(UsuarioAutenticado u, Long idConversa) {
        return repository.buscarConversa(u.getId(), idConversa).orElseThrow(LugiaServiceImpl::naoEncontrada);
    }

    private static NaoEncontradoException naoEncontrada() {
        return new NaoEncontradoException("Conversa não encontrada.");
    }

    /** Um modelo de um provedor, como tentativa do modo Automático. */
    static final class Opcao {
        final LugiaIaProvider provedor;
        final String modelo;

        Opcao(LugiaIaProvider provedor, String modelo) {
            this.provedor = provedor;
            this.modelo = modelo;
        }

        String chave() { return provedor.id() + ":" + modelo; }

        @Override public boolean equals(Object o) { return o instanceof Opcao && ((Opcao) o).chave().equals(chave()); }
        @Override public int hashCode() { return chave().hashCode(); }
    }

    /** Repassa o progresso do agente e guarda o texto já recebido (para gravar se a sessão falhar no meio). */
    private static final class SaidaQueAcumula implements LugiaSaida {
        private final LugiaSaida destino;
        private final StringBuilder recebido;

        SaidaQueAcumula(LugiaSaida destino, StringBuilder recebido) {
            this.destino = destino;
            this.recebido = recebido;
        }

        @Override public void abrir() { destino.abrir(); }
        @Override public void conversa(Long idConversa) { destino.conversa(idConversa); }
        @Override public void texto(String delta) { recebido.append(delta); destino.texto(delta); }
        @Override public void ferramenta(String descricao) { destino.ferramenta(descricao); }
        @Override public void fim(String motivo) { destino.fim(motivo); }
        @Override public void erro(String mensagem) { destino.erro(mensagem); }
    }
}
