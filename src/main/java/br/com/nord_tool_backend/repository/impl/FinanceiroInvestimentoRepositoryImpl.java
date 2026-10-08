package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.controller.response.NordHttpEnum;
import br.com.nord_tool_backend.domain.FinanceiroAtivo;
import br.com.nord_tool_backend.domain.FinanceiroOperacao;
import br.com.nord_tool_backend.domain.FinanceiroProvento;
import br.com.nord_tool_backend.excepetion.ValidacaoException;
import br.com.nord_tool_backend.repository.FinanceiroInvestimentoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Repository @Slf4j @RequiredArgsConstructor
@PropertySource("classpath:query/financeiro_investimento.properties")
public class FinanceiroInvestimentoRepositoryImpl implements FinanceiroInvestimentoRepository {

    private final NamedParameterJdbcTemplate jdbc;

    @Value("${SPS.FININV.ATIVO.LISTAR}") private String qAtivoListar;
    @Value("${SPS.FININV.ATIVO.BUSCAR}") private String qAtivoBuscar;
    @Value("${SPS.FININV.ATIVO.BUSCAR_TICKER}") private String qAtivoTicker;
    @Value("${SPI.FININV.ATIVO.INSERIR}") private String qAtivoInserir;
    @Value("${SPU.FININV.ATIVO.ALTERAR}") private String qAtivoAlterar;
    @Value("${SPU.FININV.ATIVO.COTACAO}") private String qAtivoCotacao;

    @Value("${SPS.FININV.OPERACAO.LISTAR}") private String qOpListar;
    @Value("${SPS.FININV.OPERACAO.BUSCAR}") private String qOpBuscar;
    @Value("${SPS.FININV.OPERACAO.BUSCAR_REQUISICAO}") private String qOpRequisicao;
    @Value("${SPI.FININV.OPERACAO.INSERIR}") private String qOpInserir;
    @Value("${SPD.FININV.OPERACAO.DELETAR}") private String qOpDeletar;

    @Value("${SPS.FININV.PROVENTO.LISTAR}") private String qProvListar;
    @Value("${SPS.FININV.PROVENTO.BUSCAR}") private String qProvBuscar;
    @Value("${SPI.FININV.PROVENTO.MANUAL}") private String qProvManual;
    @Value("${SPI.FININV.PROVENTO.IMPORTADO}") private String qProvImportado;
    @Value("${SPD.FININV.PROVENTO.DELETAR}") private String qProvDeletar;

    @Override
    public List<FinanceiroAtivo> listarAtivos(Long idPessoa) {
        return executar("Erro ao listar os fundos", () -> jdbc.query(qAtivoListar,
                new MapSqlParameterSource().addValue("idPessoa", idPessoa, java.sql.Types.BIGINT),
                BeanPropertyRowMapper.newInstance(FinanceiroAtivo.class)));
    }

    @Override
    public Optional<FinanceiroAtivo> buscarAtivo(Long id) {
        return executar("Erro ao buscar o fundo", () -> jdbc.query(qAtivoBuscar, new MapSqlParameterSource("id", id),
                BeanPropertyRowMapper.newInstance(FinanceiroAtivo.class)).stream().findFirst());
    }

    @Override
    public Optional<FinanceiroAtivo> buscarAtivoPorTicker(String cdTicker, Long idPessoa) {
        return executar("Erro ao buscar o fundo", () -> jdbc.query(qAtivoTicker,
                new MapSqlParameterSource().addValue("cdTicker", cdTicker).addValue("idPessoa", idPessoa),
                BeanPropertyRowMapper.newInstance(FinanceiroAtivo.class)).stream().findFirst());
    }

    @Override
    public Long inserirAtivo(FinanceiroAtivo a) {
        return executar("Erro ao salvar o fundo", () -> jdbc.queryForObject(qAtivoInserir, new MapSqlParameterSource()
                .addValue("cdTicker", a.getCdTicker()).addValue("nmAtivo", a.getNmAtivo())
                .addValue("idPessoa", a.getIdPessoa()), Long.class));
    }

