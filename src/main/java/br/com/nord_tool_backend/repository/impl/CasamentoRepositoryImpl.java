package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.controller.response.NordHttpEnum;
import br.com.nord_tool_backend.domain.CasamentoAnexo;
import br.com.nord_tool_backend.domain.CasamentoConvidado;
import br.com.nord_tool_backend.domain.CasamentoFornecedor;
import br.com.nord_tool_backend.domain.CasamentoMarco;
import br.com.nord_tool_backend.dto.CasamentoTotaisDto;
import br.com.nord_tool_backend.excepetion.ValidacaoException;
import br.com.nord_tool_backend.repository.CasamentoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@Repository @Slf4j @RequiredArgsConstructor
@PropertySource("classpath:query/casamento.properties")
public class CasamentoRepositoryImpl implements CasamentoRepository {

    private final NamedParameterJdbcTemplate jdbc;

    @Value("${SPS.CASAMENTO.CONFIGURACAO.LISTAR}") private String qConfigListar;
    @Value("${SPI.CASAMENTO.CONFIGURACAO.SALVAR}") private String qConfigSalvar;

    @Value("${SPS.CASAMENTO.FORNECEDOR.LISTAR}") private String qFornecedorListar;
    @Value("${SPS.CASAMENTO.FORNECEDOR.BUSCAR}") private String qFornecedorBuscar;
    @Value("${SPI.CASAMENTO.FORNECEDOR.INSERIR}") private String qFornecedorInserir;
    @Value("${SPU.CASAMENTO.FORNECEDOR.ALTERAR}") private String qFornecedorAlterar;
    @Value("${SPD.CASAMENTO.FORNECEDOR.DELETAR}") private String qFornecedorDeletar;

    @Value("${SPS.CASAMENTO.ANEXO.LISTAR}") private String qAnexoListar;
    @Value("${SPS.CASAMENTO.ANEXO.BUSCAR}") private String qAnexoBuscar;
    @Value("${SPI.CASAMENTO.ANEXO.INSERIR}") private String qAnexoInserir;
    @Value("${SPD.CASAMENTO.ANEXO.DELETAR}") private String qAnexoDeletar;

    @Value("${SPS.CASAMENTO.CONVIDADO.LISTAR}") private String qConvidadoListar;
    @Value("${SPS.CASAMENTO.CONVIDADO.BUSCAR}") private String qConvidadoBuscar;
    @Value("${SPI.CASAMENTO.CONVIDADO.INSERIR}") private String qConvidadoInserir;
    @Value("${SPU.CASAMENTO.CONVIDADO.ALTERAR}") private String qConvidadoAlterar;
    @Value("${SPD.CASAMENTO.CONVIDADO.DELETAR}") private String qConvidadoDeletar;

    @Value("${SPS.CASAMENTO.MARCO.LISTAR}") private String qMarcoListar;
    @Value("${SPS.CASAMENTO.MARCO.BUSCAR}") private String qMarcoBuscar;
    @Value("${SPS.CASAMENTO.MARCO.PROXIMOS}") private String qMarcoProximos;
    @Value("${SPS.CASAMENTO.MARCO.CONTAR}") private String qMarcoContar;
    @Value("${SPI.CASAMENTO.MARCO.INSERIR}") private String qMarcoInserir;
    @Value("${SPU.CASAMENTO.MARCO.ALTERAR}") private String qMarcoAlterar;
    @Value("${SPU.CASAMENTO.MARCO.CONCLUIR}") private String qMarcoConcluir;
    @Value("${SPD.CASAMENTO.MARCO.DELETAR}") private String qMarcoDeletar;

    @Value("${SPS.CASAMENTO.DASHBOARD.TOTAIS}") private String qTotais;

    // ---------- configuração ----------

    @Override
    public Map<String, String> listarConfiguracao() {
        return executar("Erro ao ler a configuração do casamento", () -> {
            Map<String, String> mapa = new HashMap<>();
            jdbc.query(qConfigListar, rs -> {
                mapa.put(rs.getString("cdChave"), rs.getString("vlValor"));
            });
            return mapa;
        });
    }

