package br.com.nord_tool_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RetiradaControleChavesDto {
    private Long id;
    private String codigo;
    private ApartamentoControleChavesDto apartamento;
    private PessoaControleChavesDto retirante;
    private PessoaControleChavesDto liberador;
    private PessoaControleChavesDto recebedor;
    private String dataRetirada;
    private String dataRecebimento;
    private String status;
}