    @Override
    public int alterarAtivo(FinanceiroAtivo a, int nrVersao) {
        return executar("Erro ao alterar o fundo", () -> jdbc.update(qAtivoAlterar, new MapSqlParameterSource()
                .addValue("id", a.getId()).addValue("nmAtivo", a.getNmAtivo())
                .addValue("inAtivo", !Boolean.FALSE.equals(a.getInAtivo())).addValue("nrVersao", nrVersao)));
    }

    @Override
    public void atualizarCotacao(Long idAtivo, BigDecimal vlCotacao, LocalDateTime dhCotacao) {
        executar("Erro ao guardar a cotação", () -> jdbc.update(qAtivoCotacao, new MapSqlParameterSource()
                .addValue("id", idAtivo).addValue("vlCotacao", vlCotacao).addValue("dhCotacao", Timestamp.valueOf(dhCotacao))));
    }

    @Override
    public List<FinanceiroOperacao> listarOperacoes(Collection<Long> idsAtivo) {
        if (idsAtivo.isEmpty()) return Collections.emptyList();
        return executar("Erro ao listar as operações", () -> jdbc.query(qOpListar, new MapSqlParameterSource("idsAtivo", idsAtivo),
                BeanPropertyRowMapper.newInstance(FinanceiroOperacao.class)));
    }

    @Override
    public Optional<FinanceiroOperacao> buscarOperacao(Long id) {
        return executar("Erro ao buscar a operação", () -> jdbc.query(qOpBuscar, new MapSqlParameterSource("id", id),
                BeanPropertyRowMapper.newInstance(FinanceiroOperacao.class)).stream().findFirst());
    }

    @Override
    public Optional<Long> buscarAtivoDaRequisicao(String cdRequisicao) {
        return executar("Erro ao buscar a operação", () -> jdbc.queryForList(qOpRequisicao,
                new MapSqlParameterSource("cdRequisicao", cdRequisicao), Long.class).stream().findFirst());
    }

    @Override
    public Optional<Long> inserirOperacao(FinanceiroOperacao o) {
        return executar("Erro ao salvar a operação", () -> jdbc.queryForList(qOpInserir, new MapSqlParameterSource()
                .addValue("cdRequisicao", o.getCdRequisicao()).addValue("idAtivo", o.getIdAtivo())
                .addValue("dtOperacao", Date.valueOf(o.getDtOperacao())).addValue("cdTipo", o.getCdTipo())
                .addValue("qtCotas", o.getQtCotas()).addValue("vlPreco", o.getVlPreco()), Long.class).stream().findFirst());
    }

    @Override
    public void excluirOperacao(Long id) {
        executar("Erro ao excluir a operação", () -> jdbc.update(qOpDeletar, new MapSqlParameterSource("id", id)));
    }

    @Override
    public List<FinanceiroProvento> listarProventos(Collection<Long> idsAtivo) {
        if (idsAtivo.isEmpty()) return Collections.emptyList();
        return executar("Erro ao listar os proventos", () -> jdbc.query(qProvListar, new MapSqlParameterSource("idsAtivo", idsAtivo),
                BeanPropertyRowMapper.newInstance(FinanceiroProvento.class)));
    }

    @Override
    public Optional<FinanceiroProvento> buscarProvento(Long id) {
        return executar("Erro ao buscar o provento", () -> jdbc.query(qProvBuscar, new MapSqlParameterSource("id", id),
                BeanPropertyRowMapper.newInstance(FinanceiroProvento.class)).stream().findFirst());
    }

    @Override
    public void gravarProventoManual(FinanceiroProvento p) {
        executar("Erro ao salvar o provento", () -> jdbc.update(qProvManual, parametros(p)));
    }

    @Override
    public boolean gravarProventoImportado(FinanceiroProvento p) {
        return executar("Erro ao importar o provento", () -> jdbc.update(qProvImportado, parametros(p)) > 0);
    }

    @Override
    public void excluirProvento(Long id) {
        executar("Erro ao excluir o provento", () -> jdbc.update(qProvDeletar, new MapSqlParameterSource("id", id)));
    }

    private static MapSqlParameterSource parametros(FinanceiroProvento p) {
        return new MapSqlParameterSource().addValue("idAtivo", p.getIdAtivo()).addValue("dtCom", Date.valueOf(p.getDtCom()))
                .addValue("dtPagamento", Date.valueOf(p.getDtPagamento())).addValue("vlPorCota", p.getVlPorCota());
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
