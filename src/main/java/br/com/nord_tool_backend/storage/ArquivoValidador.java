package br.com.nord_tool_backend.storage;

import br.com.nord_tool_backend.controller.response.NordHttpEnum;
import br.com.nord_tool_backend.excepetion.ValidacaoException;

/** Validação de uploads por conteúdo (magic bytes), tamanho e nome. */
public final class ArquivoValidador {

    public static final int MAX_PDF_BYTES = 15 * 1024 * 1024;
    public static final int MAX_IMAGEM_BYTES = 5 * 1024 * 1024;
    public static final int MAX_NOME = 180;

    public static final String PDF = "application/pdf";
    public static final String JPEG = "image/jpeg";
    public static final String PNG = "image/png";

    private ArquivoValidador() {
    }

    /** Valida um PDF e devolve o content-type. */
    public static String validarPdf(String nome, byte[] bytes) {
        validarNome(nome);
        validarTamanho(bytes, MAX_PDF_BYTES, "O PDF deve ter no máximo 15 MB");
        if (!comecaCom(bytes, '%', 'P', 'D', 'F')) {
            throw erro("O arquivo enviado não é um PDF válido");
        }
        return PDF;
    }

    /** Valida uma imagem JPEG ou PNG e devolve o content-type detectado pelo conteúdo. */
    public static String validarImagem(String nome, byte[] bytes) {
        validarNome(nome);
        validarTamanho(bytes, MAX_IMAGEM_BYTES, "A imagem deve ter no máximo 5 MB");
        if (comecaCom(bytes, 0xFF, 0xD8, 0xFF)) return JPEG;
        if (comecaCom(bytes, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)) return PNG;
        throw erro("A imagem deve ser JPEG ou PNG");
    }

    /** Nome com no máximo 180 caracteres e sem caracteres de controle. */
    public static void validarNome(String nome) {
        if (nome == null || nome.trim().isEmpty()) {
            throw erro("Informe o nome do arquivo");
        }
        if (nome.length() > MAX_NOME) {
            throw erro("O nome do arquivo deve ter no máximo " + MAX_NOME + " caracteres");
        }
        for (int i = 0; i < nome.length(); i++) {
            if (Character.isISOControl(nome.charAt(i))) {
                throw erro("O nome do arquivo contém caracteres inválidos");
            }
        }
    }

    private static void validarTamanho(byte[] bytes, int max, String mensagemMax) {
        if (bytes == null || bytes.length == 0) throw erro("Arquivo vazio");
        if (bytes.length > max) throw erro(mensagemMax);
    }

    private static boolean comecaCom(byte[] bytes, int... assinatura) {
        if (bytes.length < assinatura.length) return false;
        for (int i = 0; i < assinatura.length; i++) {
            if ((bytes[i] & 0xFF) != assinatura[i]) return false;
        }
        return true;
    }

    private static ValidacaoException erro(String mensagem) {
        return new ValidacaoException(NordHttpEnum.HTTP_400, mensagem, null);
    }
}
