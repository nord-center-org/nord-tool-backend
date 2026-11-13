package br.com.nord_tool_backend.service.lugia;

import br.com.nord_tool_backend.dto.LugiaMensagemDto;
import br.com.nord_tool_backend.dto.LugiaParteDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/**
 * Secretária local: o Ollama na máquina do dono do NordTool, pela API nativa ({@code /api/chat}, NDJSON). A máquina
 * fica atrás de um túnel reverso (Cloudflare Tunnel + Access): nenhuma porta aberta no roteador e só este servidor,
 * com o service token, passa. Toda chamada passa pelo {@link LugiaProtetor} (fila curta, poucos pedidos ao mesmo
 * tempo, disjuntor) e por tempos-limite, e pede ao Ollama contexto e resposta limitados, para não sobrecarregar o
 * hardware. Vem primeiro no modo Automático: com a máquina fora, o disjuntor recusa na hora e a cadeia segue para a
 * nuvem.
 */
@Slf4j
@Component
@Order(0)
public class LugiaOllamaProvider implements LugiaIaProvider {

    private final LugiaOllamaProperties props;
    private final ObjectMapper json;
    private final LugiaProtetor protetor;
    private final LongSupplier relogio;

    @Autowired
    public LugiaOllamaProvider(LugiaOllamaProperties props, ObjectMapper json) {
        this(props, json, System::currentTimeMillis);
    }

    LugiaOllamaProvider(LugiaOllamaProperties props, ObjectMapper json, LongSupplier relogio) {
        this.props = props;
        this.json = json;
        this.relogio = relogio;
        this.protetor = new LugiaProtetor(props.getMaxSimultaneos(), props.getFilaMax(), props.getEsperaMaxMs(), 3, 60_000, relogio);
        if (props.configurada() && !props.segura()) {
            log.warn("Secretária local desligada: a URL do Ollama fora de localhost precisa de HTTPS e de proteção (Cloudflare Access ou token).");
        }
    }

    @Override public String id() { return "ollama"; }
    @Override public String nome() { return "Secretária local"; }
    @Override public List<String> modelos() { return props.getModelos(); }
    @Override public boolean disponivel() { return props.segura(); }
    /** Sem custo por uso: entra (primeiro) na cadeia do modo Automático. */
    @Override public boolean gratuito() { return true; }

    LugiaProtetor protetor() { return protetor; }

    @Override
    public LugiaResposta conversar(String modelo, String sistema, List<LugiaMensagemDto> historico, Consumer<String> aoTexto) {
        Runnable liberar = protetor.reservar();
        long limite = relogio.getAsLong() + props.getTotalMs();
        StringBuilder texto = new StringBuilder();
        String[] motivo = {"stop"};
        Integer[] uso = {null, null};
        try {
            LugiaSseHttp.postarLinhas(props.getUrl() + "/api/chat", props.cabecalhos(), json.writeValueAsString(corpo(modelo, sistema, historico)),
                    Duration.ofMillis(props.getPrimeiroPedacoMs()), linha -> {
                        if (relogio.getAsLong() > limite) throw new TempoEsgotado();
                        JsonNode ev = ler(linha);
                        if (ev.hasNonNull("error")) throw new ErroDoModelo(ev.path("error").asText());
                        String delta = ev.path("message").path("content").asText("");
                        if (!delta.isEmpty()) {
                            texto.append(delta);
                            aoTexto.accept(delta);
                        }
                        if (ev.path("done").asBoolean(false)) {
                            motivo[0] = ev.path("done_reason").asText("stop");
                            uso[0] = ev.path("prompt_eval_count").asInt();
                            uso[1] = ev.path("eval_count").asInt();
                        }
                    });
            protetor.sucesso();
            return new LugiaResposta(texto.toString(), null, motivo[0], uso[0], uso[1]);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LugiaFalhaException("Resposta cancelada.", e);
        } catch (TempoEsgotado e) {
            protetor.falha();
            throw new LugiaFalhaException("A resposta da secretária local passou do tempo-limite.", e, 30);
        } catch (ErroDoModelo e) {
            protetor.falha();
            throw new LugiaFalhaException("O modelo local respondeu com erro: " + e.getMessage(), e, 30);
        } catch (Exception e) {
            if (!(e instanceof LugiaSseHttp.ErroHttp) || ((LugiaSseHttp.ErroHttp) e).status >= 500 || ((LugiaSseHttp.ErroHttp) e).status == 403) protetor.falha();
            log.warn("Secretária local falhou: {}", e.getMessage());
            throw new LugiaFalhaException(mensagem(modelo, e), e, pausa(e));
        } finally {
            liberar.run();
        }
    }