    @Override
    public void salvarConfiguracao(String chave, String valor) {
        executar("Erro ao salvar a configuração do casamento", () ->
                jdbc.update(qConfigSalvar, new MapSqlParameterSource().addValue("cdChave", chave).addValue("vlValor", valor)));
    }

    // ---------- fornecedores ----------

    @Override
    public List<CasamentoFornecedor> listarFornecedores() {
        return executar("Erro ao listar fornecedores", () ->
                jdbc.query(qFornecedorListar, BeanPropertyRowMapper.newInstance(CasamentoFornecedor.class)));
    }

    @Override
    public Optional<CasamentoFornecedor> buscarFornecedor(Long id) {
        return executar("Erro ao buscar fornecedor", () ->
                jdbc.query(qFornecedorBuscar, new MapSqlParameterSource("id", id),
                        BeanPropertyRowMapper.newInstance(CasamentoFornecedor.class)).stream().findFirst());
    }

    @Override
    public Long inserirFornecedor(CasamentoFornecedor f) {
        return executar("Erro ao salvar fornecedor", () -> inserir(qFornecedorInserir, new BeanPropertySqlParameterSource(f), "id_fornecedor"));
    }

    @Override
    public void alterarFornecedor(CasamentoFornecedor f) {
        executar("Erro ao alterar fornecedor", () -> jdbc.update(qFornecedorAlterar, new BeanPropertySqlParameterSource(f)));
    }

    @Override
    public void deletarFornecedor(Long id) {
        executar("Erro ao excluir fornecedor", () -> jdbc.update(qFornecedorDeletar, new MapSqlParameterSource("id", id)));
    }

    // ---------- anexos ----------

    @Override
    public List<CasamentoAnexo> listarAnexos(Long idFornecedor) {
        return executar("Erro ao listar anexos", () ->
                jdbc.query(qAnexoListar, new MapSqlParameterSource("idFornecedor", idFornecedor),
                        BeanPropertyRowMapper.newInstance(CasamentoAnexo.class)));
    }

    @Override
    public Optional<CasamentoAnexo> buscarAnexo(Long idAnexo) {
        return executar("Erro ao buscar anexo", () ->
                jdbc.query(qAnexoBuscar, new MapSqlParameterSource("id", idAnexo),
                        BeanPropertyRowMapper.newInstance(CasamentoAnexo.class)).stream().findFirst());
    }

    @Override
    public Long inserirAnexo(Long idFornecedor, Long idArquivo, String descricao) {
        return executar("Erro ao salvar anexo", () -> inserir(qAnexoInserir, new MapSqlParameterSource()
                .addValue("idFornecedor", idFornecedor).addValue("idArquivo", idArquivo).addValue("txDescricao", descricao), "id_anexo"));
    }

    @Override
    public void deletarAnexo(Long idAnexo) {
        executar("Erro ao excluir anexo", () -> jdbc.update(qAnexoDeletar, new MapSqlParameterSource("id", idAnexo)));
    }

    // ---------- convidados ----------

    @Override
    public List<CasamentoConvidado> listarConvidados() {
        return executar("Erro ao listar convidados", () ->
                jdbc.query(qConvidadoListar, BeanPropertyRowMapper.newInstance(CasamentoConvidado.class)));
    }

    @Override
    public Optional<CasamentoConvidado> buscarConvidado(Long id) {
        return executar("Erro ao buscar convidado", () ->
                jdbc.query(qConvidadoBuscar, new MapSqlParameterSource("id", id),
                        BeanPropertyRowMapper.newInstance(CasamentoConvidado.class)).stream().findFirst());
    }

    @Override
    public Long inserirConvidado(CasamentoConvidado c) {
        return executar("Erro ao salvar convidado", () -> inserir(qConvidadoInserir, new BeanPropertySqlParameterSource(c), "id_convidado"));
    }

