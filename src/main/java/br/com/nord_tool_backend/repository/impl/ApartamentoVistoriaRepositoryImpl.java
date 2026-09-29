package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.controller.response.NordHttpEnum;
import br.com.nord_tool_backend.domain.ApartamentoVistoria;
import br.com.nord_tool_backend.domain.InfoGeralApartamentoVistoria;
import br.com.nord_tool_backend.dto.ApartamentoVistoriaDto;
import br.com.nord_tool_backend.dto.ApartamentoVistoriaFiltroDto;
import br.com.nord_tool_backend.excepetion.ValidacaoException;
import br.com.nord_tool_backend.repository.ApartamentoVistoriaRepository;
import br.com.nord_tool_backend.repository.RepositoryJdbcOperationsSql;
import br.com.nord_tool_backend.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.util.List;

import static br.com.nord_tool_backend.utils.StringUtils.parseDataPermitida;

@Repository
@Slf4j
@PropertySource("classpath:query/apartamento-vistoria.properties")
public class ApartamentoVistoriaRepositoryImpl extends RepositoryJdbcOperationsSql<ApartamentoVistoria> implements ApartamentoVistoriaRepository {

    private static final String ERRO_GENERICO_SALVAR = "Erro ao salvar apartamento";
    private static final String ERRO_GENERICO_ALTERAR = "Erro ao alterar apartamento";
    private static final String ERRO_GENERICO_DELETAR = "Erro ao deletar apartamento";
    private static final String ERRO_GENERICO_BUSCAR = "Erro ao buscar apartamento";
    private static final String ERRO_GENERICO_LISTAR = "Erro ao listar apartamentos";

    @Value("${SPI.APARTAMENTO_VISTORIA}")
    private String querySalvaApartamentoVistoria;

    @Value("${SPU.APARTAMENTO_VISTORIA}")
    private String queryAlteraApartamentoVistoria;

    @Value("${SPD.APARTAMENTO_VISTORIA.WHERE.ID}")
    private String queryDeletaApartamentoVistoria;

    @Value("${SPS.BUSCAR.APARTAMENTO_VISTORIA}")
    private String queryBuscaApartamentoVistoria;

    @Value("${SPS.LISTAR.APARTAMENTO_VISTORIA}")
    private String queryListaApartamentoVistoria;

    @Value("${SPS.APARTAMENTO_VISTORIA_PAGINACAO}")
    private String queryPaginacao;

    @Value("${SPS.LISTAR.INFO_GERAL_APARTAMENTO_VISTORIA}")
    private String queryListaInfoGeralApartamentoVistoria;

    @Override
    public ApartamentoVistoriaDto salvarApartamentoVistoria(ApartamentoVistoria apartamentoVistoria) {
        try {
            log.info("Salvando na base de dados um Apartamento Vistoria");
            ApartamentoVistoria apartamentoVistoriaSalvar = salvar(querySalvaApartamentoVistoria, apartamentoVistoria, "id_apartamento_vistoria");
            return ApartamentoVistoriaDto.converterToDto(apartamentoVistoriaSalvar);
        } catch (Exception ex) {
            log.error(ExceptionUtils.getMessage(ex));
            throw new ValidacaoException(NordHttpEnum.HTTP_400, StringUtils.getMensagem(ERRO_GENERICO_SALVAR), ex.getMessage());
        }
    }

    @Override
    public ApartamentoVistoriaDto alterarApartamentoVistoria(ApartamentoVistoria apartamentoVistoria) {
        try {
            log.info("Alterando na base de dados um Apartamento Vistoria");
            ApartamentoVistoria apartamentoVistoriaAlterar = alterar(queryAlteraApartamentoVistoria, apartamentoVistoria);
            return ApartamentoVistoriaDto.converterToDto(apartamentoVistoriaAlterar);
        } catch (Exception ex) {
            log.error(ExceptionUtils.getMessage(ex));
            throw new ValidacaoException(NordHttpEnum.HTTP_400, StringUtils.getMensagem(ERRO_GENERICO_ALTERAR), ex.getMessage());
        }
    }

    @Override
    public void deletarApartamentoVistoria(Long id) {
        MapSqlParameterSource params = new MapSqlParameterSource("id", id);
        try {
            log.info("Apagando na base de dados um Apartamento Vistoria");
            deletar(queryDeletaApartamentoVistoria, params);
        } catch (Exception ex) {
            log.error(ExceptionUtils.getMessage(ex));
            throw new ValidacaoException(NordHttpEnum.HTTP_400, StringUtils.getMensagem(ERRO_GENERICO_DELETAR), ex.getMessage());
        }
    }

    @Override
    public ApartamentoVistoria buscarApartamentoVistoria(Long id) {
        MapSqlParameterSource params = new MapSqlParameterSource("id", id);
        try {
            log.info("Buscando na base de dados um Apartamento Vistoria");
            return buscarPorId(queryBuscaApartamentoVistoria, params, BeanPropertyRowMapper.newInstance(ApartamentoVistoria.class));
        } catch (Exception ex) {
            log.error(ExceptionUtils.getMessage(ex));
            throw new ValidacaoException(NordHttpEnum.HTTP_400, StringUtils.getMensagem(ERRO_GENERICO_BUSCAR), ex.getMessage());
        }
    }

