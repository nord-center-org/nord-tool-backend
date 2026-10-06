package br.com.nord_tool_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor
public class CasamentoConfiguracaoDto {
    private String casal;
    /** yyyy-MM-dd */
    private String dataCasamento;
}
