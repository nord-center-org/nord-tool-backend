package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.dto.DemandaAvisoDto;
import br.com.nord_tool_backend.dto.NotificacaoPreferenciaDto;
import br.com.nord_tool_backend.dto.PushInscricaoDto;
import br.com.nord_tool_backend.repository.NotificacaoRepository;
import br.com.nord_tool_backend.repository.jdbc.JdbcExecutor;
import br.com.nord_tool_backend.repository.jdbc.SqlQueries;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** O pool roda com auto-commit desligado: cada operação tem a própria transação. */
@Repository
@Transactional
public class NotificacaoRepositoryImpl implements NotificacaoRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcExecutor executor;
    private final SqlQueries queries;

    private static final BeanPropertyRowMapper<PushInscricaoDto> INSCRICAO = BeanPropertyRowMapper.newInstance(PushInscricaoDto.class);
    private static final BeanPropertyRowMapper<NotificacaoPreferenciaDto> PREFERENCIA = BeanPropertyRowMapper.newInstance(NotificacaoPreferenciaDto.class);
    private static final BeanPropertyRowMapper<DemandaAvisoDto> DEMANDA = BeanPropertyRowMapper.newInstance(DemandaAvisoDto.class);

    public NotificacaoRepositoryImpl(NamedParameterJdbcTemplate jdbc, JdbcExecutor executor, SqlQueries queries) {
        this.jdbc = jdbc;
        this.executor = executor;
        this.queries = queries;
        // Falha cedo se alguma query faltar no catálogo.
        for (String chave : new String[]{"SPS.PUSH.CHAVES", "SPI.PUSH.INSCREVER", "SPS.NOTIFICACAO.DEMANDA", "SPI.NOTIFICACAO.ENVIADA"}) {
            queries.get(chave);
        }
    }

    private String q(String chave) {
        return queries.get(chave);
    }

    @Override
    public Optional<String[]> chaves() {
        return executor.executar("Erro ao ler as chaves de notificação", () ->
                jdbc.query(q("SPS.PUSH.CHAVES"), new MapSqlParameterSource(), (rs, i) -> new String[]{rs.getString(1), rs.getString(2)}).stream().findFirst());
    }

    @Override
    public void salvarChaves(String publica, String privada) {
        executor.executar("Erro ao guardar as chaves de notificação", () ->
                jdbc.update(q("SPI.PUSH.CHAVES"), new MapSqlParameterSource("publica", publica).addValue("privada", privada)));
    }

    @Override
    public void inscrever(Long idUsuario, String endpoint, String p256dh, String auth, String aparelho) {
        executor.executar("Erro ao ativar as notificações", () ->
                jdbc.update(q("SPI.PUSH.INSCREVER"), new MapSqlParameterSource("idUsuario", idUsuario).addValue("endpoint", endpoint)
                        .addValue("p256dh", p256dh).addValue("auth", auth).addValue("aparelho", aparelho)));
    }

    @Override
    public int cancelar(Long idUsuario, String endpoint) {
        return executor.executar("Erro ao desativar as notificações", () ->
                jdbc.update(q("SPD.PUSH.CANCELAR"), new MapSqlParameterSource("idUsuario", idUsuario).addValue("endpoint", endpoint)));
    }

    @Override
    public int removerEndpoint(String endpoint) {
        return executor.executar("Erro ao remover aparelho", () ->
                jdbc.update(q("SPD.PUSH.ENDPOINT"), new MapSqlParameterSource("endpoint", endpoint)));
    }

    @Override
    public List<PushInscricaoDto> inscricoes(Collection<Long> idsUsuarios) {
        if (idsUsuarios.isEmpty()) return Collections.emptyList();
        return executor.executar("Erro ao listar aparelhos", () ->
                jdbc.query(q("SPS.PUSH.INSCRICOES"), new MapSqlParameterSource("ids", new ArrayList<>(idsUsuarios)), INSCRICAO));
    }

    @Override
    public void marcarEnvio(Long idInscricao) {
        executor.executar("Erro ao registrar envio", () -> jdbc.update(q("SPU.PUSH.ENVIO"), new MapSqlParameterSource("id", idInscricao)));
    }

    @Override
    public Optional<NotificacaoPreferenciaDto> preferencia(Long idUsuario) {
        return preferencias(Collections.singletonList(idUsuario)).stream().findFirst();
    }

    @Override
    public List<NotificacaoPreferenciaDto> preferencias(Collection<Long> idsUsuarios) {
        if (idsUsuarios.isEmpty()) return Collections.emptyList();
        return executor.executar("Erro ao ler preferências de notificação", () ->
                jdbc.query(q("SPS.NOTIFICACAO.PREFERENCIAS"), new MapSqlParameterSource("ids", new ArrayList<>(idsUsuarios)), PREFERENCIA));
    }

    @Override
    public void salvarPreferencia(NotificacaoPreferenciaDto p) {
        executor.executar("Erro ao salvar preferências de notificação", () ->
                jdbc.update(q("SPI.NOTIFICACAO.PREFERENCIA"), new MapSqlParameterSource("idUsuario", p.getIdUsuario())
                        .addValue("nova", p.isInDemandaNova()).addValue("alterada", p.isInDemandaAlterada()).addValue("horario", p.isInHorario())
                        .addValue("antecedencia", p.getNrAntecedenciaMin()).addValue("prazo", p.isInPrazo()).addValue("sac", p.isInSac())
                        .addValue("minhas", p.isInMinhasAcoes())));
    }

    @Override
    public boolean registrarEnvio(Long idUsuario, String chave) {
        return executor.executar("Erro ao registrar aviso", () ->
                jdbc.update(q("SPI.NOTIFICACAO.ENVIADA"), new MapSqlParameterSource("idUsuario", idUsuario).addValue("chave", chave)) == 1);
    }

    @Override
    public int limparEnviadas(LocalDateTime antesDe) {
        return executor.executar("Erro ao limpar avisos antigos", () ->
                jdbc.update(q("SPD.NOTIFICACAO.ENVIADAS"), new MapSqlParameterSource("antes", antesDe)));
    }

    @Override
    public Optional<DemandaAvisoDto> demanda(Long id) {
        return executor.executar("Erro ao buscar a demanda do aviso", () ->
                jdbc.query(q("SPS.NOTIFICACAO.DEMANDA"), new MapSqlParameterSource("id", id), DEMANDA).stream().findFirst());
    }

    @Override
    public List<DemandaAvisoDto> demandasAgendadas(LocalDateTime de, LocalDateTime ate) {
        return executor.executar("Erro ao buscar demandas agendadas", () ->
                jdbc.query(q("SPS.NOTIFICACAO.AGENDADAS"), new MapSqlParameterSource("de", de).addValue("ate", ate), DEMANDA));
    }

    @Override
    public List<DemandaAvisoDto> demandasComPrazo(LocalDate dia) {
        return executor.executar("Erro ao buscar prazos do dia", () ->
                jdbc.query(q("SPS.NOTIFICACAO.PRAZOS"), new MapSqlParameterSource("inicio", dia.atStartOfDay()).addValue("fim", dia.plusDays(1).atStartOfDay()), DEMANDA));
    }

    @Override
    public String nomeDoUsuario(Long id) {
        return executor.executar("Erro ao buscar o nome", () ->
                jdbc.query(q("SPS.NOTIFICACAO.NOME"), new MapSqlParameterSource("id", id), (rs, i) -> rs.getString(1)).stream().findFirst().orElse("Alguém"));
    }
}
