package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class BlocoArquivoForm {
    /** PLANILHA ou DOCUMENTO (só ao criar). */
    private String cdTipo;
    @Size(min = 1, max = 120, message = "O nome tem de 1 a 120 caracteres")
    private String nmTitulo;
    @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Cor inválida")
    private String cdCor;
    /** Conteúdo do editor; nulo mantém o que está salvo. */
    private String conteudo;
    /** Obrigatória ao alterar. */
    private Integer nrVersao;
}
