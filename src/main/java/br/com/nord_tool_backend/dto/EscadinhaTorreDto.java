package br.com.nord_tool_backend.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.time.LocalDateTime;

/** Torre da escadinha como o frontend usa: config e ciclos já como JSON (a prancha vem em rota própria). */
@Data
public class EscadinhaTorreDto {
    private Long idTorre;
    private String nmTorre;
    private Integer nrOrdem;
    private JsonNode config;
    private JsonNode ciclos;
    private Boolean temPlanta;
    private Integer nrVersao;
    private LocalDateTime dhAtualizacao;
}
