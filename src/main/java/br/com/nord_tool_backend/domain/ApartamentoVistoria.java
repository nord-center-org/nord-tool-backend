package br.com.nord_tool_backend.domain;

import lombok.EqualsAndHashCode;
import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@EqualsAndHashCode(callSuper = false)
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApartamentoVistoria extends GlobalDomain implements Serializable{

    private static final long serialVersionUID = 325243634534646L;

    private Long id;
    private String nmApartamentoVistoria;
    private Integer idDiaSemana;
    private String nmDiaSemana;
    private LocalDate dtApartamentoVigente;
    private String nmHorarioVistoria;
    private Integer idStatusVistoria;
    private String nmStatusVistoria;
    private boolean inMarcarRevistoria;
    private String txObservacaoRevistoria;
    private LocalDate dtRevistoriaVigente;

    // Último termo de reprova (agregado da listagem; não é gravado em apartamento_vistoria)
    private boolean inTermoAnexado;
    private Integer qtTermos;
    private Integer nrUltimoTermo;
    private String nmSituacaoTermo;
    private Integer qtFotosTermo;
    private Integer nrPaginasTermo;
    private Integer nrPaginasComFoto;

}
