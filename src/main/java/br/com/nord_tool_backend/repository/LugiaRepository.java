package br.com.nord_tool_backend.repository;

import br.com.nord_tool_backend.dto.LugiaConversaDto;
import br.com.nord_tool_backend.dto.LugiaMensagemDto;

import java.util.List;
import java.util.Optional;

public interface LugiaRepository {

    List<LugiaConversaDto> listarConversas(Long idUsuario);

    /** Conversa do usuário (vazio se não existir ou for de outra pessoa). */
    Optional<LugiaConversaDto> buscarConversa(Long idUsuario, Long idConversa);

    Long criarConversa(Long idUsuario, String titulo);

    boolean renomear(Long idUsuario, Long idConversa, String titulo);

    boolean excluir(Long idUsuario, Long idConversa);

    void definirSessaoAgente(Long idConversa, String sessao);

    List<LugiaMensagemDto> mensagens(Long idConversa);

    void adicionarMensagem(Long idConversa, LugiaMensagemDto mensagem, String modelo, Integer tokensEntrada, Integer tokensSaida);

    Optional<String> lerConfiguracao(String chave);

    void gravarConfiguracao(String chave, String valor);
}
