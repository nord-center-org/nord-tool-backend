package br.com.nord_tool_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Os dados da própria pessoa (tela Meus dados). Cargo e e-mail só aparecem: o cargo dá acessos e é do administrador. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MeusDadosDto {
    private String nmNome;
    private String nmEmail;
    private String nrCelular;
    private Integer idEmpresa;
    private String nmEmpresa;
    private String nmCargo;
}
