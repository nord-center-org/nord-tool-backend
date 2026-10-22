package br.com.nord_tool_backend.repository;

import br.com.nord_tool_backend.dto.RelatorioFotoDto;
import br.com.nord_tool_backend.dto.RelatorioFotograficoDto;
import br.com.nord_tool_backend.dto.RelatorioItemDto;
import br.com.nord_tool_backend.dto.RelatorioModeloDto;
import br.com.nord_tool_backend.dto.RelatorioModeloItemDto;

import java.util.List;
import java.util.Optional;

public interface RelatorioFotograficoRepository {

    List<RelatorioModeloDto> listarModelos();

    List<RelatorioModeloItemDto> listarItensDeModelos();

    Long inserirModelo(RelatorioModeloDto modelo);

    int alterarModelo(RelatorioModeloDto modelo, int nrVersao);

    void substituirItensDoModelo(Long idModelo, List<RelatorioModeloItemDto> itens);

    /** Relatórios da fase em escopo (sem itens). */
    List<RelatorioFotograficoDto> listar();

    /** Busca respeitando o escopo de fase. */
    Optional<RelatorioFotograficoDto> buscar(Long id);

    List<RelatorioItemDto> listarItens(Long idRelatorio);

    List<RelatorioFotoDto> listarFotos(Long idRelatorio);

    Optional<Long> relatorioDoItem(Long idItem);

    Optional<RelatorioFotoDto> buscarFoto(Long idFoto);

    Long inserir(RelatorioFotograficoDto r, Long idFase);

    int alterar(RelatorioFotograficoDto r, int nrVersao);

    int alterarStatus(Long id, String cdStatus, int nrVersao);

    /** Sobe a versão do relatório (mudança em fotos), para quem está com a tela aberta notar. */
    void tocar(Long id);

    int deletar(Long id, int nrVersao);

    Long inserirItem(RelatorioItemDto item);

    int alterarItem(RelatorioItemDto item);

    int deletarItem(Long idItem, Long idRelatorio);

    Long inserirFoto(Long idItem, Long idArquivo, String txLegenda);

    int alterarFoto(Long idFoto, String txLegenda, int nrOrdem);

    int deletarFoto(Long idFoto);
}
