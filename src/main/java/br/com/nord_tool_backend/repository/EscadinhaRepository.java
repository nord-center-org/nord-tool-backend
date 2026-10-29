package br.com.nord_tool_backend.repository;

import br.com.nord_tool_backend.domain.EscadinhaTorre;

import java.util.List;
import java.util.Optional;

/** Torres e modelo de ciclos da escadinha; tudo sempre filtrado pela obra. */
public interface EscadinhaRepository {
    List<EscadinhaTorre> listarTorres(Long idObra);

    Optional<EscadinhaTorre> buscarTorre(Long idObra, Long idTorre);

    int contarTorres(Long idObra);

    Long inserirTorre(Long idObra, String nome, int ordem, String config, String ciclos, String planta, Long idUsuario);

    /** Altera se a versão bater; mudarPlanta=false mantém a imagem. Devolve as linhas alteradas (0 = conflito). */
    int alterarTorre(Long idObra, Long idTorre, int versao, String nome, int ordem, String config, String ciclos,
                     boolean mudarPlanta, String planta, Long idUsuario);

    int excluirTorre(Long idObra, Long idTorre);

    Optional<String> planta(Long idObra, Long idTorre);

    Optional<String> modelo(Long idObra);

    void gravarModelo(Long idObra, String modelo, Long idUsuario);

    /** Grava (valor JSON) ou apaga (valor null) uma célula da grade. Devolve as linhas alteradas (0 = torre de outra obra). */
    int gravarCelula(Long idObra, Long idTorre, String chave, String valor, Long idUsuario);

    Optional<String> grade(Long idObra, Long idTorre);

    List<br.com.nord_tool_backend.dto.EscadinhaTipologiaDto> tipologias(Long idObra, Long idTorre, Long idTipologia);

    Optional<String> imagemTipologia(Long idObra, Long idTipologia);

    boolean nomeTipologiaEmUso(Long idTorre, String nome, Long idIgnorar);

    Long inserirTipologia(Long idTorre, String nome, String imagem, Long idUsuario);

    int alterarTipologia(Long idObra, Long idTipologia, String nome, boolean mudarImagem, String imagem, Long idUsuario);

    int excluirTipologia(Long idObra, Long idTipologia);
}
