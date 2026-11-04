package br.com.nord_tool_backend.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** O que o SAC tem de novo para a pessoa: chamados dela com resposta não vista e, para o ADMIN, os abertos. */
@Data
public class SacNovidadesDto {
    private List<SacChamadoDto> chamados = new ArrayList<>();
    /** Só para o ADMIN do SAC: chamados de todo mundo ainda abertos. */
    private Integer abertos;
}
