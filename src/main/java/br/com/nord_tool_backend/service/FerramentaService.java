package br.com.nord_tool_backend.service;

import br.com.nord_tool_backend.dto.FerramentaDto;
import br.com.nord_tool_backend.form.FerramentaForm;

import java.util.List;

public interface FerramentaService {
    FerramentaDto salvarFerramenta(FerramentaForm ferramentaForm);
    FerramentaDto alterarFerramenta(Long id, FerramentaForm ferramentaForm);
    void deletarFerramenta(Long id);
    FerramentaDto buscarPorIdFerramenta(Long id);
    List<FerramentaDto> listarFerramentas();
}
