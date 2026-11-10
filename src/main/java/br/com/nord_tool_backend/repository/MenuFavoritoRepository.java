package br.com.nord_tool_backend.repository;

import java.util.Optional;

public interface MenuFavoritoRepository {

    /** JSON com a lista de caminhos, se a pessoa já salvou. */
    Optional<String> buscar(Long idUsuario);

    void salvar(Long idUsuario, String caminhos);
}
