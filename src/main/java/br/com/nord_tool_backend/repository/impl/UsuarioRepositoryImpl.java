package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.controller.response.NordHttpEnum;
import br.com.nord_tool_backend.domain.PerfilPermissao;
import br.com.nord_tool_backend.domain.Usuario;
import br.com.nord_tool_backend.excepetion.ValidacaoException;
import br.com.nord_tool_backend.repository.RepositoryJdbcOperationsSql;
import br.com.nord_tool_backend.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository @Slf4j
@PropertySource("classpath:query/usuario.properties")
public class UsuarioRepositoryImpl extends RepositoryJdbcOperationsSql<Usuario> implements UsuarioRepository {
    @Value("${SPS.USUARIO.BUSCAR_POR_EMAIL}") private String queryBuscarPorEmail;
    @Value("${SPS.USUARIO.BUSCAR_POR_ID}") private String queryBuscarPorId;
    @Value("${SPS.USUARIO.CONTAR}") private String queryContar;
    @Value("${SPS.USUARIO.PERMISSOES}") private String queryPermissoes;
    @Value("${SPS.USUARIO.ID_PERFIL_POR_CODIGO}") private String queryIdPerfil;
    @Value("${SPI.USUARIO.INSERIR}") private String queryInserir;
    @Value("${SPU.USUARIO.REGISTRAR_FALHA}") private String queryRegistrarFalha;
    @Value("${SPU.USUARIO.REGISTRAR_LOGIN}") private String queryRegistrarLogin;
    @Value("${SPU.USUARIO.ALTERAR_SENHA}") private String queryAlterarSenha;

    @Override
    public Optional<Usuario> buscarPorEmail(String email) {
        try {
            return buscarTodosPorFiltro(queryBuscarPorEmail, new MapSqlParameterSource("email", email),
                    BeanPropertyRowMapper.newInstance(Usuario.class)).stream().findFirst();
        } catch (Exception ex) {
            throw tratarErro("Erro ao buscar usuário", ex);
        }
    }

    @Override
    public Optional<Usuario> buscarPorId(Long id) {
        try {
            return buscarTodosPorFiltro(queryBuscarPorId, new MapSqlParameterSource("id", id),
                    BeanPropertyRowMapper.newInstance(Usuario.class)).stream().findFirst();
        } catch (Exception ex) {
            throw tratarErro("Erro ao buscar usuário", ex);
        }
    }

    @Override
    public long contar() {
        try {
            Long total = namedParameterJdbcTemplate.queryForObject(queryContar, new MapSqlParameterSource(), Long.class);
            return total == null ? 0 : total;
        } catch (Exception ex) {
            throw tratarErro("Erro ao contar usuários", ex);
        }
    }

    @Override
    public List<PerfilPermissao> listarPermissoes(Long idPerfil) {
        try {
            return buscarTodosPorFiltro(queryPermissoes, new MapSqlParameterSource("idPerfil", idPerfil),
                    BeanPropertyRowMapper.newInstance(PerfilPermissao.class));
        } catch (Exception ex) {
            throw tratarErro("Erro ao listar permissões do perfil", ex);
        }
    }

    @Override
    public Optional<Long> buscarIdPerfil(String cdPerfil) {
        try {
            return namedParameterJdbcTemplate.queryForList(queryIdPerfil,
                    new MapSqlParameterSource("cdPerfil", cdPerfil), Long.class).stream().findFirst();
        } catch (Exception ex) {
            throw tratarErro("Erro ao buscar perfil", ex);
        }
    }

    @Override
    public Usuario inserir(Usuario usuario) {
        try {
            return salvar(queryInserir, usuario, "id_usuario");
        } catch (Exception ex) {
            throw tratarErro("Erro ao salvar usuário", ex);
        }
    }

    @Override
    public void registrarFalha(Long id, int falhas, LocalDateTime bloqueadoAte) {
        try {
            namedParameterJdbcTemplate.update(queryRegistrarFalha, new MapSqlParameterSource()
                    .addValue("id", id)
                    .addValue("falhas", falhas)
                    .addValue("bloqueadoAte", bloqueadoAte == null ? null : Timestamp.valueOf(bloqueadoAte)));
        } catch (Exception ex) {
            throw tratarErro("Erro ao registrar falha de login", ex);
        }
    }

    @Override
    public void registrarLoginSucesso(Long id) {
        try {
            namedParameterJdbcTemplate.update(queryRegistrarLogin, new MapSqlParameterSource("id", id));
        } catch (Exception ex) {
            throw tratarErro("Erro ao registrar login", ex);
        }
    }

    @Override
    public void alterarSenha(Long id, String hash) {
        try {
            namedParameterJdbcTemplate.update(queryAlterarSenha,
                    new MapSqlParameterSource().addValue("id", id).addValue("hash", hash));
        } catch (Exception ex) {
            throw tratarErro("Erro ao alterar senha", ex);
        }
    }

    // Nunca inclui parâmetros (senha/hash) na mensagem.
    private ValidacaoException tratarErro(String mensagem, Exception ex) {
        log.error(mensagem, ex);
        return new ValidacaoException(NordHttpEnum.HTTP_500, mensagem, ExceptionUtils.getRootCauseMessage(ex));
    }
}
