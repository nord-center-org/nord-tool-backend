package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.controller.response.NordHttpEnum;
import br.com.nord_tool_backend.domain.ApartamentoVistoria;
import br.com.nord_tool_backend.domain.Ferramenta;
import br.com.nord_tool_backend.domain.ObraControleChaves;
import br.com.nord_tool_backend.domain.RequisicaoChave;
import br.com.nord_tool_backend.domain.RequisicaoChaveConsulta;
import br.com.nord_tool_backend.excepetion.ValidacaoException;
import br.com.nord_tool_backend.repository.ControleChavesRepository;
import br.com.nord_tool_backend.repository.RepositoryJdbcOperationsSql;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@Slf4j
@PropertySource("classpath:query/controle-chaves.properties")
public class ControleChavesRepositoryImpl extends RepositoryJdbcOperationsSql<RequisicaoChave> implements ControleChavesRepository {

    @Value("${SPS.CONTROLE_CHAVES.LISTAR_OBRAS}")
    private String queryListarObras;

    @Value("${SPS.CONTROLE_CHAVES.LISTAR_APARTAMENTOS}")
    private String queryListarApartamentos;

    @Value("${SPS.CONTROLE_CHAVES.LISTAR_FERRAMENTAS}")
    private String queryListarFerramentas;

    @Value("${SPS.CONTROLE_CHAVES.COUNT_EM_CAMPO}")
    private String queryCountEmCampo;

    @Value("${SPS.CONTROLE_CHAVES.COUNT_NO_QUADRO}")
    private String queryCountNoQuadro;

    @Value("${SPS.CONTROLE_CHAVES.COUNT_ENTREGUES}")
    private String queryCountEntregues;

    @Value("${SPS.CONTROLE_CHAVES.LISTAR_HISTORICO}")
    private String queryListarHistorico;

    @Value("${SPS.CONTROLE_CHAVES.BUSCAR_POR_ID}")
    private String queryBuscarPorId;

    @Value("${SPI.CONTROLE_CHAVES.INSERIR}")
    private String queryInserir;

    @Value("${SPU.CONTROLE_CHAVES.RECEBER}")
    private String queryReceber;

    @Override
    public List<ObraControleChaves> listarObras() {
        try {
            return buscarTodos(queryListarObras, BeanPropertyRowMapper.newInstance(ObraControleChaves.class));
        } catch (Exception ex) {
            throw tratarErro("Erro ao listar obras", ex);
        }
    }

    @Override
    public List<ApartamentoVistoria> listarApartamentos() {
        try {
            return buscarTodos(queryListarApartamentos, BeanPropertyRowMapper.newInstance(ApartamentoVistoria.class));
        } catch (Exception ex) {
            throw tratarErro("Erro ao listar apartamentos para controle de chaves", ex);
        }
    }

    @Override
    public List<Ferramenta> listarFerramentas() {
        try {
            return buscarTodos(queryListarFerramentas, BeanPropertyRowMapper.newInstance(Ferramenta.class));
        } catch (Exception ex) {
            throw tratarErro("Erro ao listar ferramentas para controle de chaves", ex);
        }
    }

    @Override
    public Long contarChavesEmCampo() {
        try {
            return namedParameterJdbcTemplate.queryForObject(queryCountEmCampo, new MapSqlParameterSource(), Long.class);
        } catch (Exception ex) {
            throw tratarErro("Erro ao contar chaves em campo", ex);
        }
    }

    @Override
    public Long contarChavesNoQuadro() {
        try {
            return namedParameterJdbcTemplate.queryForObject(queryCountNoQuadro, new MapSqlParameterSource(), Long.class);
        } catch (Exception ex) {
            throw tratarErro("Erro ao contar chaves no quadro", ex);
        }
    }

    @Override
    public Long contarChavesEntregues() {
        try {
            return namedParameterJdbcTemplate.queryForObject(queryCountEntregues, new MapSqlParameterSource(), Long.class);
        } catch (Exception ex) {
            throw tratarErro("Erro ao contar chaves entregues", ex);
        }
    }

    @Override
    public List<RequisicaoChaveConsulta> listarHistorico() {
        try {
            return buscarTodos(queryListarHistorico,
                    BeanPropertyRowMapper.newInstance(RequisicaoChaveConsulta.class));
        } catch (Exception ex) {
            throw tratarErro("Erro ao listar histórico de controle de chaves", ex);
        }
    }

    @Override
    public RequisicaoChaveConsulta buscarPorId(Long idRequisicao) {
        try {
            return buscarPorId(queryBuscarPorId, new MapSqlParameterSource("idRequisicao", idRequisicao), BeanPropertyRowMapper.newInstance(RequisicaoChaveConsulta.class));
        } catch (Exception ex) {
            throw tratarErro("Erro ao buscar requisição de chave por id", ex);
        }
    }

    @Override
    public Long criarRetirada(RequisicaoChave requisicaoChave) {
        try {
            RequisicaoChave salva = salvar(queryInserir, requisicaoChave, "id_requisicao");
            return salva.getId();
        } catch (Exception ex) {
            throw tratarErro("Erro ao criar retirada de chave", ex);
        }
    }

    @Override
    public void receberRetirada(Long idRequisicao, Long idUserRecebimento, LocalDateTime dtRecebimento, String nmStatusRequisicao) {
        try {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("idRequisicao", idRequisicao)
                    .addValue("idUserRecebimento", idUserRecebimento)
                    .addValue("dtRecebimento", dtRecebimento)
                    .addValue("nmStatusRequisicao", nmStatusRequisicao);

            int atualizadas = namedParameterJdbcTemplate.update(queryReceber, params);
            if (atualizadas == 0) {throw new IllegalStateException("A retirada não existe ou não está aberta para recebimento");}
        } catch (Exception ex) {
            throw tratarErro("Erro ao receber retirada de chave", ex);
        }
    }

    private ValidacaoException tratarErro(String mensagem, Exception ex) {
        log.error(mensagem, ex);
        return new ValidacaoException(NordHttpEnum.HTTP_400, mensagem, ExceptionUtils.getMessage(ex));
    }
}
