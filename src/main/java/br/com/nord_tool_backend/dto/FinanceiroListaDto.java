package br.com.nord_tool_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Extrato: lançamentos filtrados, seus totais e os cadastros usados nos filtros e no formulário. */
@Data @AllArgsConstructor @NoArgsConstructor
public class FinanceiroListaDto {
    private List<FinanceiroLancamentoDto> lancamentos;
    private FinanceiroResumoDto resumo;
    private List<FinanceiroPessoaDto> pessoas;
    private List<FinanceiroCategoriaDto> categorias;
}
