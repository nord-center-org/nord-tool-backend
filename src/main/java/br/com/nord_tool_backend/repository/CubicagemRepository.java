package br.com.nord_tool_backend.repository;

import br.com.nord_tool_backend.dto.CubicagemDto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** Cubicagem: toda leitura filtra pelo dono (idUsuario). */
public interface CubicagemRepository {

    List<CubicagemDto> listar(Long idUsuario);

    Optional<CubicagemDto> buscar(Long id, Long idUsuario);

    int contar(Long idUsuario);

    Long inserir(Long idUsuario, String titulo, BigDecimal perda, String pecas);

    int alterar(Long id, Long idUsuario, String titulo, BigDecimal perda, String pecas, int nrVersao);

    int excluir(Long id, Long idUsuario);
}
