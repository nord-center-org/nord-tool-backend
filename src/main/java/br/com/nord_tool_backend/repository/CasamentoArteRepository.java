package br.com.nord_tool_backend.repository;

import br.com.nord_tool_backend.dto.CasamentoArteDto;

import java.util.List;
import java.util.Optional;

public interface CasamentoArteRepository {

    List<CasamentoArteDto> listar();

    Optional<CasamentoArteDto> buscar(Long id);

    long contar();

    Long inserir(Long idArquivo, String titulo, String categoria, Long idUsuario);

    void atualizar(Long id, String titulo, String categoria);

    void deletar(Long id);
}
