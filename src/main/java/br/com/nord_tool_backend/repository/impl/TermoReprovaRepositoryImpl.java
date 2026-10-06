package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.controller.response.NordHttpEnum;
import br.com.nord_tool_backend.domain.TermoReprova;
import br.com.nord_tool_backend.dto.TermoReprovaResumoGeralDto;
import br.com.nord_tool_backend.excepetion.ValidacaoException;
import br.com.nord_tool_backend.repository.RepositoryJdbcOperationsSql;
import br.com.nord_tool_backend.repository.TermoReprovaRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository @Slf4j
@PropertySource("classpath:query/termo-reprova.properties")
public class TermoReprovaRepositoryImpl extends RepositoryJdbcOperationsSql<TermoReprova> implements TermoReprovaRepository {

    @Value("${SPS.TERMO_REPROVA.LISTAR_POR_APARTAMENTO}") private String queryListar;
    @Value("${SPS.TERMO_REPROVA.BUSCAR_POR_ID}") private String queryBuscar;
    @Value("${SPS.TERMO_REPROVA.PROXIMO_NUMERO}") private String queryProximoNumero;
    @Value("${SPS.TERMO_REPROVA.APARTAMENTO_EXISTE}") private String queryApartamentoExiste;
    @Value("${SPS.TERMO_REPROVA.IDS_POR_APARTAMENTO}") private String queryIds;
    @Value("${SPI.TERMO_REPROVA.INSERIR}") private String queryInserir;
    @Value("${SPU.TERMO_REPROVA.ATUALIZAR_ARQUIVO}") private String queryAtualizarArquivo;
    @Value("${SPU.TERMO_REPROVA.ATUALIZAR_SITUACAO}") private String queryAtualizarSituacao;
    @Value("${SPD.TERMO_REPROVA.DELETAR}") private String queryDeletar;
    @Value("${SPS.TERMO_REPROVA.RESUMO_GERAL}") private String queryResumoGeral;

    @Override
    public boolean apartamentoExiste(Long idApartamento) {
        try {
            Integer total = namedParameterJdbcTemplate.queryForObject(queryApartamentoExiste,
                    new MapSqlParameterSource("id", idApartamento), Integer.class);
            return total != null && total > 0;
        } catch (Exception ex) {
            throw tratarErro("Erro ao consultar apartamento", ex);
        }
    }

    @Override
    public List<TermoReprova> listarPorApartamento(Long idApartamento) {
        try {
            return buscarTodosPorFiltro(queryListar, new MapSqlParameterSource("idApartamento", idApartamento),
                    BeanPropertyRowMapper.newInstance(TermoReprova.class));
        } catch (Exception ex) {
            throw tratarErro("Erro ao listar termos de reprova", ex);
        }
    }

    @Override
    public Optional<TermoReprova> buscarPorId(Long id) {
        try {
            return buscarTodosPorFiltro(queryBuscar, new MapSqlParameterSource("id", id),
                    BeanPropertyRowMapper.newInstance(TermoReprova.class)).stream().findFirst();
        } catch (Exception ex) {
            throw tratarErro("Erro ao buscar termo de reprova", ex);
        }
    }

    @Override
    public int proximoNumero(Long idApartamento) {
        try {
            Integer proximo = namedParameterJdbcTemplate.queryForObject(queryProximoNumero,
                    new MapSqlParameterSource("idApartamento", idApartamento), Integer.class);
            return proximo == null ? 1 : proximo;
        } catch (Exception ex) {
            throw tratarErro("Erro ao numerar termo de reprova", ex);
        }
    }

    @Override
    public List<Long> listarIdsPorApartamento(Long idApartamento) {
        try {
            return namedParameterJdbcTemplate.queryForList(queryIds,
                    new MapSqlParameterSource("idApartamento", idApartamento), Long.class);
        } catch (Exception ex) {
            throw tratarErro("Erro ao listar termos de reprova", ex);
        }
    }

    @Override
    public TermoReprova inserir(TermoReprova termo) {
        try {
            return salvar(queryInserir, termo, "id_termo_reprova");
        } catch (Exception ex) {
            throw tratarErro("Erro ao salvar termo de reprova", ex);
        }
    }

    @Override
    public void atualizarArquivo(Long id, Long idArquivo, int nrPaginas) {
        try {
            namedParameterJdbcTemplate.update(queryAtualizarArquivo, new MapSqlParameterSource()
                    .addValue("id", id).addValue("idArquivo", idArquivo).addValue("nrPaginas", nrPaginas));
        } catch (Exception ex) {
            throw tratarErro("Erro ao trocar o PDF do termo", ex);
        }
    }

    @Override
    public void atualizarSituacao(Long id, String situacao, String observacao) {
        try {
            namedParameterJdbcTemplate.update(queryAtualizarSituacao, new MapSqlParameterSource()
                    .addValue("id", id).addValue("situacao", situacao).addValue("observacao", observacao));
        } catch (Exception ex) {
            throw tratarErro("Erro ao atualizar a situação do termo", ex);
        }
    }

    @Override
    public void deletar(Long id) {
        try {
            deletar(queryDeletar, new MapSqlParameterSource("id", id));
        } catch (Exception ex) {
            throw tratarErro("Erro ao excluir termo de reprova", ex);
        }
    }

    @Override
    public TermoReprovaResumoGeralDto resumoGeral() {
        try {
            return buscarPorId(queryResumoGeral, new MapSqlParameterSource(),
                    BeanPropertyRowMapper.newInstance(TermoReprovaResumoGeralDto.class));
        } catch (Exception ex) {
            throw tratarErro("Erro ao calcular o resumo dos termos de reprova", ex);
        }
    }

    private ValidacaoException tratarErro(String mensagem, Exception ex) {
        log.error(mensagem, ex);
        return new ValidacaoException(NordHttpEnum.HTTP_400, mensagem, ExceptionUtils.getRootCauseMessage(ex));
    }
}
