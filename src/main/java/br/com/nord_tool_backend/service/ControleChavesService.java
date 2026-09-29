package br.com.nord_tool_backend.service;

import br.com.nord_tool_backend.dto.ApartamentoControleChavesDto;
import br.com.nord_tool_backend.dto.DashboardControleChavesDto;
import br.com.nord_tool_backend.dto.ObraControleChavesDto;
import br.com.nord_tool_backend.dto.RetiradaControleChavesDto;
import br.com.nord_tool_backend.form.NovaRetiradaControleChavesForm;
import br.com.nord_tool_backend.form.RecebimentoControleChavesForm;

import java.util.List;

public interface ControleChavesService {
    List<ObraControleChavesDto> listarObras();
    List<ApartamentoControleChavesDto> listarApartamentos(String nmBusca, int nrQuantidadePorPagina, int nrPagina);
    DashboardControleChavesDto buscarDashboard(int nrLimiteRecentes, String idObra);
    List<RetiradaControleChavesDto> listarHistorico(String nmBusca, String nmStatusRequisicao, String idObra,
                                                     int nrQuantidadePorPagina, int nrPagina);
    RetiradaControleChavesDto criarRetirada(NovaRetiradaControleChavesForm novaRetiradaControleChavesForm);
    RetiradaControleChavesDto receberRetirada(Long idRequisicao, RecebimentoControleChavesForm recebimentoControleChavesForm);
}
