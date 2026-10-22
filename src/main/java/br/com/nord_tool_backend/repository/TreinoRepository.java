package br.com.nord_tool_backend.repository;

import br.com.nord_tool_backend.dto.TreinoDto;
import br.com.nord_tool_backend.dto.TreinoExercicioDto;
import br.com.nord_tool_backend.dto.TreinoSessaoDto;
import br.com.nord_tool_backend.dto.TreinoSessaoExercicioDto;

import java.util.List;
import java.util.Optional;

/** Treinos: todo método recebe o dono e só enxerga os dados dele. */
public interface TreinoRepository {

    List<TreinoDto> listar(Long idDono);

    Optional<TreinoDto> buscar(Long idDono, Long id);

    List<TreinoExercicioDto> listarExercicios(Long idDono);

    Long inserir(Long idDono, TreinoDto t);

    int alterar(Long idDono, TreinoDto t, int nrVersao);

    int deletar(Long idDono, Long id, int nrVersao);

    void inserirExercicio(TreinoExercicioDto e);

    int alterarExercicio(TreinoExercicioDto e);

    int deletarExercicio(Long idTreino, Long idExercicio);

    int atualizarCarga(Long idDono, Long idExercicio, java.math.BigDecimal vlCarga);

    List<TreinoSessaoDto> listarSessoes(Long idDono, int limite);

    List<TreinoSessaoExercicioDto> listarExerciciosDasSessoes(Long idDono, List<Long> idsSessoes);

    Long inserirSessao(Long idDono, TreinoSessaoDto s);

    void inserirExercicioDaSessao(TreinoSessaoExercicioDto e);

    int deletarSessao(Long idDono, Long id);
}
