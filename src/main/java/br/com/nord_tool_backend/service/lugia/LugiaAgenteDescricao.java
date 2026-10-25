package br.com.nord_tool_backend.service.lugia;

import com.anthropic.core.JsonValue;

import java.util.Map;
import java.util.Optional;

/** Linha curta de progresso para cada ferramenta que o agente usa (ex.: "bash: git status"). */
final class LugiaAgenteDescricao {

    private static final int MAX = 140;

    private LugiaAgenteDescricao() {
    }

    static String ferramenta(String nome, Map<String, JsonValue> entrada) {
        String detalhe = primeiro(entrada, "command", "file_path", "path", "pattern", "url", "title", "query");
        if (detalhe == null) return nome;
        String linha = detalhe.replaceAll("\\s+", " ").trim();
        return nome + ": " + (linha.length() > MAX ? linha.substring(0, MAX - 1) + "…" : linha);
    }

    private static String primeiro(Map<String, JsonValue> entrada, String... chaves) {
        if (entrada == null) return null;
        for (String chave : chaves) {
            JsonValue valor = entrada.get(chave);
            if (valor == null) continue;
            Optional<String> texto = valor.asString();
            if (texto.isPresent() && !texto.get().isEmpty()) return texto.get();
        }
        return null;
    }
}
