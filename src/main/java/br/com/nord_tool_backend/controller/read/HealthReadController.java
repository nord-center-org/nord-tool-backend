package br.com.nord_tool_backend.controller.read;

import br.com.nord_tool_backend.dto.HealthDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/nord-tool")
@Tag(name = "Health", description = "Endpoint Health")
public class HealthReadController {

    @GetMapping("/health")
    @Operation(summary = "Testa se aplicação está funcionando normalmente")
    public HealthDto getCliente() {
        HealthDto healthDto = new HealthDto();
        healthDto.setMessage("Retornando com sucesso");
        return healthDto;

    }
}
