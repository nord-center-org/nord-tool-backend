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
    List<ApartamentoControleChavesDto> listarApartamentos(String busca, int limite, int pagina);
    DashboardControleChavesDto buscarDashboard(int limiteRecentes, String idObra);
    List<RetiradaControleChavesDto> listarHistorico(String busca, String status, String idObra, int limite, int pagina);
    RetiradaControleChavesDto criarRetirada(NovaRetiradaControleChavesForm form);
    RetiradaControleChavesDto receberRetirada(Long id, RecebimentoControleChavesForm form);
}
