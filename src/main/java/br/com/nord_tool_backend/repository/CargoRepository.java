package br.com.nord_tool_backend.repository;

import br.com.nord_tool_backend.domain.Cargo;
import java.util.List;

public interface CargoRepository {
    List<Cargo> listarCargos();
    Cargo salvarCargo(Cargo cargo);
    Cargo alterarCargo(Cargo cargo);
    void deletarCargo(Long id);
}
