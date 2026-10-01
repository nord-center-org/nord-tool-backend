package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.controller.response.NordHttpEnum;
import br.com.nord_tool_backend.domain.Ferramenta;
import br.com.nord_tool_backend.dto.FerramentaDto;
import br.com.nord_tool_backend.excepetion.ValidacaoException;
import br.com.nord_tool_backend.repository.FerramentaRepository;
import br.com.nord_tool_backend.repository.RepositoryJdbcOperationsSql;
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
@PropertySource("classpath:query/ferramenta.properties")
public class FerramentaRepositoryImpl extends RepositoryJdbcOperationsSql<Ferramenta> implements FerramentaRepository {
    private static final String ERRO_SALVAR = "Erro ao salvar ferramenta";
    private static final String ERRO_ALTERAR = "Erro ao alterar ferramenta";
    private static final String ERRO_DELETAR = "Erro ao deletar ferramenta";
    private static final String ERRO_BUSCAR = "Erro ao buscar ferramenta";
    private static final String ERRO_LISTAR = "Erro ao listar ferramentas";

    @Value("${SPI.FERRAMENTA.INSERIR}") private String querySalvar;
    @Value("${SPU.FERRAMENTA.ALTERAR}") private String queryAlterar;
    @Value("${SPD.FERRAMENTA.DELETAR}") private String queryDeletar;
    @Value("${SPS.FERRAMENTA.BUSCAR_POR_ID}") private String queryBuscarPorId;
    @Value("${SPS.FERRAMENTA.LISTAR}") private String queryListar;

    @Override
    public FerramentaDto salvarFerramenta(Ferramenta ferramenta) {
        try {
            Ferramenta salva = salvar(querySalvar, ferramenta, "id_ferramenta");
            return FerramentaDto.converterToDto(buscarPorIdInterno(salva.getId()));
        } catch (Exception ex) {
            throw tratarErro(ERRO_SALVAR, ex);
        }
    }

    @Override
    public FerramentaDto alterarFerramenta(Ferramenta ferramenta) {
        try {
            alterar(queryAlterar, ferramenta);
            return FerramentaDto.converterToDto(buscarPorIdInterno(ferramenta.getId()));
        } catch (Exception ex) {
            throw tratarErro(ERRO_ALTERAR, ex);
        }
    }

    @Override
    public void deletarFerramenta(Long id) {
        try {
            deletar(queryDeletar, new MapSqlParameterSource("id", id));
        } catch (Exception ex) {
            throw tratarErro(ERRO_DELETAR, ex);
        }
    }

    @Override
    public Ferramenta buscarPorIdFerramenta(Long id) {
        try {
            return buscarPorIdInterno(id);
        } catch (Exception ex) {
            throw tratarErro(ERRO_BUSCAR, ex);
        }
    }

    @Override
    public List<Ferramenta> listarFerramentas() {
        try {
            return buscarTodos(queryListar, BeanPropertyRowMapper.newInstance(Ferramenta.class));
        } catch (Exception ex) {
            throw tratarErro(ERRO_LISTAR, ex);
        }
    }

    private Ferramenta buscarPorIdInterno(Long id) {
        return buscarPorId(queryBuscarPorId, new MapSqlParameterSource("id", id),
                BeanPropertyRowMapper.newInstance(Ferramenta.class));
    }

    private ValidacaoException tratarErro(String mensagem, Exception ex) {
        log.error(mensagem, ex);
        return new ValidacaoException(NordHttpEnum.HTTP_400, mensagem, ExceptionUtils.getMessage(ex));
    }
}
