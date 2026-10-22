package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.domain.CronogramaSemanal;
import br.com.nord_tool_backend.dto.CronogramaSemanalDto;
import br.com.nord_tool_backend.dto.CronogramaSemanalExcecaoDto;
import br.com.nord_tool_backend.repository.CronogramaSemanalRepository;
import br.com.nord_tool_backend.repository.jdbc.JdbcExecutor;
import br.com.nord_tool_backend.repository.jdbc.JdbcSuporte;
import br.com.nord_tool_backend.repository.jdbc.SqlQueries;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public class CronogramaSemanalRepositoryImpl implements CronogramaSemanalRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcExecutor executor;
    private final String qInserir;
    private final String qAlterar;
    private final String qDeletar;
    private final String qBuscarPorId;
    private final String qListar;
    private final String qListarExcecoes;
    private final String qInserirExcecao;
    private final String qDeletarExcecao;

    private final String qLimparCompartilhamento;
    private final String qInserirCompartilhamento;
    private final String qPessoas;
    private final String qUsuariosAtivos;
    private final br.com.nord_tool_backend.service.AutorizacaoService autorizacao;

    public CronogramaSemanalRepositoryImpl(NamedParameterJdbcTemplate jdbc, JdbcExecutor executor, SqlQueries queries,
                                           br.com.nord_tool_backend.service.AutorizacaoService autorizacao) {
        this.jdbc = jdbc;
        this.autorizacao = autorizacao;
        this.qLimparCompartilhamento = queries.get("SPD.CRONOGRAMA_SEMANAL_COMPARTILHAMENTO.LIMPAR");
        this.qInserirCompartilhamento = queries.get("SPI.CRONOGRAMA_SEMANAL_COMPARTILHAMENTO.INSERIR");
        this.qPessoas = queries.get("SPS.CRONOGRAMA_SEMANAL.PESSOAS");
        this.qUsuariosAtivos = queries.get("SPS.CRONOGRAMA_SEMANAL.USUARIOS_ATIVOS");
        this.executor = executor;
        this.qInserir = queries.get("SPI.CRONOGRAMA_SEMANAL.INSERIR");
        this.qAlterar = queries.get("SPU.CRONOGRAMA_SEMANAL.ALTERAR");
        this.qDeletar = queries.get("SPD.CRONOGRAMA_SEMANAL.DELETAR");
        this.qBuscarPorId = queries.get("SPS.CRONOGRAMA_SEMANAL.BUSTAR_POR_ID");
        this.qListar = queries.get("SPS.CRONOGRAMA_SEMANAL.LISTAR");
        this.qListarExcecoes = queries.get("SPS.CRONOGRAMA_SEMANAL_EXCECAO.LISTAR");
        this.qInserirExcecao = queries.get("SPI.CRONOGRAMA_SEMANAL_EXCECAO.INSERIR");
        this.qDeletarExcecao = queries.get("SPD.CRONOGRAMA_SEMANAL_EXCECAO.DELETAR");
    }

    @Override
    public CronogramaSemanalDto salvarCronogramaSemanal(CronogramaSemanal cronogramaSemanal) {
        return executor.executar("Salvar cronograma semanal", () -> {
            Long id = JdbcSuporte.inserir(jdbc, qInserir, comUsuario(new BeanPropertySqlParameterSource(cronogramaSemanal)),
                    "id_cronograma_semanal");
            return CronogramaSemanalDto.converterToDto(buscar(id));
        });
    }

    @Override
    public CronogramaSemanalDto alterarCronogramaSemanal(CronogramaSemanal cronogramaSemanal) {
        return executor.executar("Alterar cronograma semanal", () -> {
            if (jdbc.update(qAlterar, comUsuario(new BeanPropertySqlParameterSource(cronogramaSemanal))) == 0) throw naoEDono();
            return CronogramaSemanalDto.converterToDto(buscar(cronogramaSemanal.getId()));
        });
    }

    @Override
    public void deletarCronogramaSemanal(Long id) {
        executor.executar("Excluir cronograma semanal", () -> {
            if (jdbc.update(qDeletar, usuario(new MapSqlParameterSource("id", id))) == 0) throw naoEDono();
        });
    }

    @Override
    public CronogramaSemanal buscarPorIdCronogramaSemanal(Long id) {
        return executor.executar("Buscar cronograma semanal", () -> buscar(id));
    }

    @Override
    public List<CronogramaSemanal> listarCronogramaSemanal() {
        return executor.executar("Listar cronogramas semanais", () ->
                jdbc.query(qListar, usuario(new MapSqlParameterSource()), BeanPropertyRowMapper.newInstance(CronogramaSemanal.class)));
    }

    @Override
    public List<CronogramaSemanalExcecaoDto> listarExcecoes() {
        return executor.executar("Listar exceções do cronograma semanal", () ->
                jdbc.query(qListarExcecoes, usuario(new MapSqlParameterSource()), BeanPropertyRowMapper.newInstance(CronogramaSemanalExcecaoDto.class)));
    }

    @Override
    public void incluirExcecao(Long id, LocalDate data) {
        executor.executar("Incluir exceção do cronograma semanal", () ->
                jdbc.update(qInserirExcecao, new MapSqlParameterSource("id", id).addValue("data", java.sql.Date.valueOf(data))));
    }

    @Override
    public void removerExcecao(Long id, LocalDate data) {
        executor.executar("Remover exceção do cronograma semanal", () ->
                jdbc.update(qDeletarExcecao, new MapSqlParameterSource("id", id).addValue("data", java.sql.Date.valueOf(data))));
    }

    private CronogramaSemanal buscar(Long id) {
        return jdbc.queryForObject(qBuscarPorId, usuario(new MapSqlParameterSource("id", id)),
                BeanPropertyRowMapper.newInstance(CronogramaSemanal.class));
    }

    @Override
    public void compartilhar(Long id, java.util.List<Long> idsUsuarios) {
        executor.executar("Compartilhar item do cronograma", () -> {
            jdbc.update(qLimparCompartilhamento, new MapSqlParameterSource("id", id));
            for (Long destino : idsUsuarios) {
                jdbc.update(qInserirCompartilhamento, new MapSqlParameterSource("id", id).addValue("idUsuarioDestino", destino));
            }
        });
    }

    @Override
    public List<br.com.nord_tool_backend.dto.PessoaCronogramaDto> listarPessoas() {
        return executor.executar("Listar pessoas para compartilhar", () ->
                jdbc.query(qPessoas, usuario(new MapSqlParameterSource()), BeanPropertyRowMapper.newInstance(br.com.nord_tool_backend.dto.PessoaCronogramaDto.class)));
    }

    @Override
    public int contarUsuariosAtivos(java.util.Collection<Long> ids) {
        if (ids.isEmpty()) return 0;
        return executor.executar("Conferir usuários", () -> {
            Integer n = jdbc.queryForObject(qUsuariosAtivos, new MapSqlParameterSource("ids", ids), Integer.class);
            return n == null ? 0 : n;
        });
    }

    /**
     * Cronograma pessoal: o usuário logado (dono e destinatário dos compartilhamentos). Admin do módulo também
     * enxerga e altera os itens sem dono, que vieram de antes do cronograma pessoal.
     */
    private MapSqlParameterSource usuario(MapSqlParameterSource params) {
        br.com.nord_tool_backend.security.UsuarioAutenticado u = autorizacao.exigirAutenticado();
        return params.addValue("idUsuario", u.getId())
                .addValue("verSemDono", u.acao(br.com.nord_tool_backend.security.Modulo.CRONOGRAMA)
                        .atende(br.com.nord_tool_backend.security.Acao.ADMIN));
    }

    private org.springframework.jdbc.core.namedparam.SqlParameterSource comUsuario(org.springframework.jdbc.core.namedparam.SqlParameterSource base) {
        return new br.com.nord_tool_backend.repository.jdbc.ParametrosComEscopo(base, usuario(new MapSqlParameterSource()));
    }

    private static br.com.nord_tool_backend.exception.AcessoNegadoException naoEDono() {
        return new br.com.nord_tool_backend.exception.AcessoNegadoException("Só quem criou o item pode alterá-lo ou excluí-lo");
    }
}
