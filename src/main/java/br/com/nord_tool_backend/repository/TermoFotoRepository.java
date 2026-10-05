package br.com.nord_tool_backend.repository;

import br.com.nord_tool_backend.domain.TermoFoto;

import java.util.List;
import java.util.Optional;

public interface TermoFotoRepository {
    List<TermoFoto> listarPorTermo(Long idTermo);
    Optional<TermoFoto> buscarPorId(Long id);
    int proximaOrdem(Long idTermo, int nrPagina);
    int contarPorTermo(Long idTermo);
    TermoFoto inserir(TermoFoto foto);
    void atualizar(TermoFoto foto);
    void atualizarOrdem(Long idTermo, Long idFoto, int nrOrdem);
    void deletar(Long id);
}
