package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class MapeamentoForm {
    /** CONTAGEM ou QUANTITATIVO; só vale na criação. */
    private String cdTipo;

    @NotBlank(message = "Informe o título")
    @Size(max = 150, message = "O título deve ter no máximo 150 caracteres")
    private String nmTitulo;

    @Size(max = 150, message = "O local deve ter no máximo 150 caracteres")
    private String nmLocal;

    /** yyyy-MM-dd */
    @NotNull(message = "Informe a data")
    private LocalDate dtMapeamento;

    @DecimalMin(value = "0", message = "A tolerância vai de 0 a 100%")
    @DecimalMax(value = "100", message = "A tolerância vai de 0 a 100%")
    private BigDecimal pcTolerancia;

    @Size(max = 2000, message = "A observação deve ter no máximo 2000 caracteres")
    private String txObservacao;

    /** Obrigatória na edição. */
    private Integer nrVersao;

    /** Substitui todas as linhas. Nula = não mexe nas linhas. */
    @Valid
    @Size(max = 500, message = "O mapeamento pode ter no máximo 500 linhas")
    private List<MapeamentoLinhaForm> linhas;
}
