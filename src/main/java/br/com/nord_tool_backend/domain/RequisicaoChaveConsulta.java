package br.com.nord_tool_backend.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequisicaoChaveConsulta {
    private Long idRequisicao;
    private String cdRetirada;
    private LocalDateTime dtRetirada;
    private LocalDateTime dtRecebimento;
    private Long idApartamentoVistoria;
    private String nmApartamentoVistoria;
    private Long idFerramenta;
    private String nmFerramenta;
    private String cdTipoItemRequisicao;
    private Long idUserRetirada;
    private String nmPessoaRetirante;
    private String nmPermissaoRetirante;
    private Long idUserLiberacao;
    private String nmPessoaLiberador;
    private String nmPermissaoLiberador;
    private Long idUserRecebimento;
    private String nmPessoaRecebedor;
    private String nmPermissaoRecebedor;
    private String nmStatusRequisicao;
}
