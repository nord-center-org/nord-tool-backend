package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.domain.Medicao;
import br.com.nord_tool_backend.domain.MedicaoStatus;
import br.com.nord_tool_backend.repository.MedicaoRepository;
import br.com.nord_tool_backend.repository.jdbc.JdbcExecutor;
import br.com.nord_tool_backend.repository.jdbc.SqlQueries;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

@Repository
public class MedicaoRepositoryImpl implements MedicaoRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcExecutor executor;
    private final br.com.nord_tool_backend.service.ContextoFaseService contexto;
    private final String qListar;
    private final String qBuscar;
    private final String qInserir;
    private final String qAlterar;
    private final String qAlterarStatus;
    private final String qDeletar;
    private final String qStatusListar;

    public MedicaoRepositoryImpl(NamedParameterJdbcTemplate jdbc, JdbcExecutor executor, SqlQueries queries,
                                 br.com.nord_tool_backend.service.ContextoFaseService contexto) {
        this.jdbc = jdbc;
        this.contexto = contexto;
        this.executor = executor;
        this.qListar = queries.get("SPS.MEDICAO.LISTAR");
        this.qBuscar = queries.get("SPS.MEDICAO.BUSCAR");
        this.qInserir = queries.get("SPI.MEDICAO.INSERIR");
        this.qAlterar = queries.get("SPU.MEDICAO.ALTERAR");
        this.qAlterarStatus = queries.get("SPU.MEDICAO.STATUS");
        this.qDeletar = queries.get("SPD.MEDICAO.DELETAR");
        this.qStatusListar = queries.get("SPS.MEDICAO.STATUS.LISTAR");
    }

    @Override
    public List<Medicao> listar() {
        return executor.executar("Erro ao listar medições", () ->
                jdbc.query(qListar, contexto.escopo().aplicar(new MapSqlParameterSource()), BeanPropertyRowMapper.newInstance(Medicao.class)));
    }

    @Override
    public Optional<Medicao> buscar(Long id) {
        return executor.executar("Erro ao buscar medição", () ->
                jdbc.query(qBuscar, contexto.escopo().aplicar(new MapSqlParameterSource("id", id)), BeanPropertyRowMapper.newInstance(Medicao.class))
                        .stream().findFirst());
    }

    @Override
    public Long inserir(Medicao m) {
        return executor.executar("Erro ao salvar medição", () ->
                jdbc.queryForObject(qInserir, valores(m).addValue("idFase", contexto.faseParaCriar(null), java.sql.Types.BIGINT), Long.class));
    }

    @Override
    public int alterar(Medicao m, int nrVersao) {
        return executor.executar("Erro ao alterar medição", () ->
                jdbc.update(qAlterar, contexto.escopo().aplicar(valores(m).addValue("id", m.getId()).addValue("nrVersao", nrVersao))));
    }

    @Override
    public int alterarStatus(Long id, String cdStatus, int nrVersao) {
        return executor.executar("Erro ao alterar o status da medição", () ->
                jdbc.update(qAlterarStatus, contexto.escopo().aplicar(new MapSqlParameterSource().addValue("id", id)
                        .addValue("cdStatus", cdStatus).addValue("nrVersao", nrVersao))));
    }

    @Override
    public int deletar(Long id, int nrVersao) {
        return executor.executar("Erro ao excluir medição", () ->
                jdbc.update(qDeletar, contexto.escopo().aplicar(new MapSqlParameterSource().addValue("id", id).addValue("nrVersao", nrVersao))));
    }

    @Override
    public List<MedicaoStatus> listarStatus() {
        return executor.executar("Erro ao listar status de medição", () ->
                jdbc.query(qStatusListar, BeanPropertyRowMapper.newInstance(MedicaoStatus.class)));
    }

    private static MapSqlParameterSource valores(Medicao m) {
        return new MapSqlParameterSource()
                .addValue("nrMedicao", m.getNrMedicao())
                .addValue("cdContrato", m.getCdContrato())
                .addValue("nmRazaoSocial", m.getNmRazaoSocial())
                .addValue("nrContrato", m.getNrContrato())
                .addValue("dtMedicao", Date.valueOf(m.getDtMedicao()))
                .addValue("vlMedicao", m.getVlMedicao())
                .addValue("txMemorial", m.getTxMemorial())
                .addValue("cdStatus", m.getCdStatus());
    }
}
