package br.com.nord_tool_backend.service;

import br.com.nord_tool_backend.dto.DrumcamBilheteDto;
import br.com.nord_tool_backend.websocket.CanalSala;

/**
 * Relé do Drumcam pela internet: o celular-câmera e o painel de controle (Repertório → Drumcam) se conectam no mesmo
 * WebSocket, cada um com um bilhete emitido para quem está logado, e o servidor só repassa pedidos e respostas entre
 * as câmeras e os controles da mesma pessoa.
 */
public interface DrumcamService {

    /** Bilhete de uso único (60 s) para abrir o WebSocket como "camera" ou "controle". */
    DrumcamBilheteDto bilhete(String papel, String nome);

    /** Liga a conexão ao bilhete; bilhete inválido, usado ou vencido fecha a conexão. */
    void conectar(String bilhete, CanalSala canal);

    void receber(CanalSala canal, String mensagem);

    void desconectar(CanalSala canal);

    /** Descarta os bilhetes vencidos. */
    void limpar();
}
