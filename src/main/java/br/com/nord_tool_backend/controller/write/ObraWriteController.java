package br.com.nord_tool_backend.controller.write;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.FaseDto;
import br.com.nord_tool_backend.dto.ObraDto;
import br.com.nord_tool_backend.form.FaseForm;
import br.com.nord_tool_backend.form.FaseUsuariosForm;
import br.com.nord_tool_backend.form.ObraForm;
import br.com.nord_tool_backend.service.ObraService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nord-tool/administracao")
@Tag(name = "Obras e fases", description = "Cadastro de obras, fases e participantes (só administradores)")
public class ObraWriteController implements BaseResponse {

    private final ObraService service;

    @Operation(summary = "Cria uma obra (nome e código de acesso únicos)")
    @PostMapping("/obras")
    public ResponseEntity<ApiResponseBody<ObraDto>> criarObra(@Valid @RequestBody ObraForm form) {
        return created(service.criarObra(form));
    }

    @Operation(summary = "Edita a obra; exige nrVersao (divergente → 409)")
    @PutMapping("/obras/{id}")
    public ResponseEntity<ApiResponseBody<ObraDto>> alterarObra(@PathVariable Long id, @Valid @RequestBody ObraForm form) {
        return ok(service.alterarObra(id, form));
    }

    @Operation(summary = "Cria uma fase na obra")
    @PostMapping("/obras/{id}/fases")
    public ResponseEntity<ApiResponseBody<FaseDto>> criarFase(@PathVariable Long id, @Valid @RequestBody FaseForm form) {
        return created(service.criarFase(id, form));
    }

    @Operation(summary = "Edita a fase; exige nrVersao (divergente → 409)")
    @PutMapping("/fases/{id}")
    public ResponseEntity<ApiResponseBody<FaseDto>> alterarFase(@PathVariable Long id, @Valid @RequestBody FaseForm form) {
        return ok(service.alterarFase(id, form));
    }

    @Operation(summary = "Liga os apartamentos antigos às fases pela sigla do código (N1-01-0101 → fase N1)")
    @PostMapping("/obras/vincular-apartamentos")
    public ResponseEntity<ApiResponseBody<br.com.nord_tool_backend.dto.VinculoFaseDto>> vincular() {
        return ok(service.vincularApartamentos());
    }

    @Operation(summary = "Define quem participa da fase (lista completa)")
    @PutMapping("/fases/{id}/usuarios")
    public ResponseEntity<ApiResponseBody<FaseDto>> definirUsuarios(@PathVariable Long id, @Valid @RequestBody FaseUsuariosForm form) {
        return ok(service.definirUsuarios(id, form));
    }
}
