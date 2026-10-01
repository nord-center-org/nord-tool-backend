package br.com.nord_tool_backend.controller.write;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.CargoDto;
import br.com.nord_tool_backend.form.CargoForm;
import br.com.nord_tool_backend.service.CargoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nord-tool/cargos")
public class CargoWriteController implements BaseResponse {
    private final CargoService cargoService;

    @PostMapping
    public ResponseEntity<ApiResponseBody<CargoDto>> criarCargo(@Valid @RequestBody CargoForm cargoForm) {
        return created(cargoService.salvarCargo(cargoForm));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseBody<CargoDto>> alterarCargo(
            @PathVariable Long id, @Valid @RequestBody CargoForm cargoForm) {
        return ok(cargoService.alterarCargo(id, cargoForm));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseBody<Void>> deletarCargo(@PathVariable Long id) {
        cargoService.deletarCargo(id);
        return noContent();
    }
}
