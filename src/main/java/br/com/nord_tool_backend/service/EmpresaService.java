package br.com.nord_tool_backend.service;

import br.com.nord_tool_backend.dto.EmpresaDto;
import br.com.nord_tool_backend.form.EmpresaForm;
import java.util.List;

public interface EmpresaService {
    List<EmpresaDto> listarEmpresas();
    EmpresaDto salvarEmpresa(EmpresaForm empresaForm);
    EmpresaDto alterarEmpresa(Long id, EmpresaForm empresaForm);
    void deletarEmpresa(Long id);
}
