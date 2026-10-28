package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.domain.CronogramaEquipe;
import br.com.nord_tool_backend.repository.CronogramaEquipeRepository;
import br.com.nord_tool_backend.repository.jdbc.JdbcExecutor;
import br.com.nord_tool_backend.repository.jdbc.SqlQueries;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Repository
@Transactional
public class CronogramaEquipeRepositoryImpl implements CronogramaEquipeRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcExecutor executor;
    private final SqlQueries q;

    public CronogramaEquipeRepositoryImpl(NamedParameterJdbcTemplate jdbc, JdbcExecutor executor, SqlQueries queries) {
        this.jdbc = jdbc;
        this.executor = executor;
        this.q = queries;
    }

    @Override
    public List<CronogramaEquipe> listar(Long idObra, Long idUsuario, boolean verTudo) {
        return executor.executar("Listar equipes do cronograma", () -> jdbc.query(q.get("SPS.CRONOGRAMA_EQUIPE.LISTAR"),
                new MapSqlParameterSource("idObra", idObra).addValue("idUsuario", idUsuario).addValue("verTudo", verTudo),
                BeanPropertyRowMapper.newInstance(CronogramaEquipe.class)));
    }

    @Override
    public Long criar(Long idObra, String nmEquipe, Long idLider) {
        return executor.executar("Criar equipe do cronograma", () -> jdbc.queryForObject(q.get("SPI.CRONOGRAMA_EQUIPE.INSERIR"),
                new MapSqlParameterSource("idObra", idObra).addValue("nmEquipe", nmEquipe).addValue("idLider", idLider), Long.class));
    }

    @Override
    public int alterar(Long id, Long idObra, String nmEquipe, Long idLider) {
        return executor.executar("Alterar equipe do cronograma", () -> jdbc.update(q.get("SPU.CRONOGRAMA_EQUIPE.ALTERAR"),
                new MapSqlParameterSource("id", id).addValue("idObra", idObra).addValue("nmEquipe", nmEquipe).addValue("idLider", idLider)));
    }

    @Override
    public int excluir(Long id, Long idObra) {
        return executor.executar("Excluir equipe do cronograma", () -> jdbc.update(q.get("SPD.CRONOGRAMA_EQUIPE.EXCLUIR"),
                new MapSqlParameterSource("id", id).addValue("idObra", idObra)));
    }

    @Override
    public boolean nomeEmUso(Long idObra, String nmEquipe, Long idIgnorar) {
        return executor.executar("Conferir nome da equipe", () -> {
            Integer n = jdbc.queryForObject(q.get("SPS.CRONOGRAMA_EQUIPE.NOME_EM_USO"), new MapSqlParameterSource("idObra", idObra)
                    .addValue("nmEquipe", nmEquipe).addValue("id", idIgnorar == null ? -1L : idIgnorar), Integer.class);
            return n != null && n > 0;
        });
    }

    @Override
    public void substituirMembros(Long id, Collection<Long> idsUsuarios) {
        executor.executar("Salvar membros da equipe", () -> {
            jdbc.update(q.get("SPD.CRONOGRAMA_EQUIPE.MEMBROS"), new MapSqlParameterSource("id", id));
            for (Long u : idsUsuarios) {
                jdbc.update(q.get("SPI.CRONOGRAMA_EQUIPE.MEMBRO"), new MapSqlParameterSource("id", id).addValue("idUsuario", u));
            }
        });
    }

    @Override
    public List<Long> hierarquia(Long idLider) {
        return executor.executar("Montar equipe pela hierarquia", () ->
                jdbc.queryForList(q.get("SPS.CRONOGRAMA_EQUIPE.HIERARQUIA"), new MapSqlParameterSource("idLider", idLider), Long.class));
    }
}
