package br.com.nord_tool_backend.service.lugia;

import br.com.nord_tool_backend.dto.LugiaMensagemDto;
import br.com.nord_tool_backend.dto.LugiaParteDto;
import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.ObjectMappers;
import com.anthropic.core.http.StreamResponse;
import com.anthropic.errors.AnthropicIoException;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.BadRequestException;
import com.anthropic.errors.PermissionDeniedException;
import com.anthropic.errors.RateLimitException;
import com.anthropic.errors.UnauthorizedException;
import com.anthropic.helpers.BetaMessageAccumulator;
import com.anthropic.models.beta.messages.BetaAssistantMessageParam;
import com.anthropic.models.beta.messages.BetaBase64ImageSource;
import com.anthropic.models.beta.messages.BetaBase64PdfSource;
import com.anthropic.models.beta.messages.BetaCacheControlEphemeral;
import com.anthropic.models.beta.messages.BetaContentBlockParam;
import com.anthropic.models.beta.messages.BetaFallbacksParam;
import com.anthropic.models.beta.messages.BetaMessage;
import com.anthropic.models.beta.messages.BetaOutputConfig;
import com.anthropic.models.beta.messages.BetaRawMessageStreamEvent;
import com.anthropic.models.beta.messages.BetaRequestDocumentBlock;
import com.anthropic.models.beta.messages.BetaThinkingBlockBinding;
import com.anthropic.models.beta.messages.BetaThinkingConfigAdaptive;
import com.anthropic.models.beta.messages.BetaThinkingConfigParam;
import com.anthropic.models.beta.messages.BetaThinkingPrefixMismatchBehavior;
import com.anthropic.models.beta.messages.BetaUserMessageParam;
import com.anthropic.models.beta.messages.MessageCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Claude pelo SDK oficial da Anthropic: thinking adaptativo, cache de prompt e, nos modelos que aceitam,
 * {@code fallbacks: "default"} (se o modelo recusar, a própria API tenta outro). A resposta completa (com
 * blocos de thinking) é guardada e reenviada sem alteração enquanto a conversa seguir no Claude.
 */
@Component @Slf4j
@Order(1)
public class LugiaClaudeProvider implements LugiaIaProvider {

    static final Set<String> COM_FALLBACK = new HashSet<>(Arrays.asList(
            "claude-fable-5-1", "claude-opus-5-5", "claude-opus-5", "claude-sonnet-5-5"));

    private final LugiaProperties props;
    private volatile AnthropicClient client;

    public LugiaClaudeProvider(LugiaProperties props) {
        this.props = props;
    }

    @Override
    public String id() { return "claude"; }

    @Override
    public String nome() { return "Claude"; }

    @Override
    public List<String> modelos() { return props.getModelosClaude(); }

    @Override
    public boolean disponivel() {
        return !props.getAnthropicApiKey().isEmpty() && !props.getModelosClaude().isEmpty();
    }

    private AnthropicClient client() {
        if (client == null) {
            synchronized (this) {
                if (client == null) client = AnthropicOkHttpClient.builder().apiKey(props.getAnthropicApiKey()).build();
            }
        }
        return client;
    }

