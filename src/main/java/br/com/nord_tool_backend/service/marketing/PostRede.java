package br.com.nord_tool_backend.service.marketing;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Um vídeo/post da conta, com as métricas que a rede devolve (nulo = a rede não informou). */
@Getter
@AllArgsConstructor
public class PostRede {
    private final String id;
    private final String titulo;
    private final String miniatura;
    private final String link;
    private final String data;
    private final Long views;
    private final Long curtidas;
    private final Long comentarios;
}
