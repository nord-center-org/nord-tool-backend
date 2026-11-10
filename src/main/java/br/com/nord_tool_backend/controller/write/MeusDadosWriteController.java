package br.com.nord_tool_backend.controller.write;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.MeusDadosDto;
import br.com.nord_tool_backend.form.MeusDadosForm;
import br.com.nord_tool_backend.service.MeusDadosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nord-tool/conta/dados")
@Tag(name = "Conta", description = "Os dados da própria pessoa")
public class MeusDadosWriteController implements BaseResponse {

    private final MeusDadosService service;

    @Operation(summary = "Salva o meu nome, celular e empresa (o cargo é do administrador)")
    @PutMapping
    public ResponseEntity<ApiResponseBody<MeusDadosDto>> salvar(@Valid @RequestBody MeusDadosForm form) {
        return ok(service.salvar(form));
    }
}
