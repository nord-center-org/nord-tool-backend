package br.com.nord_tool_backend.controller.read;

import br.com.nord_tool_backend.controller.response.ApiResponseBody;
import br.com.nord_tool_backend.controller.response.ArquivoResponse;
import br.com.nord_tool_backend.controller.response.BaseResponse;
import br.com.nord_tool_backend.dto.RepertorioMusicaDto;
import br.com.nord_tool_backend.dto.RepertorioProjetoDto;
import br.com.nord_tool_backend.dto.RepertorioUsoDto;
import br.com.nord_tool_backend.service.RepertorioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nord-tool/repertorio")
@Tag(name = "Repertório", description = "Músicas e criações guardadas na conta")
public class RepertorioReadController implements BaseResponse {

    private final RepertorioService service;

    @Operation(summary = "Minhas músicas (sem os dados)")
    @GetMapping("/musicas")
    public ResponseEntity<ApiResponseBody<List<RepertorioMusicaDto>>> musicas() {
        return ok(service.listarMusicas());
    }

    @Operation(summary = "Música com os dados (análise, cifra, ajustes)")
    @GetMapping("/musicas/{chave}")
    public ResponseEntity<ApiResponseBody<RepertorioMusicaDto>> musica(@PathVariable String chave) {
        return ok(service.abrirMusica(chave));
    }

    @Operation(summary = "Áudio da música")
    @GetMapping("/musicas/{chave}/audio")
    public ResponseEntity<byte[]> audio(@PathVariable String chave, @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {
        return ArquivoResponse.imagem(service.baixarAudio(chave), ifNoneMatch);
    }

    @Operation(summary = "Espaço usado no Repertório")
    @GetMapping("/uso")
    public ResponseEntity<ApiResponseBody<RepertorioUsoDto>> uso() {
        return ok(service.uso());
    }

    @Operation(summary = "Minhas criações do estúdio (sem os dados)")
    @GetMapping("/projetos")
    public ResponseEntity<ApiResponseBody<List<RepertorioProjetoDto>>> projetos() {
        return ok(service.listarProjetos());
    }

    @Operation(summary = "Pessoas com quem dá para compartilhar a cifra")
    @GetMapping("/pessoas")
    public ResponseEntity<ApiResponseBody<List<br.com.nord_tool_backend.dto.PessoaCronogramaDto>>> pessoas() {
        return ok(service.pessoas());
    }

    @Operation(summary = "Cifras que compartilharam comigo (sem o texto)")
    @GetMapping("/cifras-recebidas")
    public ResponseEntity<ApiResponseBody<List<br.com.nord_tool_backend.dto.RepertorioCifraDto>>> cifrasRecebidas() {
        return ok(service.cifrasRecebidas());
    }

    @Operation(summary = "Criação com os dados")
    @GetMapping("/projetos/{chave}")
    public ResponseEntity<ApiResponseBody<RepertorioProjetoDto>> projeto(@PathVariable String chave) {
        return ok(service.abrirProjeto(chave));
    }
}
