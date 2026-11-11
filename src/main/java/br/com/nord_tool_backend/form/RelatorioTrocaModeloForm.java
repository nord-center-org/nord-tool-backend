package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.NotNull;

/** v3: troca o modelo de um relatório já criado (aparência do PDF e, se pedido, itens e layout). */
@Data
public class RelatorioTrocaModeloForm {
    /** Novo modelo; nulo = sem modelo. */
    private Long idModelo;

    @NotNull(message = "Informe a versão (nrVersao)")
    private Integer nrVersao;

    /** Acrescenta no fim os itens do modelo que o relatório ainda não tem (pelo nome). Nada é apagado. */
    private Boolean inAdicionarItens;

    /** Passa a usar o layout da página do modelo. */
    private Boolean inUsarLayout;
}