    @Override
    public List<ApartamentoVistoria> listarApartamentoVistoria() {
        try {
            log.info("Listando na base de dados Apartamentos Vistoria");
            return buscarTodos(queryListaApartamentoVistoria, BeanPropertyRowMapper.newInstance(ApartamentoVistoria.class));
        } catch (Exception ex) {
            log.error(ExceptionUtils.getMessage(ex));
            throw new ValidacaoException(NordHttpEnum.HTTP_400, StringUtils.getMensagem(ERRO_GENERICO_LISTAR), ex.getMessage());
        }
    }

    @Override
    public void salvarEmLote(List<ApartamentoVistoria> lsApartamentoVistoria) {
        try {
            log.info("Salvando dados da planilha de apartamentos em lote na base de dados");
            salvarTodos(querySalvaApartamentoVistoria, lsApartamentoVistoria);
        } catch (Exception ex) {
            log.error(ExceptionUtils.getMessage(ex));
            throw new ValidacaoException(NordHttpEnum.HTTP_400, StringUtils.getMensagem(ERRO_GENERICO_SALVAR), ex.getMessage());
        }
    }

    @Override
    public List<ApartamentoVistoriaDto> listarApartamentoVistoriaFiltrado(String query, ApartamentoVistoriaFiltroDto apartamentoVistoriaFiltroDto, String filtraTodos, int nrPagina, int nrQuantidadePorPagina) {
        MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();

        if (filtraTodos != null && !filtraTodos.isEmpty()) {
            mapSqlParameterSource.addValue("filtraTodos", "%".concat(filtraTodos).concat("%"));
        }
        mapSqlParameterSource.addValue("nmApartamentoVistoria", apartamentoVistoriaFiltroDto.getNmApartamentoVistoria(), Types.VARCHAR);
        mapSqlParameterSource.addValue("nmDiaSemana", apartamentoVistoriaFiltroDto.getNmDiaSemana(), Types.VARCHAR);
        mapSqlParameterSource.addValue("dtApartamentoVigente", parseDataPermitida(apartamentoVistoriaFiltroDto.getDtApartamentoVigente()), Types.DATE);
        mapSqlParameterSource.addValue("nmHorarioVistoria", apartamentoVistoriaFiltroDto.getNmHorarioVistoria(), Types.VARCHAR);
        mapSqlParameterSource.addValue("nmStatusVistoria", apartamentoVistoriaFiltroDto.getNmStatusVistoria(), Types.VARCHAR);
        mapSqlParameterSource.addValue("txObservacaoRevistoria", apartamentoVistoriaFiltroDto.getTxObservacaoRevistoria(), Types.VARCHAR);
        mapSqlParameterSource.addValue("dtRevistoriaVigente", parseDataPermitida(apartamentoVistoriaFiltroDto.getDtRevistoriaVigente()), Types.DATE);

        mapSqlParameterSource.addValue("nrPagina", nrPagina);
        mapSqlParameterSource.addValue("nrQuantidadePorPagina", nrQuantidadePorPagina);

        String sql = query + queryPaginacao;
        try {
            log.info("Listando da base de dados os Apartamentos Vistoria filtrados");
            return buscarTodosPorFiltro(sql, mapSqlParameterSource, BeanPropertyRowMapper.newInstance(ApartamentoVistoriaDto.class));
        } catch (Exception ex) {
            log.error(ExceptionUtils.getMessage(ex));
            throw new ValidacaoException(NordHttpEnum.HTTP_400, StringUtils.getMensagem(ERRO_GENERICO_LISTAR), ex.getMessage());
        }
    }

    @Override
    public List<InfoGeralApartamentoVistoria> listarInfoGeralApartamentoVistoria(String dtiApartamentoVistoriaFiltro, String dtfApartamentoVistoriaFiltro) {
        try {
            MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();

            log.info("Listando na base de dados informações gerais dos Apartamentos");
            mapSqlParameterSource.addValue("dtiApartamentoVistoriaFiltro", parseDataPermitida(dtiApartamentoVistoriaFiltro), Types.DATE);
            mapSqlParameterSource.addValue("dtfApartamentoVistoriaFiltro", parseDataPermitida(dtfApartamentoVistoriaFiltro), Types.DATE);

            return buscarTodosPorFiltro(queryListaInfoGeralApartamentoVistoria, mapSqlParameterSource, BeanPropertyRowMapper.newInstance(InfoGeralApartamentoVistoria.class));
        } catch (Exception ex) {
            log.error(ExceptionUtils.getMessage(ex));
            throw new ValidacaoException(NordHttpEnum.HTTP_400, StringUtils.getMensagem(ERRO_GENERICO_LISTAR), ex.getMessage());
        }
    }
}
