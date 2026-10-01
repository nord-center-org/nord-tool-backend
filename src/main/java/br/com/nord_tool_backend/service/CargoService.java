package br.com.nord_tool_backend.service;

import br.com.nord_tool_backend.dto.CargoDto;
import br.com.nord_tool_backend.form.CargoForm;
import java.util.List;

public interface CargoService {
    List<CargoDto> listarCargos();
    CargoDto salvarCargo(CargoForm cargoForm);
    CargoDto alterarCargo(Long id, CargoForm cargoForm);
    void deletarCargo(Long id);
}
