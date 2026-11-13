package br.com.nord_tool_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Quanto do espaço do Repertório a pessoa já usa. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepertorioUsoDto {
    private long bytesUsados;
    private long bytesCota;
    private int musicas;
    private int musicasMax;
    private long bytesPorArquivo;
}
