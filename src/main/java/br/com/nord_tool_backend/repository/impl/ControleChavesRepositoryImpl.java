package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.controller.response.NordHttpEnum;
import br.com.nord_tool_backend.domain.RequisicaoChave;
import br.com.nord_tool_backend.dto.ApartamentoControleChavesDto;
import br.com.nord_tool_backend.dto.ObraControleChavesDto;
import br.com.nord_tool_backend.excepetion.ValidacaoException;
import br.com.nord_tool_backend.form.NovaRetiradaControleChavesForm;
import br.com.nord_tool_backend.form.RecebimentoControleChavesForm;
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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Repository
@Slf4j
@PropertySource("classpath:query/controle-chaves.properties")
public class ControleChavesRepositoryImpl extends RepositoryJdbcOperationsSql<RequisicaoChave> implements ControleChavesRepository {

    @Value("${SPS.CONTROLE_CHAVES.LISTAR_OBRAS}") private String queryListarObras;
    @Value("${SPS.CONTROLE_CHAVES.LISTAR_APARTAMENTOS}") private String queryListarApartamentos;
    @Value("${SPS.CONTROLE_CHAVES.COUNT_EM_CAMPO}") private String queryCountEmCampo;
    @Value("${SPS.CONTROLE_CHAVES.COUNT_NO_QUADRO}") private String queryCountNoQuadro;
    @Value("${SPS.CONTROLE_CHAVES.COUNT_ENTREGUES}") private String queryCountEntregues;
    @Value("${SPS.CONTROLE_CHAVES.LISTAR_HISTORICO}") private String queryListarHistorico;
    @Value("${SPS.CONTROLE_CHAVES.BUSCAR_POR_ID}") private String queryBuscarPorId;
    @Value("${SPI.CONTROLE_CHAVES.INSERIR}") private String queryInserir;
    @Value("${SPU.CONTROLE_CHAVES.RECEBER}") private String queryReceber;

    @Override
    public List<ObraControleChavesDto> listarObras() {
        try {
            return namedParameterJdbcTemplate.query(queryListarObras, (rs, rowNum) -> {
                String idObra = rs.getString("id_obra");
                return ObraControleChavesDto.builder()
                        .id(idObra)
                        .nome(idObra)
                        .build();
            });
        } catch (Exception ex) {
            throw tratarErro("Erro ao listar obras", ex);
        }
    }

    @Override
    public List<ApartamentoControleChavesDto> listarApartamentos(String busca, int limite, int pagina) {
        try {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("busca", busca)
                    .addValue("limite", limite > 0 ? limite : 20)
                    .addValue("offset", Math.max(0, pagina) * (limite > 0 ? limite : 20));

            return namedParameterJdbcTemplate.query(queryListarApartamentos, params, (rs, rowNum) ->
                    ApartamentoControleChavesDto.builder()
                            .id(rs.getLong("id"))
                            .label(rs.getString("label"))
                            .build());
        } catch (Exception ex) {
            throw tratarErro("Erro ao listar apartamentos para controle de chaves", ex);
        }
    }

    @Override
    public Long contarChavesEmCampo() {
        try {
            return namedParameterJdbcTemplate.queryForObject(queryCountEmCampo, new MapSqlParameterSource(), Long.class);
        } catch (Exception ex) {
            return 0L;
        }
    }

    @Override
    public Long contarChavesNoQuadro() {
        try {
            return namedParameterJdbcTemplate.queryForObject(queryCountNoQuadro, new MapSqlParameterSource(), Long.class);
        } catch (Exception ex) {
            return 0L;
        }
    }

    @Override
    public Long contarChavesEntregues() {
        try {
            return namedParameterJdbcTemplate.queryForObject(queryCountEntregues, new MapSqlParameterSource(), Long.class);
        } catch (Exception ex) {
            return 0L;
        }
    }

    @Override
    public List<RequisicaoChave> listarHistorico(String busca, String status, String idObra, int limite, int pagina) {
        try {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("busca", busca)
                    .addValue("status", status)
                    .addValue("idObra", idObra)
                    .addValue("limite", limite > 0 ? limite : 20)
                    .addValue("offset", Math.max(0, pagina) * (limite > 0 ? limite : 20));

            return namedParameterJdbcTemplate.query(queryListarHistorico, params, BeanPropertyRowMapper.newInstance(RequisicaoChave.class));
        } catch (Exception ex) {
            throw tratarErro("Erro ao listar histórico de controle de chaves", ex);
        }
    }

    @Override
    public RequisicaoChave buscarPorId(Long id) {
        try {
            return namedParameterJdbcTemplate.queryForObject(queryBuscarPorId,
                    new MapSqlParameterSource("id", id),
                    BeanPropertyRowMapper.newInstance(RequisicaoChave.class));
        } catch (Exception ex) {
            throw tratarErro("Erro ao buscar requisição de chave por id", ex);
        }
    }

    @Override
    public RequisicaoChave criarRetirada(NovaRetiradaControleChavesForm form) {
        try {
            String codigoStr = "RET-" + System.currentTimeMillis() % 1000000;
            RequisicaoChave req = RequisicaoChave.builder()
                    .cdRetirada(codigoStr)
                    .dtRetirada(LocalDateTime.now())
                    .idApartamentoVistoria(form.getIdApartamento())
                    .idUserRetirada(form.getIdRetirante())
                    .idUserLiberacao(form.getIdLiberador())
                    .stRequisicao("ABERTO")
                    .build();

            RequisicaoChave salva = salvar(queryInserir, req, "id_requisicao");
            return buscarPorId(salva.getId());
        } catch (Exception ex) {
            throw tratarErro("Erro ao criar retirada de chave", ex);
        }
    }

    @Override
    public RequisicaoChave receberRetirada(Long id, RecebimentoControleChavesForm form) {
        try {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("id", id)
                    .addValue("idUserRecebimento", form.getIdRecebedor())
                    .addValue("dtRecebimento", LocalDateTime.now());

            namedParameterJdbcTemplate.update(queryReceber, params);
            return buscarPorId(id);
        } catch (Exception ex) {
            throw tratarErro("Erro ao receber retirada de chave", ex);
        }
    }

    private ValidacaoException tratarErro(String mensagem, Exception ex) {
        log.error(mensagem, ex);
        return new ValidacaoException(NordHttpEnum.HTTP_400, mensagem, ExceptionUtils.getMessage(ex));
    }
}
