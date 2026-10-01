package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.controller.response.NordHttpEnum;
import br.com.nord_tool_backend.domain.Empresa;
import br.com.nord_tool_backend.excepetion.ValidacaoException;
import br.com.nord_tool_backend.repository.EmpresaRepository;
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
@PropertySource("classpath:query/empresa.properties")
public class EmpresaRepositoryImpl extends RepositoryJdbcOperationsSql<Empresa> implements EmpresaRepository {
    @Value("${SPS.EMPRESA.LISTAR}") private String queryListar;
    @Value("${SPI.EMPRESA.INSERIR}") private String queryInserir;
    @Value("${SPU.EMPRESA.ALTERAR}") private String queryAlterar;
    @Value("${SPD.EMPRESA.DELETAR}") private String queryDeletar;

    public List<Empresa> listarEmpresas() {
        try {
            return buscarTodos(queryListar, BeanPropertyRowMapper.newInstance(Empresa.class));
        } catch (Exception ex) {
            throw tratarErro("Erro ao listar empresas", ex);
        }
    }

    @Override
    public Empresa salvarEmpresa(Empresa empresa) {
        try {
            return salvar(queryInserir, empresa, "id_empresa");
        } catch (Exception ex) {
            throw tratarErro("Erro ao salvar empresa", ex);
        }
    }

    @Override
    public Empresa alterarEmpresa(Empresa empresa) {
        try {
            return alterar(queryAlterar, empresa);
        } catch (Exception ex) {
            throw tratarErro("Erro ao alterar empresa", ex);
        }
    }

    @Override
    public void deletarEmpresa(Long id) {
        try {
            deletar(queryDeletar, new MapSqlParameterSource("id", id));
        } catch (Exception ex) {
            throw tratarErro("Erro ao deletar empresa", ex);
        }
    }

    private ValidacaoException tratarErro(String mensagem, Exception ex) {
        log.error(mensagem, ex);
        return new ValidacaoException(NordHttpEnum.HTTP_400, mensagem, ExceptionUtils.getMessage(ex));
    }
}
