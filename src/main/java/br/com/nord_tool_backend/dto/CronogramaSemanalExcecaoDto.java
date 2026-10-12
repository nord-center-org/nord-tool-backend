package br.com.nord_tool_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CronogramaSemanalExcecaoDto {
    private Long idCronogramaSemanal;
    /** Data no formato yyyy-MM-dd. */
    private String dtExcecao;
}
