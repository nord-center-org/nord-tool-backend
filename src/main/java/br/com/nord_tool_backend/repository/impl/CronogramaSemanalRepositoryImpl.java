package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.controller.response.NordHttpEnum;
import br.com.nord_tool_backend.domain.CronogramaSemanal;
import br.com.nord_tool_backend.dto.CronogramaSemanalDto;
import br.com.nord_tool_backend.excepetion.ValidacaoException;
import br.com.nord_tool_backend.repository.CronogramaSemanalRepository;
import br.com.nord_tool_backend.repository.RepositoryJdbcOperationsSql;
import br.com.nord_tool_backend.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Slf4j
@PropertySource("classpath:query/cronograma-semanal.properties")
public class CronogramaSemanalRepositoryImpl extends RepositoryJdbcOperationsSql<CronogramaSemanal> implements CronogramaSemanalRepository {

    private static final String ERRO_GENERICO_SALVAR = "Erro ao salvar um Cronograma Semanal";
    private static final String ERRO_GENERICO_ALTERAR = "Erro ao alterar um Cronograma Semanal";
    private static final String ERRO_GENERICO_DELETAR = "Erro ao deletar um Cronograma Semanal";
    private static final String ERRO_GENERICO_BUSCAR = "Erro ao buscar um Cronograma Semanal";
    private static final String ERRO_GENERICO_LISTAR = "Erro ao listar Cronograma Semanais";

    @Value("${SPI.CRONOGRAMA_SEMANAL.INSERIR}")
    private String querySalvaCronogramaSemanal;

    @Value("${SPU.CRONOGRAMA_SEMANAL.ALTERAR}")
    private String queryAlteraCronogramaSemanal;

    @Value("${SPD.CRONOGRAMA_SEMANAL.DELETAR}")
    private String queryDeletaCronogramaSemanal;

    @Value("${SPS.CRONOGRAMA_SEMANAL.BUSTAR_POR_ID}")
    private String queryBuscaPorIdCronogramaSemanal;

    @Value("${SPS.CRONOGRAMA_SEMANAL.LISTAR}")
    private String queryListaCronogramaSemanal;


    @Override
    public CronogramaSemanalDto salvarCronogramaSemanal(CronogramaSemanal cronogramaSemanal) {
        try {
            log.info("Salvando na base de dados um Cronograma Semanal");
            CronogramaSemanal cronogramaSemanalSalvar = salvar(querySalvaCronogramaSemanal, cronogramaSemanal, "id_cronograma_semanal");
            return CronogramaSemanalDto.converterToDto(buscarPorIdCronogramaSemanal(cronogramaSemanalSalvar.getId()));
        } catch (Exception ex) {
            log.error(ExceptionUtils.getMessage(ex));
            throw new ValidacaoException(NordHttpEnum.HTTP_400, StringUtils.getMensagem(ERRO_GENERICO_SALVAR), ex.getMessage());
        }
    }

    @Override
    public CronogramaSemanalDto alterarCronogramaSemanal(CronogramaSemanal cronogramaSemanal) {
        try {
            log.info("Alterando na base de dados um Cronograma Semanal");
            CronogramaSemanal cronogramaSemanalAlterar = alterar(queryAlteraCronogramaSemanal, cronogramaSemanal);
            return CronogramaSemanalDto.converterToDto(buscarPorIdCronogramaSemanal(cronogramaSemanalAlterar.getId()));
        } catch (Exception ex) {
            log.error(ExceptionUtils.getMessage(ex));
            throw new ValidacaoException(NordHttpEnum.HTTP_400, StringUtils.getMensagem(ERRO_GENERICO_ALTERAR), ex.getMessage());
        }
    }

    @Override
    public void deletarCronogramaSemanal(Long id) {
        MapSqlParameterSource params = new MapSqlParameterSource("id", id);
        try {
            log.info("Apagando na base de dados um Apartamento Vistoria");
            deletar(queryDeletaCronogramaSemanal, params);
        } catch (Exception ex) {
            log.error(ExceptionUtils.getMessage(ex));
            throw new ValidacaoException(NordHttpEnum.HTTP_400, StringUtils.getMensagem(ERRO_GENERICO_DELETAR), ex.getMessage());
        }
    }

    @Override
    public CronogramaSemanal buscarPorIdCronogramaSemanal(Long id) {
        MapSqlParameterSource params = new MapSqlParameterSource("id", id);
        try {
            log.info("Buscando na base de dados um Cronograma Semanal");
            return buscarPorId(queryBuscaPorIdCronogramaSemanal, params, BeanPropertyRowMapper.newInstance(CronogramaSemanal.class));
        } catch (Exception ex) {
            log.error(ExceptionUtils.getMessage(ex));
            throw new ValidacaoException(NordHttpEnum.HTTP_400, StringUtils.getMensagem(ERRO_GENERICO_BUSCAR), ex.getMessage());
        }
    }

    @Override
    public List<CronogramaSemanal> listarCronogramaSemanal() {
        try {
            log.info("Listando na base de dados os Cronograma Semanais");
            return buscarTodos(queryListaCronogramaSemanal, BeanPropertyRowMapper.newInstance(CronogramaSemanal.class));
        } catch (Exception ex) {
            log.error(ExceptionUtils.getMessage(ex));
            throw new ValidacaoException(NordHttpEnum.HTTP_400, StringUtils.getMensagem(ERRO_GENERICO_LISTAR), ex.getMessage());
        }
    }
}
