package br.com.nord_tool_backend.repository;

import br.com.nord_tool_backend.domain.CasamentoAnexo;
import br.com.nord_tool_backend.domain.CasamentoConvidado;
import br.com.nord_tool_backend.domain.CasamentoFornecedor;
import br.com.nord_tool_backend.domain.CasamentoMarco;
import br.com.nord_tool_backend.dto.CasamentoTotaisDto;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Acesso a dados do módulo Casamento (configuração, fornecedores, anexos, convidados, marcos e totais). */
public interface CasamentoRepository {

    // configuração
    Map<String, String> listarConfiguracao();
    void salvarConfiguracao(String chave, String valor);

    // fornecedores
    List<CasamentoFornecedor> listarFornecedores();
    Optional<CasamentoFornecedor> buscarFornecedor(Long id);
    Long inserirFornecedor(CasamentoFornecedor fornecedor);
    void alterarFornecedor(CasamentoFornecedor fornecedor);
    void deletarFornecedor(Long id);

    // anexos
    List<CasamentoAnexo> listarAnexos(Long idFornecedor);
    Optional<CasamentoAnexo> buscarAnexo(Long idAnexo);
    Long inserirAnexo(Long idFornecedor, Long idArquivo, String descricao);
    void deletarAnexo(Long idAnexo);

    // convidados
    List<CasamentoConvidado> listarConvidados();
    Optional<CasamentoConvidado> buscarConvidado(Long id);
    Long inserirConvidado(CasamentoConvidado convidado);
    void alterarConvidado(CasamentoConvidado convidado);
    void deletarConvidado(Long id);

    // marcos
    List<CasamentoMarco> listarMarcos();
    List<CasamentoMarco> listarProximosMarcos(int limite);
    Optional<CasamentoMarco> buscarMarco(Long id);
    int contarMarcos();
    Long inserirMarco(CasamentoMarco marco);
    void alterarMarco(CasamentoMarco marco);
    void concluirMarco(Long id, boolean concluido);
    void deletarMarco(Long id);

    // dashboard
    CasamentoTotaisDto buscarTotais();
}
