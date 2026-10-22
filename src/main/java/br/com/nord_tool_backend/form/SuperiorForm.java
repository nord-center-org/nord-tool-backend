package br.com.nord_tool_backend.form;

import lombok.Data;

@Data
public class SuperiorForm {
    /** Superior direto; nulo tira o superior. */
    private Long idSuperior;
}
