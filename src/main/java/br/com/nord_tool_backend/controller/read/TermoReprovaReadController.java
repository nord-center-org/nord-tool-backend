package br.com.nord_tool_backend.controller.read;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.ArquivoResponse;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.TermoReprovaDto;
import br.com.nord_tool_backend.dto.TermoReprovaResumoDto;
import br.com.nord_tool_backend.dto.TermoReprovaResumoGeralDto;
import br.com.nord_tool_backend.service.TermoReprovaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nord-tool/apartamentoVistoria")
@Tag(name = "Termo de reprova", description = "Termos de reprova (PDF) e fotos por apartamento")
public class TermoReprovaReadController implements BaseResponse {

    private final TermoReprovaService termoReprovaService;

    @Operation(summary = "Lista os termos de reprova de um apartamento")
    @GetMapping("/{idApartamento}/termos-reprova")
    public ResponseEntity<ApiResponseBody<List<TermoReprovaResumoDto>>> listar(@PathVariable Long idApartamento) {
        return ok(termoReprovaService.listarPorApartamento(idApartamento));
    }

    @Operation(summary = "Totais do controle de finalização do DAT (dashboard)")
    @GetMapping("/termos-reprova/resumo")
    public ResponseEntity<ApiResponseBody<TermoReprovaResumoGeralDto>> resumo() {
        return ok(termoReprovaService.resumoGeral());
    }

    @Operation(summary = "Metadados do termo e suas fotos (sem bytes)")
    @GetMapping("/termos-reprova/{idTermo}")
    public ResponseEntity<ApiResponseBody<TermoReprovaDto>> buscar(@PathVariable Long idTermo) {
        return ok(termoReprovaService.buscar(idTermo));
    }

    @Operation(summary = "PDF do termo")
    @GetMapping("/termos-reprova/{idTermo}/arquivo")
    public ResponseEntity<byte[]> pdf(@PathVariable Long idTermo,
                                      @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        return ArquivoResponse.pdf(termoReprovaService.abrirPdf(idTermo), ifNoneMatch);
    }

    @Operation(summary = "Imagem da foto (use ?v=nrVersao para cache)")
    @GetMapping("/termos-reprova/fotos/{idFoto}/imagem")
    public ResponseEntity<byte[]> imagem(@PathVariable Long idFoto,
                                         @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        return ArquivoResponse.imagem(termoReprovaService.abrirImagem(idFoto), ifNoneMatch);
    }

    @Operation(summary = "Miniatura da foto (use ?v=nrVersao para cache)")
    @GetMapping("/termos-reprova/fotos/{idFoto}/miniatura")
    public ResponseEntity<byte[]> miniatura(@PathVariable Long idFoto,
                                            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        return ArquivoResponse.imagem(termoReprovaService.abrirMiniatura(idFoto), ifNoneMatch);
    }
}
