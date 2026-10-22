package br.com.nord_tool_backend.repository.jdbc;

import org.springframework.jdbc.core.namedparam.AbstractSqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

/** Junta os parâmetros de um objeto (ex.: BeanPropertySqlParameterSource) com parâmetros extras (escopo de fase). */
public class ParametrosComEscopo extends AbstractSqlParameterSource {

    private final SqlParameterSource base;
    private final MapSqlParameterSource extra;

    public ParametrosComEscopo(SqlParameterSource base, MapSqlParameterSource extra) {
        this.base = base;
        this.extra = extra;
    }

    @Override
    public boolean hasValue(String nome) {
        return extra.hasValue(nome) || base.hasValue(nome);
    }

    @Override
    public Object getValue(String nome) {
        return extra.hasValue(nome) ? extra.getValue(nome) : base.getValue(nome);
    }

    @Override
    public int getSqlType(String nome) {
        return extra.hasValue(nome) ? extra.getSqlType(nome) : base.getSqlType(nome);
    }

    @Override
    public String[] getParameterNames() {
        java.util.Set<String> nomes = new java.util.LinkedHashSet<>(java.util.Arrays.asList(extra.getParameterNames()));
        String[] doBase = base.getParameterNames();
        if (doBase != null) nomes.addAll(java.util.Arrays.asList(doBase));
        return nomes.toArray(new String[0]);
    }
}
