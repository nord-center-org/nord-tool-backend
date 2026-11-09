package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/** Página nova: na posição (1 = primeira; vazio = no fim), com o fundo e o tamanho; ou cópia de outra página. */
@Data
public class BlocoPaginaForm {
    @Min(value = 1, message = "Posição inválida")
    private Integer nrPosicao;
    private String cdFundo;
    @Min(value = 100, message = "Largura inválida")
    @Max(value = 4000, message = "Largura inválida")
    private Integer nrLargura;
    @Min(value = 100, message = "Altura inválida")
    @Max(value = 4000, message = "Altura inválida")
    private Integer nrAltura;
    /** Duplica esta página (fundo e desenho). */
    private Long idCopiarDe;
}
