package br.com.nord_tool_backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
    /** v16: quem criou a criação (a aba é aberta a todos; só quem criou exclui). */
    private String nmCriador;
    /** v16: quem mexeu por último (null se ninguém além de quem criou). */
    private String nmAlterador;
    /** v16: a criação foi criada por quem está vendo. */
    private Boolean inMinha;
    /** v16: id de quem criou (para a cota de espaço); não vai para a tela. */
    @JsonIgnore
    private Long idUsuario;
    private LocalDateTime dhAlteracao;
    /** JSON do projeto (BPM, compassos, faixas e notas); só com a criação aberta. */
    private String txDados;
}
