package br.com.nord_tool_backend.storage;

import br.com.nord_tool_backend.excepetion.ValidacaoException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ArquivoValidadorTest {

    private static byte[] comPrefixo(int tamanho, int... prefixo) {
        byte[] b = new byte[tamanho];
        for (int i = 0; i < prefixo.length; i++) b[i] = (byte) prefixo[i];
        return b;
    }

    private static final int[] JPEG = {0xFF, 0xD8, 0xFF, 0xE0};
    private static final int[] PNG = {0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final int[] PDF = {'%', 'P', 'D', 'F', '-'};

    @Test
    void aceitaPdfValido() {
        assertEquals("application/pdf", ArquivoValidador.validarPdf("termo.pdf", comPrefixo(100, PDF)));
    }

    @Test
    void rejeitaPdfSemAssinaturaOuAcimaDe15Mb() {
        ValidacaoException a = assertThrows(ValidacaoException.class,
                () -> ArquivoValidador.validarPdf("x.pdf", comPrefixo(100, JPEG)));
        assertTrue(a.getMessage().contains("PDF"));
        assertThrows(ValidacaoException.class,
                () -> ArquivoValidador.validarPdf("x.pdf", comPrefixo(ArquivoValidador.MAX_PDF_BYTES + 1, PDF)));
        assertDoesNotThrow(() -> ArquivoValidador.validarPdf("x.pdf", comPrefixo(ArquivoValidador.MAX_PDF_BYTES, PDF)));
    }

    @Test
    void detectaJpegEPngPeloConteudoENaoPelaExtensao() {
        assertEquals("image/jpeg", ArquivoValidador.validarImagem("a.png", comPrefixo(50, JPEG)));
        assertEquals("image/png", ArquivoValidador.validarImagem("a.jpg", comPrefixo(50, PNG)));
    }

    @Test
    void rejeitaImagemInvalidaOuAcimaDe5Mb() {
        assertThrows(ValidacaoException.class, () -> ArquivoValidador.validarImagem("a.gif", comPrefixo(50, 'G', 'I', 'F')));
        assertThrows(ValidacaoException.class, () -> ArquivoValidador.validarImagem("a.jpg", comPrefixo(ArquivoValidador.MAX_IMAGEM_BYTES + 1, JPEG)));
        assertDoesNotThrow(() -> ArquivoValidador.validarImagem("a.jpg", comPrefixo(ArquivoValidador.MAX_IMAGEM_BYTES, JPEG)));
    }

    @Test
    void rejeitaArquivoVazioOuNulo() {
        assertThrows(ValidacaoException.class, () -> ArquivoValidador.validarPdf("x.pdf", new byte[0]));
        assertThrows(ValidacaoException.class, () -> ArquivoValidador.validarImagem("x.jpg", null));
        assertThrows(ValidacaoException.class, () -> ArquivoValidador.validarPdf("x.pdf", new byte[3]));
    }

    @Test
    void validaNome() {
        char[] longo = new char[181];
        Arrays.fill(longo, 'a');
        assertThrows(ValidacaoException.class, () -> ArquivoValidador.validarNome(new String(longo)));
        assertDoesNotThrow(() -> ArquivoValidador.validarNome(new String(longo, 0, 180)));
        assertThrows(ValidacaoException.class, () -> ArquivoValidador.validarNome("a\nb.pdf"));
        assertThrows(ValidacaoException.class, () -> ArquivoValidador.validarNome("a\u0000.pdf"));
        assertThrows(ValidacaoException.class, () -> ArquivoValidador.validarNome("  "));
        assertThrows(ValidacaoException.class, () -> ArquivoValidador.validarNome(null));
        assertDoesNotThrow(() -> ArquivoValidador.validarNome("Relatório de reprova (1º).pdf"));
    }

    @Test
    void contratoAceitaPdfEImagemAte15Mb() {
        assertEquals("application/pdf", ArquivoValidador.validarContrato("c.pdf", comPrefixo(100, PDF)));
        assertEquals("image/jpeg", ArquivoValidador.validarContrato("c.jpg", comPrefixo(100, JPEG)));
        assertEquals("image/png", ArquivoValidador.validarContrato("c.png", comPrefixo(100, PNG)));
        assertDoesNotThrow(() -> ArquivoValidador.validarContrato("c.jpg", comPrefixo(ArquivoValidador.MAX_CONTRATO_BYTES, JPEG)));
    }

    @Test
    void contratoRecusaOutrosTiposVazioEAcimaDe15Mb() {
        assertThrows(ValidacaoException.class, () -> ArquivoValidador.validarContrato("c.txt", comPrefixo(100, 'G', 'I', 'F')));
        assertThrows(ValidacaoException.class, () -> ArquivoValidador.validarContrato("c.pdf", new byte[0]));
        assertThrows(ValidacaoException.class, () -> ArquivoValidador.validarContrato("c.pdf", comPrefixo(ArquivoValidador.MAX_CONTRATO_BYTES + 1, PDF)));
        assertThrows(ValidacaoException.class, () -> ArquivoValidador.validarContrato("a\nb.pdf", comPrefixo(100, PDF)));
    }
}
