package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.controller.response.NordHttpEnum;
import br.com.nord_tool_backend.domain.Cargo;
import br.com.nord_tool_backend.excepetion.ValidacaoException;
import br.com.nord_tool_backend.repository.CargoRepository;
import br.com.nord_tool_backend.repository.RepositoryJdbcOperationsSql;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository @Slf4j
@PropertySource("classpath:query/cargo.properties")
public class CargoRepositoryImpl extends RepositoryJdbcOperationsSql<Cargo> implements CargoRepository {
    @Value("${SPS.CARGO.LISTAR}") private String queryListar;
    @Value("${SPI.CARGO.INSERIR}") private String queryInserir;
    @Value("${SPU.CARGO.ALTERAR}") private String queryAlterar;
    @Value("${SPD.CARGO.DELETAR}") private String queryDeletar;

    public List<Cargo> listarCargos() {
        try {
            return buscarTodos(queryListar, BeanPropertyRowMapper.newInstance(Cargo.class));
        } catch (Exception ex) {
            throw tratarErro("Erro ao listar cargos", ex);
        }
    }

    @Override
    public Cargo salvarCargo(Cargo cargo) {
        try {
            return salvar(queryInserir, cargo, "id_cargo");
        } catch (Exception ex) {
            throw tratarErro("Erro ao salvar cargo", ex);
        }
    }

    @Override
    public Cargo alterarCargo(Cargo cargo) {
        try {
            return alterar(queryAlterar, cargo);
        } catch (Exception ex) {
            throw tratarErro("Erro ao alterar cargo", ex);
        }
    }

    @Override
    public void deletarCargo(Long id) {
        try {
            deletar(queryDeletar, new MapSqlParameterSource("id", id));
        } catch (Exception ex) {
            throw tratarErro("Erro ao deletar cargo", ex);
        }
    }

    private ValidacaoException tratarErro(String mensagem, Exception ex) {
        log.error(mensagem, ex);
        return new ValidacaoException(NordHttpEnum.HTTP_400, mensagem, ExceptionUtils.getMessage(ex));
    }
}
