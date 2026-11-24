package br.com.nord_tool_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.nio.file.Path;

/** Vídeo servido pelo link público e temporário (o Instagram busca por ele). */
@Getter
@AllArgsConstructor
public class MarketingMidiaPublicaDto {
    private final Path arquivo;
    private final String tipo;
    private final long tamanho;
}
