package br.com.nord_tool_backend.domain;

import lombok.EqualsAndHashCode;
import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = false)
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CronogramaSemanal extends GlobalDomain {
    private Long id;
    private Integer idDiaSemana;
    private String nmDiaSemana;
    private String nmCronogramaSemanal;
    private String nmHorario;
    private String nmCronogramaCategoria;
    private String nmStatusCronograma;
    private String txObservacao;
    private LocalDateTime dtPrazo;
    private LocalDateTime dtFinalizacao;
    private String nmTag;
    private LocalDateTime dtAgendamento;
    private Boolean inCronogramaFixo;
    /** Cronograma pessoal: dono do item (null = item antigo, de antes do cronograma pessoal). */
    private Long idDono;
    private String nmDono;
    /** O usuário logado pode alterar (é o dono, ou Admin do módulo num item sem dono). */
    private Boolean inProprio;
    /** Ids dos usuários com quem o item foi compartilhado, separados por vírgula. */
    private String txCompartilhados;
}
