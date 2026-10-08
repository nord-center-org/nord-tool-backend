package br.com.nord_tool_backend.dto;

import br.com.nord_tool_backend.domain.FinanceiroPessoa;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor
public class FinanceiroPessoaDto {
    private Long idPessoa;
    private String nmPessoa;
    private boolean inCompartilhado;
    private Long idUsuario;
    private Integer nrOrdem;
    private boolean inAtivo;

    public static FinanceiroPessoaDto de(FinanceiroPessoa p) {
        return new FinanceiroPessoaDto(p.getId(), p.getNmPessoa(), Boolean.TRUE.equals(p.getInCompartilhado()),
                p.getIdUsuario(), p.getNrOrdem(), !Boolean.FALSE.equals(p.getInAtivo()));
    }
}
