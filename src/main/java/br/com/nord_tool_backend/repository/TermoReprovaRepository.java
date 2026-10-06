package br.com.nord_tool_backend.repository;

import br.com.nord_tool_backend.domain.TermoReprova;
import br.com.nord_tool_backend.dto.TermoReprovaResumoGeralDto;

import java.util.List;
import java.util.Optional;

public interface TermoReprovaRepository {
    boolean apartamentoExiste(Long idApartamento);
    List<TermoReprova> listarPorApartamento(Long idApartamento);
    Optional<TermoReprova> buscarPorId(Long id);
    int proximoNumero(Long idApartamento);
    List<Long> listarIdsPorApartamento(Long idApartamento);
    TermoReprova inserir(TermoReprova termo);
    void atualizarArquivo(Long id, Long idArquivo, int nrPaginas);
    void atualizarSituacao(Long id, String situacao, String observacao);
    void deletar(Long id);

    /** Contagens para o dashboard (sem o percentual, calculado no service). */
    TermoReprovaResumoGeralDto resumoGeral();
}
