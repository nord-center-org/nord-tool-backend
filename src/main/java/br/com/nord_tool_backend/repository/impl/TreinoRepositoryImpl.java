package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.dto.TreinoDto;
import br.com.nord_tool_backend.dto.TreinoExercicioDto;
import br.com.nord_tool_backend.dto.TreinoSessaoDto;
import br.com.nord_tool_backend.dto.TreinoSessaoExercicioDto;
import br.com.nord_tool_backend.repository.TreinoRepository;
import br.com.nord_tool_backend.repository.jdbc.JdbcExecutor;
import br.com.nord_tool_backend.repository.jdbc.SqlQueries;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Types;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
public class TreinoRepositoryImpl implements TreinoRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcExecutor executor;
    private final SqlQueries queries;

    public TreinoRepositoryImpl(NamedParameterJdbcTemplate jdbc, JdbcExecutor executor, SqlQueries queries) {
        this.jdbc = jdbc;
        this.executor = executor;
        this.queries = queries;
        queries.get("SPS.TREINO.LISTAR");
    }

    private String q(String chave) {
        return queries.get(chave);
    }

    private static MapSqlParameterSource dono(Long idDono) {
        return new MapSqlParameterSource("idDono", idDono);
    }

    @Override
    public List<TreinoDto> listar(Long idDono) {
        return executor.executar("Erro ao listar treinos", () ->
                jdbc.query(q("SPS.TREINO.LISTAR"), dono(idDono).addValue("id", null, Types.BIGINT), BeanPropertyRowMapper.newInstance(TreinoDto.class)));
    }

    @Override
    public Optional<TreinoDto> buscar(Long idDono, Long id) {
        return executor.executar("Erro ao buscar treino", () ->
                jdbc.query(q("SPS.TREINO.LISTAR"), dono(idDono).addValue("id", id, Types.BIGINT), BeanPropertyRowMapper.newInstance(TreinoDto.class))
                        .stream().findFirst());
    }

    @Override
    public List<TreinoExercicioDto> listarExercicios(Long idDono) {
        return executor.executar("Erro ao listar exercícios", () ->
                jdbc.query(q("SPS.TREINO.EXERCICIOS"), dono(idDono), BeanPropertyRowMapper.newInstance(TreinoExercicioDto.class)));
    }

    @Override
    public Long inserir(Long idDono, TreinoDto t) {
        return executor.executar("Erro ao salvar treino", () -> jdbc.queryForObject(q("SPI.TREINO.INSERIR"), valores(idDono, t), Long.class));
    }

    @Override
    public int alterar(Long idDono, TreinoDto t, int nrVersao) {
        return executor.executar("Erro ao alterar treino", () ->
                jdbc.update(q("SPU.TREINO.ALTERAR"), valores(idDono, t).addValue("id", t.getId()).addValue("nrVersao", nrVersao)));
    }

    @Override
    public int deletar(Long idDono, Long id, int nrVersao) {
        return executor.executar("Erro ao excluir treino", () ->
                jdbc.update(q("SPD.TREINO.DELETAR"), dono(idDono).addValue("id", id).addValue("nrVersao", nrVersao)));
    }

    @Override
    public void inserirExercicio(TreinoExercicioDto e) {
        executor.executar("Erro ao salvar exercício", () -> {
            jdbc.update(q("SPI.TREINO.EXERCICIO.INSERIR"), valores(e));
        });
    }

    @Override
    public int alterarExercicio(TreinoExercicioDto e) {
        return executor.executar("Erro ao alterar exercício", () -> jdbc.update(q("SPU.TREINO.EXERCICIO.ALTERAR"), valores(e).addValue("id", e.getId())));
    }

    @Override
    public int deletarExercicio(Long idTreino, Long idExercicio) {
        return executor.executar("Erro ao excluir exercício", () ->
                jdbc.update(q("SPD.TREINO.EXERCICIO.DELETAR"), new MapSqlParameterSource("id", idExercicio).addValue("idTreino", idTreino)));
    }

    @Override
    public int atualizarCarga(Long idDono, Long idExercicio, BigDecimal vlCarga) {
        return executor.executar("Erro ao atualizar carga", () ->
                jdbc.update(q("SPU.TREINO.EXERCICIO.CARGA"), dono(idDono).addValue("id", idExercicio).addValue("vlCarga", vlCarga, Types.NUMERIC)));
    }

    @Override
    public List<TreinoSessaoDto> listarSessoes(Long idDono, int limite) {
        return executor.executar("Erro ao listar sessões de treino", () ->
                jdbc.query(q("SPS.TREINO.SESSOES"), dono(idDono).addValue("limite", limite), BeanPropertyRowMapper.newInstance(TreinoSessaoDto.class)));
    }

    @Override
    public List<TreinoSessaoExercicioDto> listarExerciciosDasSessoes(Long idDono, List<Long> idsSessoes) {
        if (idsSessoes.isEmpty()) return Collections.emptyList();
        return executor.executar("Erro ao listar exercícios das sessões", () ->
                jdbc.query(q("SPS.TREINO.SESSAO.EXERCICIOS"), dono(idDono).addValue("ids", idsSessoes),
                        BeanPropertyRowMapper.newInstance(TreinoSessaoExercicioDto.class)));
    }

    @Override
    public Long inserirSessao(Long idDono, TreinoSessaoDto s) {
        return executor.executar("Erro ao salvar sessão de treino", () ->
                jdbc.queryForObject(q("SPI.TREINO.SESSAO.INSERIR"), dono(idDono)
                        .addValue("idTreino", s.getIdTreino(), Types.BIGINT).addValue("nmTreino", s.getNmTreino())
                        .addValue("dtSessao", Date.valueOf(s.getDtSessao())).addValue("nrDuracaoMin", s.getNrDuracaoMin(), Types.SMALLINT)
                        .addValue("txObservacao", s.getTxObservacao()), Long.class));
    }

    @Override
    public void inserirExercicioDaSessao(TreinoSessaoExercicioDto e) {
        executor.executar("Erro ao salvar exercício da sessão", () -> {
            jdbc.update(q("SPI.TREINO.SESSAO.EXERCICIO.INSERIR"), new MapSqlParameterSource()
                    .addValue("idSessao", e.getIdSessao()).addValue("nrOrdem", e.getNrOrdem())
                    .addValue("idExercicio", e.getIdExercicio(), Types.BIGINT).addValue("nmExercicio", e.getNmExercicio())
                    .addValue("txRepeticoes", e.getTxRepeticoes()).addValue("vlCarga", e.getVlCarga(), Types.NUMERIC)
                    .addValue("inFeito", !Boolean.FALSE.equals(e.getInFeito())));
        });
    }

    @Override
    public int deletarSessao(Long idDono, Long id) {
        return executor.executar("Erro ao excluir sessão", () -> jdbc.update(q("SPD.TREINO.SESSAO.DELETAR"), dono(idDono).addValue("id", id)));
    }

    private static MapSqlParameterSource valores(Long idDono, TreinoDto t) {
        return dono(idDono).addValue("nmTreino", t.getNmTreino()).addValue("txObservacao", t.getTxObservacao())
                .addValue("txDias", t.getTxDias()).addValue("nrOrdem", t.getNrOrdem()).addValue("inAtivo", t.getInAtivo());
    }

    private static MapSqlParameterSource valores(TreinoExercicioDto e) {
        return new MapSqlParameterSource().addValue("idTreino", e.getIdTreino()).addValue("nrOrdem", e.getNrOrdem())
                .addValue("nmExercicio", e.getNmExercicio()).addValue("nrSeries", e.getNrSeries())
                .addValue("txRepeticoes", e.getTxRepeticoes()).addValue("vlCarga", e.getVlCarga(), Types.NUMERIC)
                .addValue("nrDescansoSeg", e.getNrDescansoSeg(), Types.SMALLINT).addValue("txObservacao", e.getTxObservacao());
    }
}
