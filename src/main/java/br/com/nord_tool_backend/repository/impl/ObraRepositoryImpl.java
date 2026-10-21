package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.domain.Fase;
import br.com.nord_tool_backend.domain.Obra;
import br.com.nord_tool_backend.repository.ObraRepository;
import br.com.nord_tool_backend.repository.jdbc.JdbcExecutor;
import br.com.nord_tool_backend.repository.jdbc.SqlQueries;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class ObraRepositoryImpl implements ObraRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcExecutor executor;
    private final SqlQueries q;

    public ObraRepositoryImpl(NamedParameterJdbcTemplate jdbc, JdbcExecutor executor, SqlQueries queries) {
        this.jdbc = jdbc;
        this.executor = executor;
        this.q = queries;
        // Falha cedo se alguma query sumir do catálogo.
        for (String chave : new String[]{"SPS.OBRA.LISTAR", "SPS.OBRA.BUSCAR", "SPI.OBRA.INSERIR", "SPU.OBRA.ALTERAR",
                "SPS.FASE.LISTAR", "SPS.FASE.BUSCAR", "SPI.FASE.INSERIR", "SPU.FASE.ALTERAR", "SPS.FASE.USUARIOS",
                "SPD.FASE.USUARIOS", "SPI.FASE.USUARIO", "SPS.FASE.CONTAR_USUARIOS"}) {
            queries.get(chave);
        }
    }

    @Override
    public List<Obra> listarObras() {
        return executor.executar("Listar obras", () -> jdbc.query(q.get("SPS.OBRA.LISTAR"), BeanPropertyRowMapper.newInstance(Obra.class)));
    }

    @Override
    public Optional<Obra> buscarObra(Long id) {
        return executor.executar("Buscar obra", () -> jdbc.query(q.get("SPS.OBRA.BUSCAR"), new MapSqlParameterSource("id", id),
                BeanPropertyRowMapper.newInstance(Obra.class)).stream().findFirst());
    }

    @Override
    public Long inserirObra(Obra o) {
        return executor.executar("Salvar obra", () -> jdbc.queryForObject(q.get("SPI.OBRA.INSERIR"), obra(o), Long.class));
    }

    @Override
    public int alterarObra(Obra o, int nrVersao) {
        return executor.executar("Alterar obra", () -> jdbc.update(q.get("SPU.OBRA.ALTERAR"),
                obra(o).addValue("id", o.getId()).addValue("nrVersao", nrVersao)));
    }

    @Override
    public List<Fase> listarFases() {
        return executor.executar("Listar fases", () -> jdbc.query(q.get("SPS.FASE.LISTAR"), BeanPropertyRowMapper.newInstance(Fase.class)));
    }

    @Override
    public Optional<Fase> buscarFase(Long id) {
        return executor.executar("Buscar fase", () -> jdbc.query(q.get("SPS.FASE.BUSCAR"), new MapSqlParameterSource("id", id),
                BeanPropertyRowMapper.newInstance(Fase.class)).stream().findFirst());
    }

    @Override
    public Long inserirFase(Fase f) {
        return executor.executar("Salvar fase", () -> jdbc.queryForObject(q.get("SPI.FASE.INSERIR"), fase(f), Long.class));
    }

    @Override
    public int alterarFase(Fase f, int nrVersao) {
        return executor.executar("Alterar fase", () -> jdbc.update(q.get("SPU.FASE.ALTERAR"),
                fase(f).addValue("id", f.getId()).addValue("nrVersao", nrVersao)));
    }

    @Override
    public Map<Long, List<Long>> usuariosPorFase() {
        return executor.executar("Listar participantes das fases", () -> {
            Map<Long, List<Long>> mapa = new HashMap<>();
            jdbc.query(q.get("SPS.FASE.USUARIOS"), rs -> {
                mapa.computeIfAbsent(rs.getLong("idFase"), k -> new ArrayList<>()).add(rs.getLong("idUsuario"));
            });
            return mapa;
        });
    }

    @Override
    public void substituirUsuariosDaFase(Long idFase, List<Long> idsUsuarios) {
        executor.executar("Salvar participantes da fase", () -> {
            jdbc.update(q.get("SPD.FASE.USUARIOS"), new MapSqlParameterSource("idFase", idFase));
            for (Long id : idsUsuarios) {
                jdbc.update(q.get("SPI.FASE.USUARIO"), new MapSqlParameterSource().addValue("idFase", idFase).addValue("idUsuario", id));
            }
        });
    }

    @Override
    public int contarUsuarios(List<Long> ids) {
        if (ids.isEmpty()) return 0;
        return executor.executar("Conferir usuários", () -> {
            Integer n = jdbc.queryForObject(q.get("SPS.FASE.CONTAR_USUARIOS"), new MapSqlParameterSource("ids", ids), Integer.class);
            return n == null ? 0 : n;
        });
    }

    private static MapSqlParameterSource obra(Obra o) {
        return new MapSqlParameterSource().addValue("nmObra", o.getNmObra()).addValue("cdAcesso", o.getCdAcesso())
                .addValue("inAtivo", !Boolean.FALSE.equals(o.getInAtivo()));
    }

    private static MapSqlParameterSource fase(Fase f) {
        return new MapSqlParameterSource().addValue("idObra", f.getIdObra()).addValue("nmFase", f.getNmFase())
                .addValue("cdFase", f.getCdFase()).addValue("nrOrdem", f.getNrOrdem() == null ? 0 : f.getNrOrdem())
                .addValue("inAtivo", !Boolean.FALSE.equals(f.getInAtivo()));
    }
}
