package br.com.nord_tool_backend.service;

import br.com.nord_tool_backend.dto.GameSalaDto;
import br.com.nord_tool_backend.websocket.CanalSala;

public interface GameSalaService {

    /** Abre uma sala para um jogo; quem cria hospeda (vaga 0, a tela grande). */
    GameSalaDto criar(String jogo);

    /** Entra (ou volta) numa sala pelo código; a mesma pessoa volta sempre para a mesma vaga. */
    GameSalaDto entrar(String codigo);

    /** Liga a conexão ao bilhete; bilhete inválido, usado ou vencido fecha a conexão. */
    void conectar(String bilhete, CanalSala canal);

    void receber(CanalSala canal, String mensagem);

    void desconectar(CanalSala canal);

    /** Fecha as salas vazias ou antigas. */
    void limpar();
}
