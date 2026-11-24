package br.com.nord_tool_backend.service.marketing;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** O que a rede respondeu ao publicar: id e link do post e se ficou público ou só privado (com o motivo). */
@Getter
@AllArgsConstructor
public class ResultadoPublicacao {
    private final String idExterno;
    private final String link;
    private final boolean publico;
    /** Explica por que ficou privado (ex.: app ainda sem auditoria); nulo quando saiu como pedido. */
    private final String aviso;
}
