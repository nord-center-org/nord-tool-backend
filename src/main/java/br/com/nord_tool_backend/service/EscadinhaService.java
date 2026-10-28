package br.com.nord_tool_backend.service;

import br.com.nord_tool_backend.dto.EscadinhaDto;
import br.com.nord_tool_backend.dto.EscadinhaPlantaDto;
import br.com.nord_tool_backend.dto.EscadinhaTorreDto;
import br.com.nord_tool_backend.form.EscadinhaModeloForm;
import br.com.nord_tool_backend.form.EscadinhaTorreForm;

/** Escadinha 3D da obra escolhida: torres (croqui, apartamentos, lados, ciclos) e modelo de ciclos. */
public interface EscadinhaService {
    EscadinhaDto carregar();

    EscadinhaPlantaDto planta(Long idTorre);

    EscadinhaTorreDto criarTorre(EscadinhaTorreForm form);

    EscadinhaTorreDto alterarTorre(Long idTorre, EscadinhaTorreForm form);

    void excluirTorre(Long idTorre);

    EscadinhaDto salvarModelo(EscadinhaModeloForm form);
}
