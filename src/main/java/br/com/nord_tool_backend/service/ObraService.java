package br.com.nord_tool_backend.service;

import br.com.nord_tool_backend.dto.FaseDto;
import br.com.nord_tool_backend.dto.ObraDto;
import br.com.nord_tool_backend.form.FaseForm;
import br.com.nord_tool_backend.form.FaseUsuariosForm;
import br.com.nord_tool_backend.form.ObraForm;

import java.util.List;

public interface ObraService {
    List<ObraDto> listar();
    ObraDto criarObra(ObraForm form);
    ObraDto alterarObra(Long id, ObraForm form);
    FaseDto criarFase(Long idObra, FaseForm form);
    FaseDto alterarFase(Long id, FaseForm form);
    FaseDto definirUsuarios(Long idFase, FaseUsuariosForm form);
}
