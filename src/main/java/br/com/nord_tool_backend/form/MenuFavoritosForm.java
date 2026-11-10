package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Data
public class MenuFavoritosForm {
    /** Caminhos das telas, na ordem em que aparecem nos favoritos. */
    @NotNull(message = "Informe a lista de favoritos")
    @Size(max = 40, message = "No máximo 40 favoritos")
    private List<String> caminhos;
}
