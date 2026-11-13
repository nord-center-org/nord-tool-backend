package br.com.nord_tool_backend.form;

import lombok.Data;

import java.math.BigDecimal;

/** Dados de uma música (o áudio vai em outra chamada). Os limites são conferidos no serviço. */
@Data
public class RepertorioMusicaForm {
    private String nmTitulo;
    private String nmArtista;
    private String nmArquivo;
    private BigDecimal nrDuracao;
    private String nmTom;
    private BigDecimal nrBpm;
    private String cdProjeto;
    /** JSON (objeto) com análise, cifra, tempos e ajustes. */
    private String txDados;
}
