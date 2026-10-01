package br.com.nord_tool_backend.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = false)
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequisicaoChave extends GlobalDomain {
    private Long id;
    private String cdRetirada;
    private LocalDateTime dtRetirada;
    private LocalDateTime dtRecebimento;
    private Long idApartamentoVistoria;
    private Long idFerramenta;
    private String cdTipoItemRequisicao;
    private Long idUserRetirada;
    private Long idUserLiberacao;
    private Long idUserRecebimento;
    private String nmStatusRequisicao;

}
