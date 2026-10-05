package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.controller.response.NordHttpEnum;
import br.com.nord_tool_backend.domain.TermoFoto;
import br.com.nord_tool_backend.excepetion.ValidacaoException;
import br.com.nord_tool_backend.repository.RepositoryJdbcOperationsSql;
import br.com.nord_tool_backend.repository.TermoFotoRepository;
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
public class TermoFotoRepositoryImpl extends RepositoryJdbcOperationsSql<TermoFoto> implements TermoFotoRepository {

    @Value("${SPS.TERMO_FOTO.LISTAR_POR_TERMO}") private String queryListar;
    @Value("${SPS.TERMO_FOTO.BUSCAR_POR_ID}") private String queryBuscar;
    @Value("${SPS.TERMO_FOTO.PROXIMA_ORDEM}") private String queryProximaOrdem;
    @Value("${SPS.TERMO_FOTO.CONTAR_POR_TERMO}") private String queryContar;
    @Value("${SPI.TERMO_FOTO.INSERIR}") private String queryInserir;
    @Value("${SPU.TERMO_FOTO.ATUALIZAR}") private String queryAtualizar;
    @Value("${SPU.TERMO_FOTO.ATUALIZAR_ORDEM}") private String queryAtualizarOrdem;
    @Value("${SPD.TERMO_FOTO.DELETAR}") private String queryDeletar;

    @Override
    public List<TermoFoto> listarPorTermo(Long idTermo) {
        try {
            return buscarTodosPorFiltro(queryListar, new MapSqlParameterSource("idTermo", idTermo),
                    BeanPropertyRowMapper.newInstance(TermoFoto.class));
        } catch (Exception ex) {
            throw tratarErro("Erro ao listar fotos do termo", ex);
        }
    }

    @Override
    public Optional<TermoFoto> buscarPorId(Long id) {
        try {
            return buscarTodosPorFiltro(queryBuscar, new MapSqlParameterSource("id", id),
                    BeanPropertyRowMapper.newInstance(TermoFoto.class)).stream().findFirst();
        } catch (Exception ex) {
            throw tratarErro("Erro ao buscar foto do termo", ex);
        }
    }

    @Override
    public int proximaOrdem(Long idTermo, int nrPagina) {
        try {
            Integer ordem = namedParameterJdbcTemplate.queryForObject(queryProximaOrdem,
                    new MapSqlParameterSource().addValue("idTermo", idTermo).addValue("nrPagina", nrPagina), Integer.class);
            return ordem == null ? 0 : ordem;
        } catch (Exception ex) {
            throw tratarErro("Erro ao ordenar foto do termo", ex);
        }
    }

    @Override
    public int contarPorTermo(Long idTermo) {
        try {
            Integer total = namedParameterJdbcTemplate.queryForObject(queryContar,
                    new MapSqlParameterSource("idTermo", idTermo), Integer.class);
            return total == null ? 0 : total;
        } catch (Exception ex) {
            throw tratarErro("Erro ao contar fotos do termo", ex);
        }
    }

    @Override
    public TermoFoto inserir(TermoFoto foto) {
        try {
            return salvar(queryInserir, foto, "id_termo_foto");
        } catch (Exception ex) {
            throw tratarErro("Erro ao salvar foto do termo", ex);
        }
    }

    @Override
    public void atualizar(TermoFoto foto) {
        try {
            alterar(queryAtualizar, foto);
        } catch (Exception ex) {
            throw tratarErro("Erro ao alterar foto do termo", ex);
        }
    }

    @Override
    public void atualizarOrdem(Long idTermo, Long idFoto, int nrOrdem) {
        try {
            namedParameterJdbcTemplate.update(queryAtualizarOrdem, new MapSqlParameterSource()
                    .addValue("idTermo", idTermo).addValue("id", idFoto).addValue("nrOrdem", nrOrdem));
        } catch (Exception ex) {
            throw tratarErro("Erro ao reordenar fotos do termo", ex);
        }
    }

    @Override
    public void deletar(Long id) {
        try {
            deletar(queryDeletar, new MapSqlParameterSource("id", id));
        } catch (Exception ex) {
            throw tratarErro("Erro ao excluir foto do termo", ex);
        }
    }

    private ValidacaoException tratarErro(String mensagem, Exception ex) {
        log.error(mensagem, ex);
        return new ValidacaoException(NordHttpEnum.HTTP_400, mensagem, ExceptionUtils.getRootCauseMessage(ex));
    }
}
