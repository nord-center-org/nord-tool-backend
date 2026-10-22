package br.com.nord_tool_backend.repository;

import br.com.nord_tool_backend.domain.ApartamentoVistoria;
import br.com.nord_tool_backend.domain.Ferramenta;
import br.com.nord_tool_backend.domain.ObraControleChaves;
import br.com.nord_tool_backend.domain.RequisicaoChave;
import br.com.nord_tool_backend.domain.RequisicaoChaveConsulta;

import java.util.List;
import java.time.LocalDateTime;

public interface ControleChavesRepository {
    List<ObraControleChaves> listarObras();
    /** Usuários ativos que podem retirar (base única). */
    List<br.com.nord_tool_backend.dto.PessoaControleChavesDto> listarPessoas();
    boolean usuarioAtivo(Long idUsuario);
    /** O apartamento (ou a ferramenta) está numa fase que a pessoa pode ver? */
    boolean itemNoEscopo(Long idApartamento, Long idFerramenta);
    List<ApartamentoVistoria> listarApartamentos();
    List<Ferramenta> listarFerramentas();
    Long contarChavesEmCampo();
    Long contarChavesNoQuadro();
    Long contarChavesEntregues();
    List<RequisicaoChaveConsulta> listarHistorico();
    RequisicaoChaveConsulta buscarPorId(Long idRequisicao);
    Long criarRetirada(RequisicaoChave requisicaoChave);
    void receberRetirada(Long idRequisicao, Long idUserRecebimento, LocalDateTime dtRecebimento, String nmStatusRequisicao);
}
