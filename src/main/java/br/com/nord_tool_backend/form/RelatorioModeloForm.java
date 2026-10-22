package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

@Data
public class RelatorioModeloForm {
    @NotBlank(message = "Informe o nome do modelo")
    @Size(max = 120, message = "O nome do modelo deve ter no máximo 120 caracteres")
    private String nmModelo;

    @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres")
    private String txDescricao;

    @NotNull(message = "Informe quantas fotos por página")
    private Integer nrFotosPagina;

    private Boolean inAtivo;

    /** Obrigatória na edição. */
    private Integer nrVersao;

    @Valid
    @Size(max = 100, message = "O modelo pode ter no máximo 100 itens")
    private List<RelatorioItemForm> itens = new ArrayList<>();
}