    /** Corpo de /api/chat: histórico só em texto (anexos viram aviso), contexto e resposta limitados, modelo carregado por um tempo. */
    ObjectNode corpo(String modelo, String sistema, List<LugiaMensagemDto> historico) {
        ObjectNode corpo = json.createObjectNode().put("model", modelo).put("stream", true).put("keep_alive", props.getKeepAlive());
        corpo.putObject("options").put("num_ctx", props.getNumCtx()).put("num_predict", props.getMaxTokens());
        ArrayNode itens = corpo.putArray("messages");
        itens.addObject().put("role", "system").put("content", sistema);
        for (LugiaMensagemDto m : historico) {
            boolean assistente = LugiaMensagemDto.ASSISTENTE.equals(m.getPapel());
            StringBuilder conteudo = new StringBuilder();
            if (!assistente) {
                for (LugiaParteDto p : m.getPartes()) {
                    if (!LugiaParteDto.TEXTO.equals(p.getTipo())) {
                        conteudo.append("[Anexo \"").append(p.getNome() == null ? p.getTipo() : p.getNome())
                                .append("\" não enviado: a secretária local só lê texto. Para imagens e PDFs, use o Gemini ou o Claude.]\n");
                    }
                }
            }
            String texto = LugiaClaudeProvider.textoOuReticencias(m.getPartes());
            if (!"…".equals(texto) || conteudo.length() == 0) conteudo.append(texto);
            itens.addObject().put("role", assistente ? "assistant" : "user").put("content", conteudo.toString().trim());
        }
        return corpo;
    }

    static String mensagem(String modelo, Exception e) {
        if (e instanceof HttpTimeoutException) return "A secretária local demorou para começar a responder (modelo carregando ou máquina ocupada). Tente de novo.";
        if (e instanceof LugiaSseHttp.ErroHttp) {
            int status = ((LugiaSseHttp.ErroHttp) e).status;
            String msg = e.getMessage() == null ? "" : e.getMessage();
            if (status == 404 && msg.contains("model")) return "O modelo \"" + modelo + "\" não está instalado na máquina local (rode: ollama pull " + modelo + ").";
            if (status == 401 || status == 403) return "O túnel recusou o acesso: confira o service token do Cloudflare Access no servidor.";
            if (status == 502 || status == 503 || status == 530) return "A secretária local está fora do ar (máquina desligada ou túnel parado).";
            return "Falha na secretária local (" + status + ").";
        }
        return "Não deu para falar com a secretária local (máquina desligada ou túnel parado).";
    }

    private static long pausa(Exception e) {
        return e instanceof LugiaSseHttp.ErroHttp && ((LugiaSseHttp.ErroHttp) e).status < 500 ? 0 : 60;
    }

    private JsonNode ler(String linha) {
        try {
            return json.readTree(linha);
        } catch (Exception e) {
            return json.createObjectNode();
        }
    }

    /** Passou do tempo total da resposta (interrompe a leitura). */
    static final class TempoEsgotado extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    /** O Ollama mandou {"error": "..."} no meio da resposta. */
    static final class ErroDoModelo extends RuntimeException {
        private static final long serialVersionUID = 1L;

        ErroDoModelo(String mensagem) { super(mensagem); }
    }
}
