package br.com.nord_tool_backend.security;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Fases que a requisição atual pode ver. Use nas queries a condição
 * {@code (:todasFases OR x.id_fase = ANY(CAST(string_to_array(:fasesCsv, ',') AS bigint[])) OR (:verSemFase AND x.id_fase IS NULL))}.
 */
public final class EscopoFase {

    /** Nenhuma fase cadastrada ainda: tudo segue visível como antes da configuração. */
    private final boolean todas;
    private final List<Long> fases;
    /** Registros antigos sem fase (só o admin vê). */
    private final boolean verSemFase;

    public EscopoFase(boolean todas, List<Long> fases, boolean verSemFase) {
        this.todas = todas;
        this.fases = fases == null ? Collections.emptyList() : Collections.unmodifiableList(fases);
        this.verSemFase = verSemFase;
    }

    public static EscopoFase livre() {
        return new EscopoFase(true, Collections.emptyList(), true);
    }

    public boolean isTodas() { return todas; }
    public List<Long> getFases() { return fases; }
    public boolean isVerSemFase() { return verSemFase; }

    public boolean permite(Long idFase) {
        return todas || (idFase == null ? verSemFase : fases.contains(idFase));
    }

    public String csv() {
        return fases.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    /** Acrescenta :todasFases, :fasesCsv e :verSemFase. */
    public MapSqlParameterSource aplicar(MapSqlParameterSource params) {
        return params.addValue("todasFases", todas).addValue("fasesCsv", csv()).addValue("verSemFase", verSemFase);
    }
}
