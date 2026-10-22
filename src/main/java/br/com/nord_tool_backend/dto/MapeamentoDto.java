package br.com.nord_tool_backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Mapeamento (contagem de campo ou quantitativo). Na listagem vem sem linhas. */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MapeamentoDto {
    private Long id;
    /** CONTAGEM ou QUANTITATIVO. */
    private String cdTipo;
    private String nmTitulo;
    private String nmLocal;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate dtMapeamento;
    private BigDecimal pcTolerancia;
    private String txObservacao;
    /** RASCUNHO ou CONCLUIDO. */
    private String cdStatus;
    private Long idFase;
    private String nmFase;
    private Long idAutor;
    private String nmAutor;
    private Long qtLinhas;
    private Integer nrVersao;
    private List<MapeamentoLinhaDto> linhas;
}
