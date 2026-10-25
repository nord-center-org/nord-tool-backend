package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.dto.LugiaConversaDto;
import br.com.nord_tool_backend.dto.LugiaMensagemDto;
import br.com.nord_tool_backend.dto.LugiaParteDto;
import br.com.nord_tool_backend.repository.LugiaRepository;
import br.com.nord_tool_backend.repository.jdbc.JdbcExecutor;
import br.com.nord_tool_backend.repository.jdbc.SqlQueries;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Types;
import java.util.List;
import java.util.Optional;

/**
 * Cada operação tem a própria transação: o chat transmite a resposta por minutos e não pode segurar uma
 * transação aberta (o pool roda com auto-commit desligado).
 */
@Repository
@Transactional
public class LugiaRepositoryImpl implements LugiaRepository {

    private static final TypeReference<List<LugiaParteDto>> PARTES = new TypeReference<List<LugiaParteDto>>() { };

    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcExecutor executor;
    private final ObjectMapper json;
    private final String qConversas;
    private final String qCriar;
    private final String qTitulo;
    private final String qSessao;
    private final String qTocar;
    private final String qExcluir;
    private final String qMensagens;
    private final String qMensagem;
    private final String qConfig;
    private final String qGravarConfig;

    public LugiaRepositoryImpl(NamedParameterJdbcTemplate jdbc, JdbcExecutor executor, SqlQueries queries, ObjectMapper json) {
        this.jdbc = jdbc;
        this.executor = executor;
        this.json = json;
        this.qConversas = queries.get("SPS.LUGIA.CONVERSAS");
        this.qCriar = queries.get("SPI.LUGIA.CONVERSA");
        this.qTitulo = queries.get("SPU.LUGIA.CONVERSA.TITULO");
        this.qSessao = queries.get("SPU.LUGIA.CONVERSA.SESSAO");
        this.qTocar = queries.get("SPU.LUGIA.CONVERSA.TOCAR");
        this.qExcluir = queries.get("SPD.LUGIA.CONVERSA");
        this.qMensagens = queries.get("SPS.LUGIA.MENSAGENS");
        this.qMensagem = queries.get("SPI.LUGIA.MENSAGEM");
        this.qConfig = queries.get("SPS.LUGIA.CONFIGURACAO");
        this.qGravarConfig = queries.get("SPI.LUGIA.CONFIGURACAO");
    }

    @Override
    public List<LugiaConversaDto> listarConversas(Long idUsuario) {
        return executor.executar("Erro ao listar conversas da Lugia", () -> jdbc.query(qConversas,
                new MapSqlParameterSource("idUsuario", idUsuario).addValue("id", null, Types.BIGINT),
                BeanPropertyRowMapper.newInstance(LugiaConversaDto.class)));
    }

    @Override
    public Optional<LugiaConversaDto> buscarConversa(Long idUsuario, Long idConversa) {
        return executor.executar("Erro ao buscar conversa da Lugia", () -> jdbc.query(qConversas,
                new MapSqlParameterSource("idUsuario", idUsuario).addValue("id", idConversa, Types.BIGINT),
                BeanPropertyRowMapper.newInstance(LugiaConversaDto.class)).stream().findFirst());
    }

    @Override
    public Long criarConversa(Long idUsuario, String titulo) {
        return executor.executar("Erro ao criar conversa da Lugia", () -> jdbc.queryForObject(qCriar,
                new MapSqlParameterSource("idUsuario", idUsuario).addValue("titulo", titulo), Long.class));
    }

    @Override
    public boolean renomear(Long idUsuario, Long idConversa, String titulo) {
        return executor.executar("Erro ao renomear conversa da Lugia", () -> jdbc.update(qTitulo,
                new MapSqlParameterSource("idUsuario", idUsuario).addValue("id", idConversa).addValue("titulo", titulo)) > 0);
    }

    @Override
    public boolean excluir(Long idUsuario, Long idConversa) {
        return executor.executar("Erro ao excluir conversa da Lugia", () -> jdbc.update(qExcluir,
                new MapSqlParameterSource("idUsuario", idUsuario).addValue("id", idConversa)) > 0);
    }

    @Override
    public void definirSessaoAgente(Long idConversa, String sessao) {
        executor.executar("Erro ao gravar sessão do agente", () -> {
            jdbc.update(qSessao, new MapSqlParameterSource("id", idConversa).addValue("sessao", sessao));
        });
    }

    @Override
    public List<LugiaMensagemDto> mensagens(Long idConversa) {
        return executor.executar("Erro ao ler mensagens da Lugia", () -> jdbc.query(qMensagens,
                new MapSqlParameterSource("idConversa", idConversa), (rs, i) -> new LugiaMensagemDto(
                        rs.getString("papel"), rs.getString("provedor"), lerPartes(rs.getString("partes")), rs.getString("nativo"))));
    }

    @Override
    public void adicionarMensagem(Long idConversa, LugiaMensagemDto m, String modelo, Integer tokensEntrada, Integer tokensSaida) {
        executor.executar("Erro ao gravar mensagem da Lugia", () -> {
            jdbc.update(qMensagem, new MapSqlParameterSource("idConversa", idConversa)
                    .addValue("papel", m.getPapel())
                    .addValue("provedor", m.getProvedor())
                    .addValue("modelo", modelo)
                    .addValue("partes", escrever(m.getPartes()))
                    .addValue("nativo", m.getNativo(), Types.VARCHAR)
                    .addValue("tokensEntrada", tokensEntrada, Types.INTEGER)
                    .addValue("tokensSaida", tokensSaida, Types.INTEGER));
            jdbc.update(qTocar, new MapSqlParameterSource("id", idConversa));
        });
    }

    @Override
    public Optional<String> lerConfiguracao(String chave) {
        return executor.executar("Erro ao ler configuração da Lugia", () ->
                jdbc.queryForList(qConfig, new MapSqlParameterSource("chave", chave), String.class).stream().findFirst());
    }

    @Override
    public void gravarConfiguracao(String chave, String valor) {
        executor.executar("Erro ao gravar configuração da Lugia", () -> {
            jdbc.update(qGravarConfig, new MapSqlParameterSource("chave", chave).addValue("valor", valor));
        });
    }

    private List<LugiaParteDto> lerPartes(String texto) {
        try {
            return json.readValue(texto, PARTES);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Partes da mensagem ilegíveis", e);
        }
    }

    private String escrever(List<LugiaParteDto> partes) {
        try {
            return json.writeValueAsString(partes);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Não foi possível gravar as partes da mensagem", e);
        }
    }
}
