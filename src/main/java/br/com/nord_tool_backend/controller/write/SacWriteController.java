package br.com.nord_tool_backend.controller.write;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.SacChamadoDto;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.form.SacRespostaForm;
import br.com.nord_tool_backend.form.SacVistosForm;
import br.com.nord_tool_backend.service.SacService;
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
import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nord-tool/sac")
@Tag(name = "SAC", description = "Chamados das pessoas para os administradores")
public class SacWriteController implements BaseResponse {

    private final SacService service;

    @Operation(summary = "Abre um chamado com título, descrição, a tela e até 6 prints JPEG/PNG (5 MB cada)")
    @PostMapping(value = "/chamados", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponseBody<SacChamadoDto>> abrir(@RequestParam("nmTitulo") String nmTitulo,
            @RequestParam("txDescricao") String txDescricao,
            @RequestParam(value = "txTela", required = false) String txTela,
            @RequestParam(value = "imagens", required = false) List<MultipartFile> imagens) {
        List<SacService.Anexo> anexos = new ArrayList<>();
        if (imagens != null) {
            for (MultipartFile f : imagens) {
                if (f == null || f.isEmpty()) continue;
                anexos.add(new SacService.Anexo(f.getOriginalFilename() == null ? "print.png" : f.getOriginalFilename(), bytes(f)));
            }
        }
        return created(service.abrir(nmTitulo, txDescricao, txTela, anexos));
    }

    @Operation(summary = "Responde e muda o status (ADMIN do SAC)")
    @PutMapping("/chamados/{id}/resposta")
    public ResponseEntity<ApiResponseBody<SacChamadoDto>> responder(@PathVariable Long id, @Valid @RequestBody SacRespostaForm form) {
        return ok(service.responder(id, form));
    }

    @Operation(summary = "Quem abriu viu os chamados: some o aviso de novidade")
    @PutMapping("/chamados/vistos")
    public ResponseEntity<ApiResponseBody<Void>> vistos(@Valid @RequestBody SacVistosForm form) {
        service.marcarVistos(form.getIds());
        return noContent();
    }

    @Operation(summary = "Exclui o chamado: quem abriu, enquanto está aberto; o ADMIN do SAC, sempre")
    @DeleteMapping("/chamados/{id}")
    public ResponseEntity<ApiResponseBody<Void>> excluir(@PathVariable Long id) {
        service.excluir(id);
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
