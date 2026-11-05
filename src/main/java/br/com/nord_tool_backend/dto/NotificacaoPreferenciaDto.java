package br.com.nord_tool_backend.dto;

import lombok.Data;

/** O que a pessoa quer receber no celular. */
@Data
public class NotificacaoPreferenciaDto {
    private Long idUsuario;
    private boolean inDemandaNova = true;
    private boolean inDemandaAlterada = true;
    private boolean inHorario = true;
    /** Minutos antes do horário agendado. */
    private int nrAntecedenciaMin = 30;
    private boolean inPrazo = true;
    private boolean inSac = true;
    /** Avisar também do que a própria pessoa fez (ex.: criou no computador e quer no celular). */
    private boolean inMinhasAcoes = true;
}
