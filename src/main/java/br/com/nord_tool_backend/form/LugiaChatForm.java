package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

/** Mensagem para a Lugia. {@code modo}: "chat" (padrão, conversa com o modelo) ou "agente" (executar no NordTool). */
@Data
public class LugiaChatForm {
    private Long idConversa;
    private String modelo;
    @Size(max = 100_000)
    private String texto;
    @Valid
    private List<LugiaAnexoForm> anexos = new ArrayList<>();
    private String modo;
    /** "cadernos": chat dos cadernos (o Bloco de notas da pessoa vai junto no prompt). Vazio: chat normal. */
    @Size(max = 20)
    private String contexto;
    /** No chat dos cadernos: o que está aberto na tela (CADERNO, PLANILHA ou DOCUMENTO) e o id. */
    @Size(max = 20)
    private String tipoAberto;
    private Long idAberto;
}
