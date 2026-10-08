package br.com.nord_tool_backend.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class FinanceiroCategoria extends GlobalDomain {
    private String nmCategoria;
    /** ENTRADA | SAIDA */
    private String cdTipo;
    /** FIXA_MEDIA | SALDO_ANTERIOR | FIXA_VALOR | RITMO_FATURA | VARIAVEL_MEDIA | MANUAL */
    private String cdProjecao;
    private Boolean inFixa;
    /** DIA_MES | DIA_UTIL (vazio quando a data não segue regra). */
    private String cdRegraData;
    private Integer nrDia;
    private Integer nrOrdem;
    private Boolean inAtivo;
}