    @Override
    public void alterarConvidado(CasamentoConvidado c) {
        executar("Erro ao alterar convidado", () -> jdbc.update(qConvidadoAlterar, new BeanPropertySqlParameterSource(c)));
    }

    @Override
    public void deletarConvidado(Long id) {
        executar("Erro ao excluir convidado", () -> jdbc.update(qConvidadoDeletar, new MapSqlParameterSource("id", id)));
    }

    // ---------- marcos ----------

    @Override
    public List<CasamentoMarco> listarMarcos() {
        return executar("Erro ao listar marcos", () ->
                jdbc.query(qMarcoListar, BeanPropertyRowMapper.newInstance(CasamentoMarco.class)));
    }

    @Override
    public List<CasamentoMarco> listarProximosMarcos(int limite) {
        return executar("Erro ao listar os próximos marcos", () ->
                jdbc.query(qMarcoProximos, new MapSqlParameterSource("limite", limite),
                        BeanPropertyRowMapper.newInstance(CasamentoMarco.class)));
    }

    @Override
    public Optional<CasamentoMarco> buscarMarco(Long id) {
        return executar("Erro ao buscar marco", () ->
                jdbc.query(qMarcoBuscar, new MapSqlParameterSource("id", id),
                        BeanPropertyRowMapper.newInstance(CasamentoMarco.class)).stream().findFirst());
    }

    @Override
    public int contarMarcos() {
        return executar("Erro ao contar marcos", () -> {
            Integer total = jdbc.queryForObject(qMarcoContar, new MapSqlParameterSource(), Integer.class);
            return total == null ? 0 : total;
        });
    }

    @Override
    public Long inserirMarco(CasamentoMarco m) {
        return executar("Erro ao salvar marco", () -> inserir(qMarcoInserir, new MapSqlParameterSource()
                .addValue("nmTitulo", m.getNmTitulo())
                .addValue("dtPrazo", m.getDtPrazo() == null ? null : Date.valueOf(m.getDtPrazo()))
                .addValue("inConcluido", Boolean.TRUE.equals(m.getInConcluido()))
                .addValue("txObservacao", m.getTxObservacao()), "id_marco"));
    }

    @Override
    public void alterarMarco(CasamentoMarco m) {
        executar("Erro ao alterar marco", () -> jdbc.update(qMarcoAlterar, new MapSqlParameterSource()
                .addValue("id", m.getId())
                .addValue("nmTitulo", m.getNmTitulo())
                .addValue("dtPrazo", m.getDtPrazo() == null ? null : Date.valueOf(m.getDtPrazo()))
                .addValue("txObservacao", m.getTxObservacao())));
    }

    @Override
    public void concluirMarco(Long id, boolean concluido) {
        executar("Erro ao atualizar o marco", () -> jdbc.update(qMarcoConcluir,
                new MapSqlParameterSource().addValue("id", id).addValue("inConcluido", concluido)));
    }

    @Override
    public void deletarMarco(Long id) {
        executar("Erro ao excluir marco", () -> jdbc.update(qMarcoDeletar, new MapSqlParameterSource("id", id)));
    }

    // ---------- dashboard ----------

    @Override
    public CasamentoTotaisDto buscarTotais() {
        return executar("Erro ao calcular os totais do casamento", () ->
                jdbc.queryForObject(qTotais, new MapSqlParameterSource(), BeanPropertyRowMapper.newInstance(CasamentoTotaisDto.class)));
    }

    // ---------- auxiliares ----------

    private Long inserir(String sql, org.springframework.jdbc.core.namedparam.SqlParameterSource params, String colunaId) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(sql, params, keys, new String[]{colunaId});
        return keys.getKey().longValue();
    }

    private <T> T executar(String mensagem, Supplier<T> acao) {
        try {
            return acao.get();
        } catch (ValidacaoException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error(mensagem, ex);
            throw new ValidacaoException(NordHttpEnum.HTTP_400, mensagem, ExceptionUtils.getRootCauseMessage(ex));
        }
    }
}
