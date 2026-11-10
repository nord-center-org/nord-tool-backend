package br.com.nord_tool_backend.repository;

import br.com.nord_tool_backend.dto.BlocoTrechoDto;

import java.util.List;

/** Bloco de notas v4: leitura do Bloco de notas da pessoa para o chat dos cadernos. Tudo filtra pelo dono. */
public interface BlocoAssistenteRepository {

    /** Cadernos, planilhas e documentos, sem o conteúdo, do mais recente para o mais antigo. */
    List<BlocoTrechoDto> catalogo(Long idUsuario, int limite);

    /** Páginas com caixa de texto, documentos dos cadernos, planilhas e documentos avulsos, com o conteúdo bruto. */
    List<BlocoTrechoDto> trechos(Long idUsuario, int limite);
}
