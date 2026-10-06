package br.com.nord_tool_backend.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor
public class CasamentoFornecedor extends GlobalDomain {
    private String nmFornecedor;
    private String nmCategoria;
    private String txContato;
    private String nmStatus;
    private BigDecimal vlValor;
    private String txObservacao;
    private LocalDateTime dhAlteracao;
    /** Agregado das consultas de leitura. */
    private Integer qtAnexos;
}
