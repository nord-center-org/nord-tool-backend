package br.com.nord_tool_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @AllArgsConstructor @NoArgsConstructor
public class MedicaoListaDto {
    private List<MedicaoDto> medicoes;
    private List<MedicaoStatusDto> status;
}
