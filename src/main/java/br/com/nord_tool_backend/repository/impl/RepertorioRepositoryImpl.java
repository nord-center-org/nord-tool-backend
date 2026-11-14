package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.dto.RepertorioCifraDto;
import br.com.nord_tool_backend.dto.RepertorioMusicaDto;
import br.com.nord_tool_backend.dto.RepertorioProjetoDto;
import br.com.nord_tool_backend.repository.RepertorioRepository;
import br.com.nord_tool_backend.repository.jdbc.JdbcExecutor;
import br.com.nord_tool_backend.repository.jdbc.SqlQueries;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Types;
import java.util.List;
import java.util.Optional;

/** O pool roda com auto-commit desligado: cada operação tem a própria transação (ou entra na do serviço). */
@Repository
@Transactional
public class RepertorioRepositoryImpl implements RepertorioRepository {

    private static final BeanPropertyRowMapper<RepertorioMusicaDto> MUSICA = BeanPropertyRowMapper.newInstance(RepertorioMusicaDto.class);
    private static final BeanPropertyRowMapper<RepertorioProjetoDto> PROJETO = BeanPropertyRowMapper.newInstance(RepertorioProjetoDto.class);
    private static final BeanPropertyRowMapper<RepertorioCifraDto> CIFRA = BeanPropertyRowMapper.newInstance(RepertorioCifraDto.class);
    private static final String USUARIO = "idUsuario";
    private static final String CHAVE = "chave";

    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcExecutor executor;
    private final SqlQueries queries;

    public RepertorioRepositoryImpl(NamedParameterJdbcTemplate jdbc, JdbcExecutor executor, SqlQueries queries) {
        this.jdbc = jdbc;
        this.executor = executor;
        this.queries = queries;
        for (String q : new String[]{"SPS.REPERTORIO.MUSICAS", "SPS.REPERTORIO.MUSICA", "SPS.REPERTORIO.MUSICAS.CONTAR", "SPS.REPERTORIO.BYTES",
                "SPI.REPERTORIO.MUSICA", "SPU.REPERTORIO.AUDIO", "SPD.REPERTORIO.MUSICA", "SPS.REPERTORIO.PROJETOS", "SPS.REPERTORIO.PROJETO",
                "SPS.REPERTORIO.PROJETOS.CONTAR", "SPI.REPERTORIO.PROJETO", "SPD.REPERTORIO.PROJETO", "SPI.REPERTORIO.CIFRA_ENVIADA",
                "SPS.REPERTORIO.CIFRAS_ENVIADAS.CONTAR_DESDE", "SPS.REPERTORIO.CIFRAS_RECEBIDAS", "SPS.REPERTORIO.CIFRA_RECEBIDA",
                "SPU.REPERTORIO.CIFRA_LIDA", "SPD.REPERTORIO.CIFRA_RECEBIDA"}) {
            queries.get(q);
        }
    }

    private static MapSqlParameterSource dono(Long idUsuario, String chave) {
        return new MapSqlParameterSource(USUARIO, idUsuario).addValue(CHAVE, chave);
    }

    @Override
    public List<RepertorioMusicaDto> listarMusicas(Long idUsuario) {
        return executor.executar("Erro ao listar as músicas", () ->
                jdbc.query(queries.get("SPS.REPERTORIO.MUSICAS"), new MapSqlParameterSource(USUARIO, idUsuario), MUSICA));
    }

    @Override
    public Optional<RepertorioMusicaDto> buscarMusica(Long idUsuario, String chave) {
        return executor.executar("Erro ao abrir a música", () ->
                jdbc.query(queries.get("SPS.REPERTORIO.MUSICA"), dono(idUsuario, chave), MUSICA).stream().findFirst());
    }

    @Override
    public int contarMusicas(Long idUsuario) {
        return executor.executar("Erro ao contar as músicas", () -> {
            Integer n = jdbc.queryForObject(queries.get("SPS.REPERTORIO.MUSICAS.CONTAR"), new MapSqlParameterSource(USUARIO, idUsuario), Integer.class);
            return n == null ? 0 : n;
        });
    }

    @Override
    public long bytesUsados(Long idUsuario, String excetoChave) {
        return executor.executar("Erro ao somar o espaço usado", () -> {
            Long n = jdbc.queryForObject(queries.get("SPS.REPERTORIO.BYTES"),
                    new MapSqlParameterSource(USUARIO, idUsuario).addValue("exceto", excetoChave, Types.VARCHAR), Long.class);
            return n == null ? 0L : n;
        });
    }

    @Override
    public void salvarMusica(Long idUsuario, String chave, RepertorioMusicaDto d) {
        executor.executar("Erro ao salvar a música", () ->
                jdbc.update(queries.get("SPI.REPERTORIO.MUSICA"), dono(idUsuario, chave)
                        .addValue("titulo", d.getNmTitulo()).addValue("artista", d.getNmArtista())
                        .addValue("duracao", d.getNrDuracao()).addValue("tom", d.getNmTom(), Types.VARCHAR)
                        .addValue("bpm", d.getNrBpm(), Types.NUMERIC).addValue("projeto", d.getCdProjeto(), Types.VARCHAR)
                        .addValue("dados", d.getTxDados()).addValue("arquivo", d.getNmArquivo())));
    }

