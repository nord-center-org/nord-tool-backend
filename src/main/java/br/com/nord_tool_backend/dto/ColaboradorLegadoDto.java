package br.com.nord_tool_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Colaborador da base antiga (users) e o usuário a que está vinculado. */
@Data @AllArgsConstructor @NoArgsConstructor
public class ColaboradorLegadoDto {
    private Long idUser;
    private String nmColaborador;
    private String nmEmpresa;
    private String nmCargo;
    private String nrCelular;
    private Long idUsuario;
    private String nmUsuario;
    /** Retiradas de chave em que ele aparece. */
    private Integer qtRetiradas;
}
