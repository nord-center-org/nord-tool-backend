package br.com.nord_tool_backend.service.lugia;

import br.com.nord_tool_backend.dto.LugiaParteDto;

import java.util.List;

/**
 * Agente Claude que executa pedidos no NordTool (código nos repositórios, PR para develop), hospedado pela
 * Anthropic (Managed Agents). Uma sessão por conversa.
 */
public interface LugiaAgenteClient {

    /** Há chave da Anthropic e token do GitHub configurados. */
    boolean disponivel();

    /** Cria a sessão do agente para uma conversa e devolve o id. */
    String criarSessao(String titulo);

    /**
     * Envia o pedido para a sessão e acompanha até o agente terminar, avisando o progresso em {@code saida}
     * (texto e ferramentas). Lança {@link LugiaFalhaException} com mensagem para a pessoa.
     */
    LugiaResposta executar(String idSessao, List<LugiaParteDto> partes, LugiaSaida saida);
}
