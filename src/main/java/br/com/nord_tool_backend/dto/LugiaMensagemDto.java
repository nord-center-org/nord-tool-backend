package br.com.nord_tool_backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Mensagem de uma conversa da Lugia. {@code nativo} guarda a resposta original do provedor (JSON), reenviada
 * sem alteração quando a conversa continua no mesmo provedor; nunca vai para a tela.
 */
@Data @AllArgsConstructor @NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LugiaMensagemDto {
    public static final String USUARIO = "usuario";
    public static final String ASSISTENTE = "assistente";

    private String papel;
    private String provedor;
    private List<LugiaParteDto> partes;
    @JsonIgnore
    private String nativo;
}
