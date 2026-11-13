package br.com.nord_tool_backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

/** Criação do estúdio do Repertório ("Criar"). Na lista vem sem os dados. */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RepertorioProjetoDto {
    private String cdChave;
    private String nmTitulo;
    private LocalDateTime dhCriacao;
    private LocalDateTime dhAlteracao;
    /** JSON do projeto (BPM, compassos, faixas e notas); só com a criação aberta. */
    private String txDados;
}
