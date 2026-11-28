package br.com.nord_tool_backend.controller.write;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.PlacaDto;
import br.com.nord_tool_backend.dto.PlacaModeloDto;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.form.PlacaForm;
import br.com.nord_tool_backend.form.PlacaModeloForm;
import br.com.nord_tool_backend.service.PlacaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nord-tool/placas")
@Tag(name = "Placas", description = "Modelos de placa e placas da obra")
public class PlacaWriteController implements BaseResponse {

    private final PlacaService service;

    @Operation(summary = "Cria um modelo de placa (ADMIN de Placas)")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/modelos")
    public ResponseEntity<ApiResponseBody<PlacaModeloDto>> criarModelo(@Valid @RequestBody PlacaModeloForm form) {
        return created(service.criarModelo(form));
    }

    @Operation(summary = "Altera o modelo (ADMIN de Placas)")
    @PutMapping("/modelos/{id}")
    public ResponseEntity<ApiResponseBody<PlacaModeloDto>> alterarModelo(@PathVariable Long id, @Valid @RequestBody PlacaModeloForm form) {
        return ok(service.alterarModelo(id, form));
    }

    @Operation(summary = "Exclui o modelo; com placas, só desativa (ADMIN de Placas)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/modelos/{id}")
    public ResponseEntity<ApiResponseBody<Void>> excluirModelo(@PathVariable Long id) {
        service.excluirModelo(id);
        return noContent();
    }

    @Operation(summary = "Envia a imagem do modelo: fundo ou logo, JPEG/PNG até 5 MB (ADMIN de Placas)")
    @PostMapping(value = "/modelos/{id}/imagens/{tipo}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponseBody<PlacaModeloDto>> imagem(@PathVariable Long id, @PathVariable String tipo,
            @RequestParam("arquivo") MultipartFile arquivo) {
        String nome = arquivo.getOriginalFilename() == null ? "imagem.png" : arquivo.getOriginalFilename();
        try {
            return ok(service.salvarImagem(id, tipo, nome, arquivo.getBytes()));
        } catch (IOException ex) {
            throw new EntradaInvalidaException("Não foi possível ler o arquivo enviado", ex);
        }
    }

    @Operation(summary = "Tira a imagem do modelo (ADMIN de Placas)")
    @DeleteMapping("/modelos/{id}/imagens/{tipo}")
    public ResponseEntity<ApiResponseBody<PlacaModeloDto>> removerImagem(@PathVariable Long id, @PathVariable String tipo) {
        return ok(service.removerImagem(id, tipo));
    }

    @Operation(summary = "Cria uma placa na obra escolhida")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ResponseEntity<ApiResponseBody<PlacaDto>> criar(@Valid @RequestBody PlacaForm form) {
        return created(service.criar(form));
    }

    @Operation(summary = "Altera a placa")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseBody<PlacaDto>> alterar(@PathVariable Long id, @Valid @RequestBody PlacaForm form) {
        return ok(service.alterar(id, form));
    }

    @Operation(summary = "Exclui a placa")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseBody<Void>> excluir(@PathVariable Long id) {
        service.excluir(id);
        return noContent();
    }

    @Operation(summary = "Envia uma imagem para pôr na folha (JPEG/PNG até 5 MB); devolve o id")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(value = "/imagens", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponseBody<Long>> imagemSolta(@RequestParam("arquivo") MultipartFile arquivo) {
        String nome = arquivo.getOriginalFilename() == null ? "imagem.png" : arquivo.getOriginalFilename();
        try {
            return created(service.enviarImagemSolta(nome, arquivo.getBytes()));
        } catch (IOException ex) {
            throw new EntradaInvalidaException("Não foi possível ler o arquivo enviado", ex);
        }
    }
}
