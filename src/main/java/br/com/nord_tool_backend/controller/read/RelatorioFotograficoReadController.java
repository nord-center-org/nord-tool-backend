package br.com.nord_tool_backend.controller.read;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.ArquivoResponse;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.RelatorioFotograficoDto;
import br.com.nord_tool_backend.dto.RelatorioModeloDto;
import br.com.nord_tool_backend.service.RelatorioFotograficoService;
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
@RequestMapping("/api/v1/nord-tool/relatorios-fotograficos")
@Tag(name = "Relatório fotográfico", description = "Modelos, relatórios e fotos")
public class RelatorioFotograficoReadController implements BaseResponse {

    private final RelatorioFotograficoService service;

    @Operation(summary = "Lista os modelos com seus itens")
    @GetMapping("/modelos")
    public ResponseEntity<ApiResponseBody<List<RelatorioModeloDto>>> modelos() {
        return ok(service.listarModelos());
    }

    @Operation(summary = "Lista os relatórios da pessoa e os compartilhados com ela; o administrador do módulo vê todos (sem itens)")
    @GetMapping
    public ResponseEntity<ApiResponseBody<List<RelatorioFotograficoDto>>> listar() {
        return ok(service.listar());
    }

    @Operation(summary = "Pessoas com quem dá para compartilhar um relatório")
    @GetMapping("/pessoas")
    public ResponseEntity<ApiResponseBody<List<br.com.nord_tool_backend.dto.PessoaCronogramaDto>>> pessoas() {
        return ok(service.pessoas());
    }

    @Operation(summary = "Equipes que a pessoa enxerga, para compartilhar com todos os membros")
    @GetMapping("/equipes")
    public ResponseEntity<ApiResponseBody<List<br.com.nord_tool_backend.dto.CronogramaEquipeDto>>> equipes() {
        return ok(service.equipes());
    }

    @Operation(summary = "Relatório com itens e fotos")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseBody<RelatorioFotograficoDto>> detalhar(@PathVariable Long id) {
        return ok(service.detalhar(id));
    }

    @Operation(summary = "Imagem da foto")
    @GetMapping("/fotos/{idFoto}/imagem")
    public ResponseEntity<byte[]> imagem(@PathVariable Long idFoto,
                                         @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        return ArquivoResponse.imagem(service.baixarFoto(idFoto), ifNoneMatch);
    }

    @Operation(summary = "Imagem do modelo: fundo (marca d'água) ou icone (canto superior direito)")
    @GetMapping("/modelos/{id}/imagens/{tipo}")
    public ResponseEntity<byte[]> imagemModelo(@PathVariable Long id, @PathVariable String tipo,
                                               @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        return ArquivoResponse.imagem(service.baixarImagemModelo(id, tipo), ifNoneMatch);
    }
}
