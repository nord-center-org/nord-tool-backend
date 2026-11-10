package br.com.nord_tool_backend.controller.write;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.form.MenuFavoritosForm;
import br.com.nord_tool_backend.service.MenuFavoritoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nord-tool/menu-favoritos")
@Tag(name = "Menu", description = "Favoritos do menu lateral")
public class MenuFavoritoWriteController implements BaseResponse {

    private final MenuFavoritoService service;

    @Operation(summary = "Troca a lista de favoritos (a tela manda a lista inteira)")
    @PutMapping
    public ResponseEntity<ApiResponseBody<List<String>>> salvar(@Valid @RequestBody MenuFavoritosForm form) {
        return ok(service.salvar(form.getCaminhos()));
    }
}
