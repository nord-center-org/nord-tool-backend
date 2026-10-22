package br.com.nord_tool_backend.repository;

import br.com.nord_tool_backend.dto.MapeamentoDto;
import br.com.nord_tool_backend.dto.MapeamentoLinhaDto;

import java.util.List;
import java.util.Optional;

public interface MapeamentoRepository {

    /** Mapeamentos das fases em escopo (sem linhas). */
    List<MapeamentoDto> listar();

    Optional<MapeamentoDto> buscar(Long id);

    List<MapeamentoLinhaDto> listarLinhas(Long idMapeamento);

    Long inserir(MapeamentoDto m, Long idFase);

    int alterar(MapeamentoDto m, int nrVersao);

    int alterarStatus(Long id, String cdStatus, int nrVersao);

    int deletar(Long id, int nrVersao);

    void substituirLinhas(Long idMapeamento, List<MapeamentoLinhaDto> linhas);
}
