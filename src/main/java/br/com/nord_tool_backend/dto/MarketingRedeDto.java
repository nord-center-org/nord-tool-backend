package br.com.nord_tool_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Uma rede: se o app dela está configurado no servidor e as contas que a pessoa conectou. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketingRedeDto {
    private String cdRede;
    private String nmRede;
    private boolean inConfigurada;
    private List<MarketingContaDto> contas;
}
