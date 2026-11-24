package br.com.nord_tool_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Endereço do login da rede para conectar a conta. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketingConexaoDto {
    private String txUrl;
}
