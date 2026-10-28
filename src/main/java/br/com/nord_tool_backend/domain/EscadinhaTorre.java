package br.com.nord_tool_backend.domain;

import lombok.Data;

import java.time.LocalDateTime;

/** Torre da escadinha de uma obra: config (croqui, pavimentos, apartamentos, lados) e ciclos em JSON. */
@Data
public class EscadinhaTorre {
    private Long idTorre;
    private Long idObra;
    private String nmTorre;
    private Integer nrOrdem;
    private String txConfig;
    private String txCiclos;
    private Boolean temPlanta;
    private Integer nrVersao;
    private LocalDateTime dhAtualizacao;
}
