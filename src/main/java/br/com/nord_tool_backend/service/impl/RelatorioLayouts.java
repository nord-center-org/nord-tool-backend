package br.com.nord_tool_backend.service.impl;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Layouts de página do PDF do relatório fotográfico (o desenho de cada um fica no frontend).
 * R = retrato, P = paisagem; o número é a quantidade de fotos; L = foto com texto ao lado; M = misto.
 */
final class RelatorioLayouts {

    static final Map<String, Integer> FOTOS;

    static {
        Map<String, Integer> m = new LinkedHashMap<>();
        m.put("R1", 1);
        m.put("R2", 2);
        m.put("R4", 4);
        m.put("R6", 6);
        m.put("R8", 8);
        m.put("R2L", 2);
        m.put("R3L", 3);
        m.put("R4L", 4);
        m.put("R2M", 2);
        m.put("P1L", 1);
        m.put("P2", 2);
        m.put("P3", 3);
        m.put("P4", 4);
        m.put("P6", 6);
        FOTOS = Collections.unmodifiableMap(m);
    }

    private RelatorioLayouts() {
    }

    /** Código validado: cdLayout, ou "R" + nrFotosPagina (formato antigo); null se os dois vierem vazios. */
    static String resolver(String cdLayout, Integer nrFotosPagina) {
        String codigo = cdLayout != null && !cdLayout.trim().isEmpty() ? cdLayout.trim().toUpperCase()
                : nrFotosPagina == null ? null : "R" + nrFotosPagina;
        if (codigo != null && !FOTOS.containsKey(codigo)) {
            throw new br.com.nord_tool_backend.exception.EntradaInvalidaException("Layout de página inválido");
        }
        return codigo;
    }
}
