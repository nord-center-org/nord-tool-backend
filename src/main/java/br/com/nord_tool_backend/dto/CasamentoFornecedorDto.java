package br.com.nord_tool_backend.dto;

import br.com.nord_tool_backend.domain.CasamentoFornecedor;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data @AllArgsConstructor @NoArgsConstructor
public class CasamentoFornecedorDto {
    private Long idFornecedor;
    private String nmFornecedor;
    private String nmCategoria;
    private String txContato;
    /** PESQUISANDO | ORCAMENTO | CONTRATADO */
    private String nmStatus;
    private BigDecimal vlValor;
    private String txObservacao;
    private Integer qtAnexos;

    public static CasamentoFornecedorDto de(CasamentoFornecedor f) {
        return new CasamentoFornecedorDto(f.getId(), f.getNmFornecedor(), f.getNmCategoria(), f.getTxContato(),
                f.getNmStatus(), f.getVlValor(), f.getTxObservacao(), f.getQtAnexos() == null ? 0 : f.getQtAnexos());
    }
}
