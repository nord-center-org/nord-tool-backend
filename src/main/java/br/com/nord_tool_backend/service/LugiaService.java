package br.com.nord_tool_backend.service;

import br.com.nord_tool_backend.dto.LugiaConversaDto;
import br.com.nord_tool_backend.dto.LugiaMensagemDto;
import br.com.nord_tool_backend.dto.LugiaModelosDto;
import br.com.nord_tool_backend.form.LugiaChatForm;
import br.com.nord_tool_backend.form.LugiaTituloForm;
import br.com.nord_tool_backend.service.lugia.LugiaSaida;

import java.util.List;

/** Lugia: chat com Claude, ChatGPT e Gemini e, para ADMIN do módulo, o agente que executa pedidos no NordTool. */
public interface LugiaService {

    LugiaModelosDto modelos();

    List<LugiaConversaDto> conversas();

    /** Mensagens para a tela (anexos sem o conteúdo). */
    List<LugiaMensagemDto> mensagens(Long idConversa);

    void renomear(Long idConversa, LugiaTituloForm form);

    void excluir(Long idConversa);

    /** Valida, grava a mensagem e transmite a resposta em {@code saida} (abre a transmissão só depois de validar). */
    void conversar(LugiaChatForm form, LugiaSaida saida);
}
