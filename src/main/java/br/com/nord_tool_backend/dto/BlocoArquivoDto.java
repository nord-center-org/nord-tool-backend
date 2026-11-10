package br.com.nord_tool_backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

/** Planilha ou documento avulso do bloco de notas. Na lista vem sem o conteúdo. */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BlocoArquivoDto {
    private Long id;
    /** PLANILHA ou DOCUMENTO. */
    private String cdTipo;
    private String nmTitulo;
    private String cdCor;
    private Integer nrVersao;
    private LocalDateTime dhCriacao;
    private LocalDateTime dhAlteracao;
    /** JSON das abas (planilha) ou HTML (documento); só com o arquivo aberto. */
    private String conteudo;
}
