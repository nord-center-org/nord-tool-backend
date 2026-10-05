package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.controller.response.NordHttpEnum;
import br.com.nord_tool_backend.domain.ArquivoArmazenado;
import br.com.nord_tool_backend.excepetion.ValidacaoException;
import br.com.nord_tool_backend.repository.ArquivoArmazenadoRepository;
import br.com.nord_tool_backend.repository.RepositoryJdbcOperationsSql;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository @Slf4j
@PropertySource("classpath:query/arquivo-armazenado.properties")
public class ArquivoArmazenadoRepositoryImpl extends RepositoryJdbcOperationsSql<ArquivoArmazenado>
        implements ArquivoArmazenadoRepository {

    @Value("${SPI.ARQUIVO_ARMAZENADO.INSERIR}") private String queryInserir;
    @Value("${SPS.ARQUIVO_ARMAZENADO.BUSCAR_COM_CONTEUDO}") private String queryBuscarComConteudo;
    @Value("${SPS.ARQUIVO_ARMAZENADO.BUSCAR_METADADOS}") private String queryBuscarMetadados;
    @Value("${SPD.ARQUIVO_ARMAZENADO.DELETAR}") private String queryDeletar;

    @Override
    public ArquivoArmazenado inserir(ArquivoArmazenado arquivo) {
        try {
            return salvar(queryInserir, arquivo, "id_arquivo");
        } catch (Exception ex) {
            throw tratarErro("Erro ao salvar arquivo", ex);
        }
    }

    @Override
    public Optional<ArquivoArmazenado> buscarComConteudo(Long id) {
        return buscar(queryBuscarComConteudo, id);
    }

    @Override
    public Optional<ArquivoArmazenado> buscarMetadados(Long id) {
        return buscar(queryBuscarMetadados, id);
    }

    @Override
    public int deletar(Long id) {
        try {
            return deletar(queryDeletar, new MapSqlParameterSource("id", id));
        } catch (Exception ex) {
            throw tratarErro("Erro ao apagar arquivo", ex);
        }
    }

    private Optional<ArquivoArmazenado> buscar(String sql, Long id) {
        try {
            return buscarTodosPorFiltro(sql, new MapSqlParameterSource("id", id),
                    BeanPropertyRowMapper.newInstance(ArquivoArmazenado.class)).stream().findFirst();
        } catch (Exception ex) {
            throw tratarErro("Erro ao buscar arquivo", ex);
        }
    }

    private ValidacaoException tratarErro(String mensagem, Exception ex) {
        log.error(mensagem, ex);
        return new ValidacaoException(NordHttpEnum.HTTP_500, mensagem, ExceptionUtils.getRootCauseMessage(ex));
    }
}
