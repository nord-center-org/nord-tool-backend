package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Data
public class GameFavoritosForm {
    @NotNull(message = "Informe os favoritos")
    @Size(max = 50, message = "Favoritos demais")
    private List<String> jogos;
}
