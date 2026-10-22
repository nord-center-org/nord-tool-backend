package br.com.nord_tool_backend.repository.impl;

import br.com.nord_tool_backend.dto.RelatorioFotoDto;
import br.com.nord_tool_backend.dto.RelatorioFotograficoDto;
import br.com.nord_tool_backend.dto.RelatorioItemDto;
import br.com.nord_tool_backend.dto.RelatorioModeloDto;
import br.com.nord_tool_backend.dto.RelatorioModeloItemDto;
import br.com.nord_tool_backend.repository.RelatorioFotograficoRepository;
import br.com.nord_tool_backend.repository.jdbc.JdbcExecutor;
import br.com.nord_tool_backend.repository.jdbc.SqlQueries;
import br.com.nord_tool_backend.service.ContextoFaseService;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.Types;
import java.util.List;
import java.util.Optional;

@Repository
public class RelatorioFotograficoRepositoryImpl implements RelatorioFotograficoRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcExecutor executor;
    private final ContextoFaseService contexto;
    private final SqlQueries queries;

    public RelatorioFotograficoRepositoryImpl(NamedParameterJdbcTemplate jdbc, JdbcExecutor executor, SqlQueries queries,
                                              ContextoFaseService contexto) {
        this.jdbc = jdbc;
        this.executor = executor;
        this.queries = queries;
        this.contexto = contexto;
        // Falha cedo se alguma query faltar no catálogo.
        for (String chave : new String[]{"SPS.RELATORIO.MODELO.LISTAR", "SPS.RELATORIO.LISTAR", "SPI.RELATORIO.FOTO.INSERIR"}) {
            queries.get(chave);
        }
    }

    private String q(String chave) {
        return queries.get(chave);
    }

    @Override
    public List<RelatorioModeloDto> listarModelos() {
        return executor.executar("Erro ao listar modelos de relatório", () ->
                jdbc.query(q("SPS.RELATORIO.MODELO.LISTAR"), BeanPropertyRowMapper.newInstance(RelatorioModeloDto.class)));
    }

    @Override
    public List<RelatorioModeloItemDto> listarItensDeModelos() {
        return executor.executar("Erro ao listar itens dos modelos", () ->
                jdbc.query(q("SPS.RELATORIO.MODELO.ITENS"), BeanPropertyRowMapper.newInstance(RelatorioModeloItemDto.class)));
    }

    @Override
    public Long inserirModelo(RelatorioModeloDto m) {
        return executor.executar("Erro ao salvar modelo de relatório", () ->
                jdbc.queryForObject(q("SPI.RELATORIO.MODELO.INSERIR"), valoresModelo(m), Long.class));
    }

    @Override
    public int alterarModelo(RelatorioModeloDto m, int nrVersao) {
        return executor.executar("Erro ao alterar modelo de relatório", () ->
                jdbc.update(q("SPU.RELATORIO.MODELO.ALTERAR"), valoresModelo(m).addValue("id", m.getId()).addValue("nrVersao", nrVersao)));
    }

    @Override
    public void substituirItensDoModelo(Long idModelo, List<RelatorioModeloItemDto> itens) {
        executor.executar("Erro ao salvar itens do modelo", () -> {
            jdbc.update(q("SPD.RELATORIO.MODELO.ITENS.LIMPAR"), new MapSqlParameterSource("idModelo", idModelo));
            for (RelatorioModeloItemDto i : itens) {
                jdbc.update(q("SPI.RELATORIO.MODELO.ITEM.INSERIR"), new MapSqlParameterSource()
                        .addValue("idModelo", idModelo).addValue("nrOrdem", i.getNrOrdem())
                        .addValue("nmItem", i.getNmItem()).addValue("txOrientacao", i.getTxOrientacao()));
            }
            return null;
        });
    }

    @Override
    public List<RelatorioFotograficoDto> listar() {
        return executor.executar("Erro ao listar relatórios fotográficos", () ->
                jdbc.query(q("SPS.RELATORIO.LISTAR"), contexto.escopo().aplicar(new MapSqlParameterSource().addValue("id", null, Types.BIGINT)),
                        BeanPropertyRowMapper.newInstance(RelatorioFotograficoDto.class)));
    }

    @Override
    public Optional<RelatorioFotograficoDto> buscar(Long id) {
        return executor.executar("Erro ao buscar relatório fotográfico", () ->
                jdbc.query(q("SPS.RELATORIO.LISTAR"), contexto.escopo().aplicar(new MapSqlParameterSource().addValue("id", id, Types.BIGINT)),
                        BeanPropertyRowMapper.newInstance(RelatorioFotograficoDto.class)).stream().findFirst());
    }

    @Override
    public List<RelatorioItemDto> listarItens(Long idRelatorio) {
        return executor.executar("Erro ao listar itens do relatório", () ->
                jdbc.query(q("SPS.RELATORIO.ITENS"), new MapSqlParameterSource("idRelatorio", idRelatorio),
                        BeanPropertyRowMapper.newInstance(RelatorioItemDto.class)));
    }

    @Override
    public List<RelatorioFotoDto> listarFotos(Long idRelatorio) {
        return executor.executar("Erro ao listar fotos do relatório", () ->
                jdbc.query(q("SPS.RELATORIO.FOTOS"), new MapSqlParameterSource("idRelatorio", idRelatorio),
                        BeanPropertyRowMapper.newInstance(RelatorioFotoDto.class)));
    }

    @Override
    public Optional<Long> relatorioDoItem(Long idItem) {
        return executor.executar("Erro ao buscar item do relatório", () ->
                jdbc.queryForList(q("SPS.RELATORIO.ITEM.DONO"), new MapSqlParameterSource("id", idItem), Long.class).stream().findFirst());
    }

    @Override
    public Optional<RelatorioFotoDto> buscarFoto(Long idFoto) {
        return executor.executar("Erro ao buscar foto", () ->
                jdbc.query(q("SPS.RELATORIO.FOTO.BUSCAR"), new MapSqlParameterSource("id", idFoto),
                        BeanPropertyRowMapper.newInstance(RelatorioFotoDto.class)).stream().findFirst());
    }

    @Override
    public Long inserir(RelatorioFotograficoDto r, Long idFase) {
        return executor.executar("Erro ao salvar relatório fotográfico", () ->
                jdbc.queryForObject(q("SPI.RELATORIO.INSERIR"), valores(r)
                        .addValue("idModelo", r.getIdModelo(), Types.BIGINT)
                        .addValue("idFase", idFase, Types.BIGINT)
                        .addValue("idAutor", r.getIdAutor()), Long.class));
    }

    @Override
    public int alterar(RelatorioFotograficoDto r, int nrVersao) {
        return executor.executar("Erro ao alterar relatório fotográfico", () ->
                jdbc.update(q("SPU.RELATORIO.ALTERAR"), valores(r).addValue("id", r.getId()).addValue("nrVersao", nrVersao)));
    }

    @Override
    public int alterarStatus(Long id, String cdStatus, int nrVersao) {
        return executor.executar("Erro ao alterar o status do relatório", () ->
                jdbc.update(q("SPU.RELATORIO.STATUS"), new MapSqlParameterSource().addValue("id", id)
                        .addValue("cdStatus", cdStatus).addValue("nrVersao", nrVersao)));
    }

    @Override
    public void tocar(Long id) {
        executor.executar("Erro ao atualizar relatório", () -> {
            jdbc.update(q("SPU.RELATORIO.TOCAR"), new MapSqlParameterSource("id", id));
        });
    }

    @Override
    public int deletar(Long id, int nrVersao) {
        return executor.executar("Erro ao excluir relatório fotográfico", () ->
                jdbc.update(q("SPD.RELATORIO.DELETAR"), new MapSqlParameterSource().addValue("id", id).addValue("nrVersao", nrVersao)));
    }

    @Override
    public Long inserirItem(RelatorioItemDto i) {
        return executor.executar("Erro ao salvar item do relatório", () ->
                jdbc.queryForObject(q("SPI.RELATORIO.ITEM.INSERIR"), valoresItem(i), Long.class));
    }

    @Override
    public int alterarItem(RelatorioItemDto i) {
        return executor.executar("Erro ao alterar item do relatório", () ->
                jdbc.update(q("SPU.RELATORIO.ITEM.ALTERAR"), valoresItem(i).addValue("id", i.getId())));
    }

    @Override
    public int deletarItem(Long idItem, Long idRelatorio) {
        return executor.executar("Erro ao excluir item do relatório", () ->
                jdbc.update(q("SPD.RELATORIO.ITEM.DELETAR"), new MapSqlParameterSource().addValue("id", idItem).addValue("idRelatorio", idRelatorio)));
    }

    @Override
    public Long inserirFoto(Long idItem, Long idArquivo, String txLegenda) {
        return executor.executar("Erro ao salvar foto", () ->
                jdbc.queryForObject(q("SPI.RELATORIO.FOTO.INSERIR"), new MapSqlParameterSource()
                        .addValue("idItem", idItem).addValue("idArquivo", idArquivo).addValue("txLegenda", txLegenda), Long.class));
    }

    @Override
    public int alterarFoto(Long idFoto, String txLegenda, int nrOrdem) {
        return executor.executar("Erro ao alterar foto", () ->
                jdbc.update(q("SPU.RELATORIO.FOTO.ALTERAR"), new MapSqlParameterSource()
                        .addValue("id", idFoto).addValue("txLegenda", txLegenda).addValue("nrOrdem", nrOrdem)));
    }

    @Override
    public int deletarFoto(Long idFoto) {
        return executor.executar("Erro ao excluir foto", () ->
                jdbc.update(q("SPD.RELATORIO.FOTO.DELETAR"), new MapSqlParameterSource("id", idFoto)));
    }

    private static MapSqlParameterSource valoresModelo(RelatorioModeloDto m) {
        return new MapSqlParameterSource()
                .addValue("nmModelo", m.getNmModelo())
                .addValue("txDescricao", m.getTxDescricao())
                .addValue("nrFotosPagina", m.getNrFotosPagina())
                .addValue("inAtivo", m.getInAtivo());
    }

    private static MapSqlParameterSource valores(RelatorioFotograficoDto r) {
        return new MapSqlParameterSource()
                .addValue("nmTitulo", r.getNmTitulo())
                .addValue("nmLocal", r.getNmLocal())
                .addValue("dtRelatorio", Date.valueOf(r.getDtRelatorio()))
                .addValue("nrFotosPagina", r.getNrFotosPagina());
    }

    private static MapSqlParameterSource valoresItem(RelatorioItemDto i) {
        return new MapSqlParameterSource()
                .addValue("idRelatorio", i.getIdRelatorio())
                .addValue("nrOrdem", i.getNrOrdem())
                .addValue("nmItem", i.getNmItem())
                .addValue("txOrientacao", i.getTxOrientacao())
                .addValue("txResposta", i.getTxResposta());
    }
}
