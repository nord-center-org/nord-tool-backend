package br.com.nord_tool_backend.repository;

import br.com.nord_tool_backend.domain.Medicao;
import br.com.nord_tool_backend.domain.MedicaoStatus;

import java.util.List;
import java.util.Optional;

public interface MedicaoRepository {
    List<Medicao> listar();
    Optional<Medicao> buscar(Long id);
    Long inserir(Medicao medicao);
    /** Só altera se a versão ainda for a que o cliente viu; devolve o nº de linhas (0 = conflito). */
    int alterar(Medicao medicao, int nrVersao);
    int alterarStatus(Long id, String cdStatus, int nrVersao);
    int deletar(Long id, int nrVersao);
    List<MedicaoStatus> listarStatus();
}