    @Override
    public void ligarAudio(Long idUsuario, String chave, Long idArquivo, String nomeArquivo, long tamanho) {
        executor.executar("Erro ao guardar o áudio", () ->
                jdbc.update(queries.get("SPU.REPERTORIO.AUDIO"), dono(idUsuario, chave)
                        .addValue("idArquivo", idArquivo).addValue("nome", nomeArquivo).addValue("tamanho", tamanho)));
    }

    @Override
    public int excluirMusica(Long idUsuario, String chave) {
        return executor.executar("Erro ao excluir a música", () -> jdbc.update(queries.get("SPD.REPERTORIO.MUSICA"), dono(idUsuario, chave)));
    }

    @Override
    public List<RepertorioProjetoDto> listarProjetos(Long idUsuario) {
        return executor.executar("Erro ao listar as criações", () ->
                jdbc.query(queries.get("SPS.REPERTORIO.PROJETOS"), new MapSqlParameterSource(USUARIO, idUsuario), PROJETO));
    }

    @Override
    public Optional<RepertorioProjetoDto> buscarProjeto(Long idUsuario, String chave) {
        return executor.executar("Erro ao abrir a criação", () ->
                jdbc.query(queries.get("SPS.REPERTORIO.PROJETO"), dono(idUsuario, chave), PROJETO).stream().findFirst());
    }

    @Override
    public int contarProjetos(Long idUsuario) {
        return executor.executar("Erro ao contar as criações", () -> {
            Integer n = jdbc.queryForObject(queries.get("SPS.REPERTORIO.PROJETOS.CONTAR"), new MapSqlParameterSource(USUARIO, idUsuario), Integer.class);
            return n == null ? 0 : n;
        });
    }

    @Override
    public void salvarProjeto(Long idUsuario, String chave, String titulo, String dados) {
        executor.executar("Erro ao salvar a criação", () ->
                jdbc.update(queries.get("SPI.REPERTORIO.PROJETO"), dono(idUsuario, chave).addValue("titulo", titulo).addValue("dados", dados)));
    }

    @Override
    public int excluirProjeto(Long idUsuario, String chave) {
        return executor.executar("Erro ao excluir a criação", () -> jdbc.update(queries.get("SPD.REPERTORIO.PROJETO"), dono(idUsuario, chave)));
    }

    // ---------------------------------------------------------------- v12: cifras compartilhadas

    private static MapSqlParameterSource recebida(Long idUsuario, Long id) {
        return new MapSqlParameterSource(USUARIO, idUsuario).addValue("id", id);
    }

    @Override
    public void enviarCifra(Long origem, Long destino, String titulo, String artista, String tom, String cifra, String mensagem) {
        executor.executar("Erro ao compartilhar a cifra", () ->
                jdbc.update(queries.get("SPI.REPERTORIO.CIFRA_ENVIADA"), new MapSqlParameterSource("origem", origem).addValue("destino", destino)
                        .addValue("titulo", titulo).addValue("artista", artista).addValue("tom", tom, Types.VARCHAR)
                        .addValue("cifra", cifra).addValue("mensagem", mensagem)));
    }

    @Override
    public int contarCifrasEnviadasDesde(Long origem, java.time.LocalDateTime desde) {
        return executor.executar("Erro ao contar as cifras compartilhadas", () -> {
            Integer n = jdbc.queryForObject(queries.get("SPS.REPERTORIO.CIFRAS_ENVIADAS.CONTAR_DESDE"),
                    new MapSqlParameterSource(USUARIO, origem).addValue("desde", desde), Integer.class);
            return n == null ? 0 : n;
        });
    }

    @Override
    public List<RepertorioCifraDto> listarCifrasRecebidas(Long idUsuario) {
        return executor.executar("Erro ao listar as cifras recebidas", () ->
                jdbc.query(queries.get("SPS.REPERTORIO.CIFRAS_RECEBIDAS"), new MapSqlParameterSource(USUARIO, idUsuario), CIFRA));
    }

    @Override
    public Optional<RepertorioCifraDto> buscarCifraRecebida(Long idUsuario, Long id) {
        return executor.executar("Erro ao abrir a cifra", () ->
                jdbc.query(queries.get("SPS.REPERTORIO.CIFRA_RECEBIDA"), recebida(idUsuario, id), CIFRA).stream().findFirst());
    }

    @Override
    public void marcarCifraLida(Long idUsuario, Long id) {
        executor.executar("Erro ao marcar a cifra como lida", () -> jdbc.update(queries.get("SPU.REPERTORIO.CIFRA_LIDA"), recebida(idUsuario, id)));
    }

    @Override
    public int excluirCifraRecebida(Long idUsuario, Long id) {
        return executor.executar("Erro ao excluir a cifra", () -> jdbc.update(queries.get("SPD.REPERTORIO.CIFRA_RECEBIDA"), recebida(idUsuario, id)));
    }
}
