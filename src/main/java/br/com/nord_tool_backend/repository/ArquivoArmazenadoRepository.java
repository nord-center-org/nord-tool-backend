package br.com.nord_tool_backend.repository;

import br.com.nord_tool_backend.domain.ArquivoArmazenado;

import java.util.Optional;

public interface ArquivoArmazenadoRepository {
    ArquivoArmazenado inserir(ArquivoArmazenado arquivo);

    /** Única leitura que traz os bytes (bin_conteudo). */
    Optional<ArquivoArmazenado> buscarComConteudo(Long id);

    /** Metadados apenas; nunca lê bin_conteudo. */
    Optional<ArquivoArmazenado> buscarMetadados(Long id);

    int deletar(Long id);
}
