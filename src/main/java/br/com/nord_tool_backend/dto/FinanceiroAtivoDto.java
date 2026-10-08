package br.com.nord_tool_backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Um fundo com a posição, a cotação, o resultado e os proventos a receber. */
@Data @AllArgsConstructor @NoArgsConstructor
public class FinanceiroAtivoDto {
    private Long idAtivo;
    private String cdTicker;
    private String nmAtivo;
    private Long idPessoa;
    private String nmPessoa;
    private int qtCotas;
    private BigDecimal vlPrecoMedio;
    /** Quanto foi investido nas cotas que ainda estão na carteira. */
    private BigDecimal vlInvestido;
    private BigDecimal vlCotacao;
    /** Quando a cotação foi lida, dd/MM/yyyy HH:mm. */
    private String dhCotacao;
    /** Falso quando o provedor não respondeu e vale a última cotação guardada. */
    private boolean cotacaoAoVivo;
    private BigDecimal vlPatrimonio;
    private BigDecimal vlResultado;
    /** Resultado sobre o investido, em %. */
    private BigDecimal pcResultado;
    /** Proventos com pagamento de hoje em diante (cotas na data-com x valor por cota). */
    private BigDecimal vlAReceber;
    private List<Operacao> operacoes;
    private List<Provento> proventos;
    private Integer nrVersao;

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class Operacao {
        private Long idOperacao;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy", locale = "pt_BR")
        private LocalDate dtOperacao;
        /** COMPRA | VENDA */
        private String cdTipo;
        private int qtCotas;
        private BigDecimal vlPreco;
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class Provento {
        private Long idProvento;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy", locale = "pt_BR")
        private LocalDate dtCom;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy", locale = "pt_BR")
        private LocalDate dtPagamento;
        private BigDecimal vlPorCota;
        /** Cotas que a pessoa tinha ao fim da data-com. */
        private int qtCotas;
        private BigDecimal vlTotal;
        /** Pagamento já passou. */
        private boolean recebido;
        /** MANUAL | COTACAO */
        private String cdOrigem;
    }
}
