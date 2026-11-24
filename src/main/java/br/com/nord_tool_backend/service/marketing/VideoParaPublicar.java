package br.com.nord_tool_backend.service.marketing;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.nio.file.Path;

/** O vídeo já recebido no servidor e o que vai junto em cada rede. */
@Getter
@AllArgsConstructor
public class VideoParaPublicar {
    private final Path arquivo;
    private final long tamanho;
    private final String tipo;
    private final String titulo;
    private final String legenda;
    private final boolean publico;
    /** Endereço público e temporário do vídeo (o Instagram busca o arquivo por ele). */
    private final String urlPublica;
}
