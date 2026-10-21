package br.com.nord_tool_backend.form;

import lombok.Data;

@Data
public class VinculoColaboradorForm {
    /** Usuário a vincular; nulo desfaz o vínculo. */
    private Long idUsuario;
}
