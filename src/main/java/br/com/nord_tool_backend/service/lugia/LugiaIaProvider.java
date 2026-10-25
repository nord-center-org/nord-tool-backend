package br.com.nord_tool_backend.service.lugia;

import br.com.nord_tool_backend.dto.LugiaMensagemDto;

import java.util.List;
import java.util.function.Consumer;

/** Um provedor de IA da Lugia (Claude, ChatGPT, Gemini). Fica disponível com chave e ao menos um modelo. */
public interface LugiaIaProvider {

    /** Identificador gravado nas mensagens: claude, openai ou gemini. */
    String id();

    /** Nome exibido no seletor. */
    String nome();

    List<String> modelos();

    boolean disponivel();

    /**
     * Gera a resposta para o histórico (a última mensagem é a do usuário), transmitindo o texto em pedaços.
     * Lança {@link LugiaFalhaException} com mensagem para a pessoa.
     */
    LugiaResposta conversar(String modelo, String sistema, List<LugiaMensagemDto> historico, Consumer<String> aoTexto);
}
