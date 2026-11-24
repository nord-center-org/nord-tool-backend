package br.com.nord_tool_backend.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.time.ZoneId;

/** Uma arte do casamento (convite, save the date…) com o arquivo e quem enviou. */
@Data
public class CasamentoArteDto {
    private Long idArte;
    private Long idArquivo;
    private String nmTitulo;
    private String cdCategoria;
    private String nmArquivo;
    private String nmContentType;
    private Long nrTamanhoBytes;
    private String nmCriador;
    private LocalDateTime dhCriacao;
    private LocalDateTime dhAlteracao;

    /** Epoch millis da criação; usar em ?v= para cache. */
    public Long getNrVersao() {
        return dhCriacao == null ? 0L : dhCriacao.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