    @Override
    public LugiaResposta conversar(String modelo, String sistema, List<LugiaMensagemDto> historico, Consumer<String> aoTexto) {
        boolean comFallback = COM_FALLBACK.contains(modelo);
        MessageCreateParams.Builder pedido = MessageCreateParams.builder()
                .model(modelo)
                .maxTokens(64000L)
                .system(sistema)
                // Se um bloco de thinking antigo não casar com o histórico, a API o descarta em vez de falhar.
                .thinking(BetaThinkingConfigParam.ofAdaptive(BetaThinkingConfigAdaptive.builder()
                        .blockBinding(BetaThinkingBlockBinding.builder()
                                .prefixMismatchBehavior(BetaThinkingPrefixMismatchBehavior.DROP_BLOCK).build())
                        .build()))
                .outputConfig(BetaOutputConfig.builder().effort(BetaOutputConfig.Effort.MEDIUM).build())
                .cacheControl(BetaCacheControlEphemeral.builder().build())
                .addBeta("thinking-binding-controls-2026-08-01");
        if (comFallback) {
            pedido.addBeta("server-side-fallback-2026-07-01").fallbacks(BetaFallbacksParam.ofDefault());
        }
        adicionarHistorico(pedido, historico);

        BetaMessageAccumulator acumulador = BetaMessageAccumulator.create();
        StringBuilder texto = new StringBuilder();
        try (StreamResponse<BetaRawMessageStreamEvent> stream = client().beta().messages().createStreaming(pedido.build())) {
            stream.stream().forEach(evento -> {
                acumulador.accumulate(evento);
                if (evento.isContentBlockDelta() && evento.asContentBlockDelta().delta().isText()) {
                    String delta = evento.asContentBlockDelta().delta().asText().text();
                    texto.append(delta);
                    aoTexto.accept(delta);
                }
            });
        } catch (RateLimitException e) {
            throw new LugiaFalhaException("Limite de uso do Claude atingido; tente em instantes.", e);
        } catch (UnauthorizedException | PermissionDeniedException e) {
            throw new LugiaFalhaException("Chave do Claude inválida no servidor.", e);
        } catch (BadRequestException e) {
            log.warn("Claude recusou o pedido: {}", e.getMessage());
            throw new LugiaFalhaException("O Claude recusou o pedido (verifique anexos e tamanho da conversa).", e);
        } catch (AnthropicServiceException e) {
            throw new LugiaFalhaException("Falha no Claude (" + e.statusCode() + ").", e);
        } catch (AnthropicIoException e) {
            throw new LugiaFalhaException("Não foi possível falar com o Claude agora.", e);
        }

        BetaMessage mensagem = acumulador.message();
        String motivo = mensagem.stopReason().map(Object::toString).orElse("end_turn");
        if ("refusal".equals(motivo)) {
            String aviso = "\n\n_(O Claude não pôde responder a este pedido.)_";
            texto.append(aviso);
            aoTexto.accept(aviso);
        }
        return new LugiaResposta(texto.toString(), json(mensagem), motivo,
                (int) mensagem.usage().inputTokens(), (int) mensagem.usage().outputTokens());
    }

    private void adicionarHistorico(MessageCreateParams.Builder pedido, List<LugiaMensagemDto> historico) {
        for (LugiaMensagemDto m : historico) {
            if (LugiaMensagemDto.ASSISTENTE.equals(m.getPapel())) {
                BetaMessage nativa = "claude".equals(m.getProvedor()) ? lerNativa(m.getNativo()) : null;
                if (nativa != null && !nativa.content().isEmpty()) {
                    pedido.addMessage(nativa);
                } else {
                    pedido.addMessage(BetaAssistantMessageParam.builder().content(textoOuReticencias(m.getPartes())).build());
                }
                continue;
            }
            List<BetaContentBlockParam> blocos = new ArrayList<>();
            for (LugiaParteDto p : m.getPartes()) {
                if (LugiaParteDto.IMAGEM.equals(p.getTipo())) {
                    blocos.add(BetaContentBlockParam.ofImage(BetaBase64ImageSource.builder()
                            .mediaType(BetaBase64ImageSource.MediaType.of(p.getMime())).data(p.getBase64()).build()));
                } else if (LugiaParteDto.PDF.equals(p.getTipo())) {
                    BetaRequestDocumentBlock.Builder doc = BetaRequestDocumentBlock.builder()
                            .source(BetaBase64PdfSource.builder().data(p.getBase64()).build());
                    if (p.getNome() != null) doc.title(p.getNome());
                    blocos.add(BetaContentBlockParam.ofDocument(doc.build()));
                }
            }
            // Anexos antes do texto, como recomenda a documentação.
            for (LugiaParteDto p : m.getPartes()) {
                if (LugiaParteDto.TEXTO.equals(p.getTipo()) && p.getTexto() != null && !p.getTexto().trim().isEmpty()) {
                    blocos.add(BetaContentBlockParam.ofText(p.getTexto()));
                }
            }
            if (blocos.isEmpty()) blocos.add(BetaContentBlockParam.ofText("…"));
            pedido.addMessage(BetaUserMessageParam.builder().contentOfContentBlockParams(blocos).build());
        }
    }

    static String textoOuReticencias(List<LugiaParteDto> partes) {
        StringBuilder sb = new StringBuilder();
        if (partes != null) {
            for (LugiaParteDto p : partes) {
                if (LugiaParteDto.TEXTO.equals(p.getTipo()) && p.getTexto() != null) {
                    if (sb.length() > 0) sb.append('\n');
                    sb.append(p.getTexto());
                }
            }
        }
        return sb.toString().trim().isEmpty() ? "…" : sb.toString();
    }

    private static BetaMessage lerNativa(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            return ObjectMappers.jsonMapper().readValue(json, BetaMessage.class);
        } catch (Exception e) {
            log.warn("Resposta nativa do Claude ilegível; seguindo só com o texto");
            return null;
        }
    }

    private static String json(BetaMessage mensagem) {
        try {
            return ObjectMappers.jsonMapper().writeValueAsString(mensagem);
        } catch (Exception e) {
            return null;
        }
    }
}
