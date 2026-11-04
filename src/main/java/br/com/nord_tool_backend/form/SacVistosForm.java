package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Data
public class SacVistosForm {
    @NotNull(message = "Informe os chamados")
    @Size(max = 300, message = "Muitos chamados de uma vez")
    private List<Long> ids;
}
