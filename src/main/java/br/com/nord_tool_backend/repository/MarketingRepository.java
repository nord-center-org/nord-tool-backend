package br.com.nord_tool_backend.repository;

import br.com.nord_tool_backend.dto.MarketingContaRegistroDto;
import br.com.nord_tool_backend.dto.MarketingPublicacaoDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MarketingRepository {

    List<MarketingContaRegistroDto> contas(Long idUsuario);

    Optional<MarketingContaRegistroDto> conta(Long idUsuario, Long idConta);

    /** Grava (ou atualiza, se a mesma conta já estava conectada) com os tokens já cifrados; devolve o id. */
    Long salvarConta(Long idUsuario, String rede, String idExterno, String nome, String foto, String acesso, String renovacao,
                     LocalDateTime expira);

    void atualizarTokens(Long idConta, String acesso, String renovacao, LocalDateTime expira);

    boolean excluirConta(Long idUsuario, Long idConta);

    Long criarPublicacao(Long idUsuario, Long idConta, String titulo, String legenda, boolean publico);

    void concluirPublicacao(Long id, String status, String idExterno, String link, String aviso);

    void marcarPresas(Long idUsuario);

    List<MarketingPublicacaoDto> publicacoes(Long idUsuario, int limite);
}
