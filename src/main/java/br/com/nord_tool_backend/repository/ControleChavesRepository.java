package br.com.nord_tool_backend.repository;

import br.com.nord_tool_backend.domain.RequisicaoChave;
import br.com.nord_tool_backend.dto.ApartamentoControleChavesDto;
import br.com.nord_tool_backend.dto.ObraControleChavesDto;
import br.com.nord_tool_backend.form.NovaRetiradaControleChavesForm;
import br.com.nord_tool_backend.form.RecebimentoControleChavesForm;

import java.util.List;

public interface ControleChavesRepository {
    List<ObraControleChavesDto> listarObras();
    List<ApartamentoControleChavesDto> listarApartamentos(String busca, int limite, int pagina);
    Long contarChavesEmCampo();
    Long contarChavesNoQuadro();
    Long contarChavesEntregues();
    List<RequisicaoChave> listarHistorico(String busca, String status, String idObra, int limite, int pagina);
    RequisicaoChave buscarPorId(Long id);
    RequisicaoChave criarRetirada(NovaRetiradaControleChavesForm form);
    RequisicaoChave receberRetirada(Long id, RecebimentoControleChavesForm form);
}
