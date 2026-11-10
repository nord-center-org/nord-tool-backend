package br.com.nord_tool_backend.repository;

import br.com.nord_tool_backend.dto.BlocoCadernoDto;
import br.com.nord_tool_backend.dto.BlocoDocumentoDto;
import br.com.nord_tool_backend.dto.BlocoPaginaDto;

import java.util.List;
import java.util.Optional;

/** Bloco de notas: toda leitura filtra pelo dono (idUsuario). */
public interface BlocoNotasRepository {

    List<BlocoCadernoDto> cadernos(Long idUsuario);

    Optional<BlocoCadernoDto> caderno(Long id, Long idUsuario);

    int contarCadernos(Long idUsuario);

    Long inserirCaderno(Long idUsuario, String titulo, String cor);

    int alterarCaderno(Long id, Long idUsuario, String titulo, String cor, int nrVersao);

    void tocarCaderno(Long id);

    int excluirCaderno(Long id, Long idUsuario);

    /** Fundos de PDF e imagens do caderno (para apagar os arquivos). */
    List<Long> arquivosDoCaderno(Long id);

    List<BlocoPaginaDto> paginas(Long idCaderno, Long idUsuario);

    Optional<BlocoPaginaDto> pagina(Long id, Long idUsuario);

    Optional<String> conteudo(Long id, Long idUsuario);

    /** Insere na posição (empurrando as seguintes). */
    Long inserirPagina(Long idCaderno, int ordem, String fundo, int largura, int altura, Long idArquivo, String conteudo);

    void reordenar(Long idCaderno, List<Long> ids);

    int salvarConteudo(Long id, String conteudo, String fundo, int nrVersao);

    void excluirPagina(Long id);

    Long inserirImagem(Long idCaderno, Long idArquivo);

    Optional<Long> arquivoDaImagem(Long id, Long idUsuario);

    // ---------- v2 ----------

    void girarPagina(Long id, int rotacao, int largura, int altura);

    Optional<BlocoDocumentoDto> documento(Long idCaderno, Long idUsuario);

    int salvarDocumento(Long idCaderno, Long idUsuario, String html, int nrVersao);

    Long inserirPdf(Long idCaderno, Long idArquivo);

    /** Arquivo do PDF, se for de um caderno da pessoa (e, com idCaderno, desse caderno). */
    Optional<Long> arquivoDoPdf(Long idPdf, Long idUsuario, Long idCaderno);

    void ligarPdf(Long idPagina, Long idPdf, Integer nrPagina);
}
