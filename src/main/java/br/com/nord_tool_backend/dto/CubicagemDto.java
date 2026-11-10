package br.com.nord_tool_backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Cubicagem de um projeto. Na lista vem sem as peças; aberta, com as peças (JSON da tela). */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CubicagemDto {
    private Long id;
    private String nmTitulo;
    private BigDecimal nrPerda;
    private Integer nrVersao;
    private LocalDateTime dhCriacao;
    private LocalDateTime dhAlteracao;
    /** Quantas peças (na lista). */
    private Integer qtPecas;
    /** Texto guardado no banco; vai para a tela como {@link #pecas}. */
    @JsonIgnore
    private String txPecas;
    private JsonNode pecas;
}
