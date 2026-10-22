package br.com.nord_tool_backend.controller.write;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.RelatorioFotoDto;
import br.com.nord_tool_backend.dto.RelatorioFotograficoDto;
import br.com.nord_tool_backend.dto.RelatorioModeloDto;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.form.RelatorioFotoForm;
import br.com.nord_tool_backend.form.RelatorioFotograficoForm;
import br.com.nord_tool_backend.form.RelatorioModeloForm;
import br.com.nord_tool_backend.form.RelatorioStatusForm;
import br.com.nord_tool_backend.service.RelatorioFotograficoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nord-tool/relatorios-fotograficos")
@Tag(name = "Relatório fotográfico", description = "Modelos, relatórios e fotos")
public class RelatorioFotograficoWriteController implements BaseResponse {

    private final RelatorioFotograficoService service;

    @Operation(summary = "Cria um modelo (ADMIN do módulo)")
    @PostMapping("/modelos")
    public ResponseEntity<ApiResponseBody<RelatorioModeloDto>> criarModelo(@Valid @RequestBody RelatorioModeloForm form) {
        return created(service.criarModelo(form));
    }

    @Operation(summary = "Altera um modelo e substitui os itens; exige nrVersao (ADMIN do módulo)")
    @PutMapping("/modelos/{id}")
    public ResponseEntity<ApiResponseBody<RelatorioModeloDto>> alterarModelo(@PathVariable Long id, @Valid @RequestBody RelatorioModeloForm form) {
        return ok(service.alterarModelo(id, form));
    }

    @Operation(summary = "Cria um relatório; com idModelo e sem itens, copia os itens do modelo")
    @PostMapping
    public ResponseEntity<ApiResponseBody<RelatorioFotograficoDto>> criar(@Valid @RequestBody RelatorioFotograficoForm form) {
        return created(service.criar(form));
    }

    @Operation(summary = "Altera cabeçalho e, se enviados, os itens; exige nrVersao")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseBody<RelatorioFotograficoDto>> alterar(@PathVariable Long id, @Valid @RequestBody RelatorioFotograficoForm form) {
        return ok(service.alterar(id, form));
    }

    @Operation(summary = "Conclui (CONCLUIDO) ou reabre (RASCUNHO); exige nrVersao")
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponseBody<RelatorioFotograficoDto>> status(@PathVariable Long id, @Valid @RequestBody RelatorioStatusForm form) {
        return ok(service.alterarStatus(id, form));
    }

    @Operation(summary = "Exclui o relatório com as fotos (nrVersao divergente → 409)")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseBody<Void>> excluir(@PathVariable Long id,
            @RequestParam(value = "nrVersao", required = false) Integer nrVersao) {
        service.excluir(id, nrVersao);
        return noContent();
    }

    @Operation(summary = "Adiciona uma foto JPEG/PNG (até 5 MB) ao item")
    @PostMapping(value = "/itens/{idItem}/fotos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponseBody<RelatorioFotoDto>> adicionarFoto(@PathVariable Long idItem,
            @RequestParam("arquivo") MultipartFile arquivo,
            @RequestParam(value = "txLegenda", required = false) String txLegenda) {
        String nome = arquivo.getOriginalFilename() == null ? "foto.jpg" : arquivo.getOriginalFilename();
        return created(service.adicionarFoto(idItem, nome, bytes(arquivo), txLegenda));
    }

    @Operation(summary = "Altera legenda e/ou ordem da foto")
    @PutMapping("/fotos/{idFoto}")
    public ResponseEntity<ApiResponseBody<RelatorioFotoDto>> alterarFoto(@PathVariable Long idFoto, @Valid @RequestBody RelatorioFotoForm form) {
        return ok(service.alterarFoto(idFoto, form));
    }

    @Operation(summary = "Exclui a foto e o arquivo")
    @DeleteMapping("/fotos/{idFoto}")
    public ResponseEntity<ApiResponseBody<Void>> excluirFoto(@PathVariable Long idFoto) {
        service.excluirFoto(idFoto);
        return noContent();
    }

    private static byte[] bytes(MultipartFile arquivo) {
        try {
            return arquivo.getBytes();
        } catch (IOException ex) {
            throw new EntradaInvalidaException("Não foi possível ler o arquivo enviado", ex);
        }
    }
}
