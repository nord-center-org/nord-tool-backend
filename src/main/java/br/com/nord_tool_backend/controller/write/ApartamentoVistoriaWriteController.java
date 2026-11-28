package br.com.nord_tool_backend.controller.write;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.ApartamentoVistoriaDto;
import br.com.nord_tool_backend.form.ApartamentoVistoriaForm;
import br.com.nord_tool_backend.service.ApartamentoVistoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nord-tool/apartamentoVistoria")
@Tag(name = "Apartamento Vistoria", description = "Endpoints para criar, manipular e deletar os apartamentos retornados")
public class ApartamentoVistoriaWriteController implements BaseResponse {

    private final ApartamentoVistoriaService apartamentoVistoriaService;

    @Operation(summary = "Cria os apartamentos em vistoria")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ResponseEntity<ApiResponseBody<ApartamentoVistoriaDto>> criarApartamentoVistoria(@Valid @RequestBody ApartamentoVistoriaForm apartamentoVistoriaForm) {
        ApartamentoVistoriaDto apartamentoVistoriaDto = apartamentoVistoriaService.salvarApartamentoVistoria(apartamentoVistoriaForm);
        return created(apartamentoVistoriaDto);
    }

    @Operation(summary = "Editar os apartamentos em vistoria")
    @PutMapping
    public ResponseEntity<ApiResponseBody<ApartamentoVistoriaDto>> alterarApartamentoVistoria(@Valid @RequestBody ApartamentoVistoriaForm apartamentoVistoriaForm) {
        ApartamentoVistoriaDto apartamentoVistoriaDto = apartamentoVistoriaService.alterarApartamentoVistoria(apartamentoVistoriaForm);
        return ok(apartamentoVistoriaDto);
    }

    @Operation(summary = "Excluir os apartamentos em vistoria")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseBody<Void>> deletarApartamentoVistoria(@PathVariable("id") Long id) {
        this.apartamentoVistoriaService.deletarApartamentoVistoria(id);
        return noContent();
    }

    @Operation(summary = "Importar planilha de apartamentos para vistoria")
    @PostMapping("/importar")
    public ResponseEntity<ApiResponseBody<String>> importar(@RequestParam("planilha") MultipartFile planilha) throws Exception {
        apartamentoVistoriaService.importarPlanilha(planilha);
        return ok("Planilha importada com sucesso");
    }

}
