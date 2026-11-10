package br.com.nord_tool_backend.controller.read;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.service.MenuFavoritoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nord-tool/menu-favoritos")
@Tag(name = "Menu", description = "Favoritos do menu lateral")
public class MenuFavoritoReadController implements BaseResponse {

    private final MenuFavoritoService service;

    @Operation(summary = "Minhas telas favoritas (caminhos, na ordem)")
    @GetMapping
    public ResponseEntity<ApiResponseBody<List<String>>> listar() {
        return ok(service.listar());
    }
}
