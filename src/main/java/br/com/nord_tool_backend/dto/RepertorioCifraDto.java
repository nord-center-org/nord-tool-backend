package br.com.nord_tool_backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

/** Repertório v12: uma cifra que alguém compartilhou com a pessoa. Na lista vem sem o texto da cifra. */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RepertorioCifraDto {
    private Long id;
    private Long idUsuarioOrigem;
    private String nmUsuarioOrigem;
    private String nmTitulo;
    private String nmArtista;
    private String nmTom;
    private String txMensagem;
    private LocalDateTime dhCriacao;
    /** Quando a pessoa abriu pela primeira vez (null = nova). */
    private LocalDateTime dhLeitura;
    /** O texto da cifra; só com a cifra aberta. */
    private String txCifra;
}
