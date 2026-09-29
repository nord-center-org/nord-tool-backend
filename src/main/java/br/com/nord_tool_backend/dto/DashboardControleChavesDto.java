package br.com.nord_tool_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardControleChavesDto {
    private Long qtChavesEmCampo;
    private Long qtChavesNoQuadro;
    private Long qtChavesEntregues;
    private List<RetiradaControleChavesDto> retiradasRecentes;
}
