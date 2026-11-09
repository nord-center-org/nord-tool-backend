package br.com.nord_tool_backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** Caderno do bloco de notas. Na lista vem sem páginas; aberto, com as páginas (sem o desenho). */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BlocoCadernoDto {
    private Long id;
    private String nmTitulo;
    private String cdCor;
    private Integer nrVersao;
    private LocalDateTime dhCriacao;
    private LocalDateTime dhAlteracao;
    private Long qtPaginas;
    /** Primeira página, para a miniatura da capa. */
    private Long idCapa;
    private List<BlocoPaginaDto> paginas;
}
