package br.com.nord_tool_backend.repository;

import br.com.nord_tool_backend.dto.PlacaDto;
import br.com.nord_tool_backend.dto.PlacaModeloDto;

import java.util.List;
import java.util.Optional;

public interface PlacaRepository {
    List<PlacaModeloDto> modelos();

    Optional<PlacaModeloDto> modelo(Long id);

    Long inserirModelo(String nome, String config, boolean ativo, Long idUsuario);

    int alterarModelo(Long id, String nome, String config, boolean ativo, Long idUsuario);

    /** Troca a imagem (fundo ou logo); null tira. */
    int trocarImagem(Long idModelo, boolean fundo, Long idArquivo, Long idUsuario);

    int placasDoModelo(Long idModelo);

    int excluirModelo(Long id);

    List<PlacaDto> placas(Long idObra);

    Optional<PlacaDto> placa(Long id);

    Long inserirPlaca(Long idObra, Long idModelo, String titulo, String valores, Long idUsuario);

    int alterarPlaca(Long id, Long idModelo, String titulo, String valores, Long idUsuario);

    int excluirPlaca(Long id);
}
