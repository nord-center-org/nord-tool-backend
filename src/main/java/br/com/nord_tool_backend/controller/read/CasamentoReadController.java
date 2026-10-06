package br.com.nord_tool_backend.controller.read;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.ArquivoResponse;
import br.com.nord_tool_backend.controller.response.CasamentoResponse;
import br.com.nord_tool_backend.dto.CasamentoAnexoDto;
import br.com.nord_tool_backend.dto.CasamentoConfiguracaoDto;
import br.com.nord_tool_backend.dto.CasamentoConvidadoDto;
import br.com.nord_tool_backend.dto.CasamentoDashboardDto;
import br.com.nord_tool_backend.dto.CasamentoFornecedorDto;
import br.com.nord_tool_backend.dto.CasamentoMarcoDto;
import br.com.nord_tool_backend.service.CasamentoConvidadoService;
import br.com.nord_tool_backend.service.CasamentoFornecedorService;
import br.com.nord_tool_backend.service.CasamentoMarcoService;
import br.com.nord_tool_backend.service.CasamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nord-tool/casamento")
@Tag(name = "Casamento", description = "Configuração, dashboard, fornecedores, convidados e marcos")
public class CasamentoReadController implements CasamentoResponse {

    private final CasamentoService casamentoService;
    private final CasamentoFornecedorService fornecedorService;
    private final CasamentoConvidadoService convidadoService;
    private final CasamentoMarcoService marcoService;

    @Operation(summary = "Casal e data do casamento")
    @GetMapping("/configuracao")
    public ResponseEntity<ApiResponseBody<CasamentoConfiguracaoDto>> configuracao() {
        return ok(casamentoService.buscarConfiguracao());
    }

    @Operation(summary = "Totais do dashboard do casamento")
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponseBody<CasamentoDashboardDto>> dashboard() {
        return ok(casamentoService.dashboard());
    }

    // ---------- fornecedores ----------

    @Operation(summary = "Lista os fornecedores")
    @GetMapping("/fornecedores")
    public ResponseEntity<ApiResponseBody<List<CasamentoFornecedorDto>>> fornecedores() {
        return ok(fornecedorService.listar());
    }

    @Operation(summary = "Busca um fornecedor")
    @GetMapping("/fornecedores/{id}")
    public ResponseEntity<ApiResponseBody<CasamentoFornecedorDto>> fornecedor(@PathVariable Long id) {
        return ok(fornecedorService.buscar(id));
    }

    @Operation(summary = "Lista os anexos (contratos/comprovantes) do fornecedor")
    @GetMapping("/fornecedores/{id}/anexos")
    public ResponseEntity<ApiResponseBody<List<CasamentoAnexoDto>>> anexos(@PathVariable Long id) {
        return ok(fornecedorService.listarAnexos(id));
    }

    @Operation(summary = "Arquivo do anexo (sem cache)")
    @GetMapping("/fornecedores/anexos/{idAnexo}/arquivo")
    public ResponseEntity<byte[]> arquivoAnexo(@PathVariable Long idAnexo) {
        return ArquivoResponse.semCache(fornecedorService.baixarAnexo(idAnexo));
    }

    // ---------- convidados ----------

    @Operation(summary = "Lista os convidados")
    @GetMapping("/convidados")
    public ResponseEntity<ApiResponseBody<List<CasamentoConvidadoDto>>> convidados() {
        return ok(convidadoService.listar());
    }

    @Operation(summary = "Busca um convidado")
    @GetMapping("/convidados/{id}")
    public ResponseEntity<ApiResponseBody<CasamentoConvidadoDto>> convidado(@PathVariable Long id) {
        return ok(convidadoService.buscar(id));
    }

    // ---------- marcos ----------

    @Operation(summary = "Lista os marcos, por prazo")
    @GetMapping("/marcos")
    public ResponseEntity<ApiResponseBody<List<CasamentoMarcoDto>>> marcos() {
        return ok(marcoService.listar());
    }

    @Operation(summary = "Busca um marco")
    @GetMapping("/marcos/{id}")
    public ResponseEntity<ApiResponseBody<CasamentoMarcoDto>> marco(@PathVariable Long id) {
        return ok(marcoService.buscar(id));
    }
}
