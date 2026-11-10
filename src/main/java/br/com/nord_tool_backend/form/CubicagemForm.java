package br.com.nord_tool_backend.form;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

@Data
public class CubicagemForm {
    @NotBlank(message = "Dê um nome à cubicagem")
    @Size(max = 120, message = "O nome tem no máximo 120 caracteres")
    private String nmTitulo;
    @DecimalMin(value = "0", message = "A perda vai de 0 a 50%")
    @DecimalMax(value = "50", message = "A perda vai de 0 a 50%")
    private BigDecimal nrPerda;
    /** Lista de peças, como a tela monta. */
    private JsonNode pecas;
    /** Obrigatória ao alterar. */
    private Integer nrVersao;
}
