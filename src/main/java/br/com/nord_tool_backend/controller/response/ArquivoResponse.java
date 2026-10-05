package br.com.nord_tool_backend.controller.response;

import br.com.nord_tool_backend.storage.ArquivoConteudo;
import br.com.nord_tool_backend.storage.ArquivoDownload;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;

/** Respostas binárias (PDF/imagens) com ETag por versão e cache privado. */
public final class ArquivoResponse {

    private static final String CACHE_PDF = "private, max-age=300, must-revalidate";
    private static final String CACHE_IMAGEM = "private, max-age=31536000, immutable";

    private ArquivoResponse() {
    }

    public static ResponseEntity<byte[]> pdf(ArquivoDownload download, String ifNoneMatch) {
        return montar(download, ifNoneMatch, CACHE_PDF);
    }

    /** Imagens são imutáveis por versão: o cliente usa ?v=nrVersao. */
    public static ResponseEntity<byte[]> imagem(ArquivoDownload download, String ifNoneMatch) {
        return montar(download, ifNoneMatch, CACHE_IMAGEM);
    }

    static String etag(long versao) {
        return "\"" + versao + "\"";
    }

    private static ResponseEntity<byte[]> montar(ArquivoDownload download, String ifNoneMatch, String cache) {
        String etag = etag(download.getVersao());
        if (etag.equals(ifNoneMatch)) {
            return ResponseEntity.status(304).eTag(etag).header(HttpHeaders.CACHE_CONTROL, cache).build();
        }
        ArquivoConteudo conteudo = download.getConteudo();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(conteudo.getContentType()))
                .contentLength(conteudo.getBytes().length)
                .eTag(etag)
                .header(HttpHeaders.CACHE_CONTROL, cache)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(conteudo.getNome(), StandardCharsets.UTF_8).build().toString())
                .body(conteudo.getBytes());
    }
}
