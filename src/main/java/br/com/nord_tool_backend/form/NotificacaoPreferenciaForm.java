package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@Data
public class NotificacaoPreferenciaForm {
    private boolean inDemandaNova;
    private boolean inDemandaAlterada;
    private boolean inHorario;
    @Min(value = 0, message = "Antecedência inválida")
    @Max(value = 120, message = "Antecedência de no máximo 2 horas")
    private int nrAntecedenciaMin;
    private boolean inPrazo;
    private boolean inSac;
    private boolean inMinhasAcoes;
}
