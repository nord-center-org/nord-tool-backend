package br.com.nord_tool_backend.repository;

import br.com.nord_tool_backend.domain.Ferramenta;
import br.com.nord_tool_backend.dto.FerramentaDto;

import java.util.List;

public interface FerramentaRepository {
    FerramentaDto salvarFerramenta(Ferramenta ferramenta);
    FerramentaDto alterarFerramenta(Ferramenta ferramenta);
    void deletarFerramenta(Long id);
    Ferramenta buscarPorIdFerramenta(Long id);
    List<Ferramenta> listarFerramentas();
}
