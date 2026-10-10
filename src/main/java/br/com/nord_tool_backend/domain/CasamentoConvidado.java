package br.com.nord_tool_backend.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class CasamentoConvidado extends GlobalDomain {
    private String nmConvidado;
    private String nmGrupo;
    private String nrTelefone;
    private String nmRelacao;
    private String nmStatus;
    private Integer nrAcompanhantes;
    private String nmMesa;
    /** Convidado que este acompanha (família); nulo se for um convidado principal. */
    private Long idConvidadoPrincipal;
    /** Papel no cortejo nupcial (padrinho, madrinha, daminha...); nulo = não faz parte. */
    private String nmCortejo;
    /** Vem do join com o principal. */
    private String nmConvidadoPrincipal;
}
