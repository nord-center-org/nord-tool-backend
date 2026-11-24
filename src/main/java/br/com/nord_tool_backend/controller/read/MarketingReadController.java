package br.com.nord_tool_backend.controller.read;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.MarketingConexaoDto;
import br.com.nord_tool_backend.dto.MarketingMidiaPublicaDto;
import br.com.nord_tool_backend.dto.MarketingPainelDto;
import br.com.nord_tool_backend.dto.MarketingPublicacaoDto;
import br.com.nord_tool_backend.dto.MarketingRedeDto;
import br.com.nord_tool_backend.service.MarketingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nord-tool")
@Tag(name = "Marketing", description = "Contas de redes sociais, publicações e painel de views")
public class MarketingReadController implements BaseResponse {

    private final MarketingService service;

    @Operation(summary = "Redes (configuradas no servidor ou não) e as contas que a pessoa conectou")
    @GetMapping("/marketing/redes")
    public ResponseEntity<ApiResponseBody<List<MarketingRedeDto>>> redes() {
        return ok(service.redes());
    }

    @Operation(summary = "Endereço do login da rede para conectar uma conta")
    @GetMapping("/marketing/conexao")
    public ResponseEntity<ApiResponseBody<MarketingConexaoDto>> conexao(@RequestParam String rede, @RequestParam(required = false) String origem) {
        return ok(service.conexao(rede, origem));
    }

    @Operation(summary = "Publicações feitas pelo NordTool (as mais recentes primeiro)")
    @GetMapping("/marketing/publicacoes")
    public ResponseEntity<ApiResponseBody<List<MarketingPublicacaoDto>>> publicacoes() {
        return ok(service.publicacoes());
    }

    @Operation(summary = "Painel: seguidores, views e posts recentes de cada conta conectada")
    @GetMapping("/marketing/painel")
    public ResponseEntity<ApiResponseBody<List<MarketingPainelDto>>> painel(@RequestParam(defaultValue = "false") boolean atualizar) {
        return ok(service.painel(atualizar));
    }

    @Operation(summary = "Vídeo pelo link público e temporário (o Instagram busca o vídeo por aqui)")
    @GetMapping("/publico/marketing/midia/{codigo}")
    public ResponseEntity<Resource> midia(@PathVariable String codigo) {
        Optional<MarketingMidiaPublicaDto> m = service.midiaPublica(codigo);
        if (!m.isPresent()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("X-Robots-Tag", "noindex, nofollow")
                .contentType(MediaType.parseMediaType(m.get().getTipo()))
                .contentLength(m.get().getTamanho())
                .body(new FileSystemResource(m.get().getArquivo()));
    }
}
