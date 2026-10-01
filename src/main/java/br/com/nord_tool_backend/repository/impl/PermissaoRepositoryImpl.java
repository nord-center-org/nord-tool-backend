package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.controller.response.NordHttpEnum;
import br.com.nord_tool_backend.domain.Permissao;
import br.com.nord_tool_backend.excepetion.ValidacaoException;
import br.com.nord_tool_backend.repository.PermissaoRepository;
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
@PropertySource("classpath:query/permissao.properties")
public class PermissaoRepositoryImpl extends RepositoryJdbcOperationsSql<Permissao> implements PermissaoRepository {
    @Value("${SPS.PERMISSAO.LISTAR}") private String queryListar;
    @Value("${SPI.PERMISSAO.INSERIR}") private String queryInserir;
    @Value("${SPU.PERMISSAO.ALTERAR}") private String queryAlterar;
    @Value("${SPD.PERMISSAO.DELETAR}") private String queryDeletar;

    public List<Permissao> listarPermissoes() {
        try {
            return buscarTodos(queryListar, BeanPropertyRowMapper.newInstance(Permissao.class));
        } catch (Exception ex) {
            throw tratarErro("Erro ao listar permissões", ex);
        }
    }

    @Override
    public Permissao salvarPermissao(Permissao permissao) {
        try {
            return salvar(queryInserir, permissao, "id_permissao");
        } catch (Exception ex) {
            throw tratarErro("Erro ao salvar permissão", ex);
        }
    }

    @Override
    public Permissao alterarPermissao(Permissao permissao) {
        try {
            return alterar(queryAlterar, permissao);
        } catch (Exception ex) {
            throw tratarErro("Erro ao alterar permissão", ex);
        }
    }

    @Override
    public void deletarPermissao(Long id) {
        try {
            deletar(queryDeletar, new MapSqlParameterSource("id", id));
        } catch (Exception ex) {
            throw tratarErro("Erro ao deletar permissão", ex);
        }
    }

    private ValidacaoException tratarErro(String mensagem, Exception ex) {
        log.error(mensagem, ex);
        return new ValidacaoException(NordHttpEnum.HTTP_400, mensagem, ExceptionUtils.getMessage(ex));
    }
}
