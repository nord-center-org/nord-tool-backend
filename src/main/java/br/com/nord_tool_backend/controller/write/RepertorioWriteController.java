package br.com.nord_tool_backend.controller.write;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.RepertorioMusicaDto;
import br.com.nord_tool_backend.dto.RepertorioProjetoDto;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.form.RepertorioMusicaForm;
import br.com.nord_tool_backend.form.RepertorioProjetoForm;
import br.com.nord_tool_backend.service.RepertorioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nord-tool/repertorio")
@Tag(name = "Repertório", description = "Músicas e criações guardadas na conta")
public class RepertorioWriteController implements BaseResponse {

    private final RepertorioService service;

    @Operation(summary = "Guarda (cria ou atualiza) os dados da música pela chave do aparelho")
    @PutMapping("/musicas/{chave}")
    public ResponseEntity<ApiResponseBody<RepertorioMusicaDto>> salvarMusica(@PathVariable String chave, @RequestBody RepertorioMusicaForm form) {
        return ok(service.salvarMusica(chave, form));
    }

    @Operation(summary = "Guarda o áudio da música (até 15 MB; troca o anterior)")
    @PutMapping(value = "/musicas/{chave}/audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponseBody<RepertorioMusicaDto>> audio(@PathVariable String chave, @RequestParam("arquivo") MultipartFile arquivo) {
        String nome = arquivo.getOriginalFilename() == null ? "musica" : arquivo.getOriginalFilename();
        try {
            return ok(service.enviarAudio(chave, nome, arquivo.getBytes()));
        } catch (IOException ex) {
            throw new EntradaInvalidaException("Não foi possível ler o áudio enviado", ex);
        }
    }

    @Operation(summary = "Exclui a música (e o áudio) da conta")
    @DeleteMapping("/musicas/{chave}")
    public ResponseEntity<ApiResponseBody<Void>> excluirMusica(@PathVariable String chave) {
        service.excluirMusica(chave);
        return noContent();
    }

    @Operation(summary = "Guarda (cria ou atualiza) uma criação do estúdio pela chave do aparelho")
    @PutMapping("/projetos/{chave}")
    public ResponseEntity<ApiResponseBody<RepertorioProjetoDto>> salvarProjeto(@PathVariable String chave, @RequestBody RepertorioProjetoForm form) {
        return ok(service.salvarProjeto(chave, form));
    }

    @Operation(summary = "Exclui a criação da conta")
    @DeleteMapping("/projetos/{chave}")
    public ResponseEntity<ApiResponseBody<Void>> excluirProjeto(@PathVariable String chave) {
        service.excluirProjeto(chave);
        return noContent();
    }
}
