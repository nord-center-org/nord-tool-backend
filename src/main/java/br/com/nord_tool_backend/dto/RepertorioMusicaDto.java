package br.com.nord_tool_backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Música do Repertório guardada na conta. Na lista vem sem os dados (análise, cifra, ajustes). */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RepertorioMusicaDto {
    /** Id que o aparelho deu à música (m...). */
    private String cdChave;
    private String nmTitulo;
    private String nmArtista;
    private String nmArquivo;
    private Long nrTamanhoBytes;
    private BigDecimal nrDuracao;
    private String nmTom;
    private BigDecimal nrBpm;
    /** Música gerada no estúdio: a criação de onde o áudio é refeito. */
    private String cdProjeto;
    /** O áudio está guardado na conta (id em arquivo_armazenado, para o cache do navegador). */
    private Long idArquivo;
    private LocalDateTime dhCriacao;
    private LocalDateTime dhAlteracao;
    /** JSON do aparelho (análise, cifra, tempos e ajustes); só com a música aberta. */
    private String txDados;
}
