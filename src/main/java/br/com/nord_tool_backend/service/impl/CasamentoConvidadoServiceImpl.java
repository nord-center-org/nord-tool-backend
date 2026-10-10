package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.controller.response.NordHttpEnum;
import br.com.nord_tool_backend.domain.CasamentoConvidado;
import br.com.nord_tool_backend.domain.enums.StatusConvidadoEnum;
import br.com.nord_tool_backend.dto.CasamentoConvidadoDto;
import br.com.nord_tool_backend.dto.ImportacaoConvidadosDto;
import br.com.nord_tool_backend.excepetion.ValidacaoException;
import br.com.nord_tool_backend.form.CasamentoConvidadoForm;
import br.com.nord_tool_backend.handler.ConvidadosXlsxHandler;
import br.com.nord_tool_backend.repository.CasamentoRepository;
import br.com.nord_tool_backend.service.CasamentoConvidadoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CasamentoConvidadoServiceImpl implements CasamentoConvidadoService {

    static final int MAX_BYTES_PLANILHA = 5 * 1024 * 1024;

    private final CasamentoRepository repository;
    private final ConvidadosXlsxHandler xlsxHandler;

    @Override
    @Transactional(readOnly = true)
    public List<CasamentoConvidadoDto> listar() {
        return repository.listarConvidados().stream().map(CasamentoConvidadoDto::de).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CasamentoConvidadoDto buscar(Long id) {
        return CasamentoConvidadoDto.de(convidado(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CasamentoConvidadoDto criar(CasamentoConvidadoForm form) {
        validarPrincipal(null, form.getIdConvidadoPrincipal());
        Long id = repository.inserirConvidado(converter(form));
        return CasamentoConvidadoDto.de(convidado(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CasamentoConvidadoDto alterar(Long id, CasamentoConvidadoForm form) {
        convidado(id);
        validarPrincipal(id, form.getIdConvidadoPrincipal());
        CasamentoConvidado novo = converter(form);
        novo.setId(id);
        repository.alterarConvidado(novo);
        return CasamentoConvidadoDto.de(convidado(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletar(Long id) {
        convidado(id);
        repository.deletarConvidado(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportacaoConvidadosDto importar(String nomeArquivo, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new ValidacaoException(NordHttpEnum.HTTP_400, "Planilha vazia", null);
        }
        if (bytes.length > MAX_BYTES_PLANILHA) {
            throw new ValidacaoException(NordHttpEnum.HTTP_400, "A planilha deve ter no máximo 5 MB", null);
        }
        if (nomeArquivo == null || !nomeArquivo.toLowerCase().endsWith(".xlsx")) {
            throw new ValidacaoException(NordHttpEnum.HTTP_400, "Envie um arquivo .xlsx", null);
        }
        ImportacaoConvidadosDto relatorio = new ImportacaoConvidadosDto();
        for (ConvidadosXlsxHandler.LinhaLida linha : xlsxHandler.ler(bytes)) {
            if (!linha.valida()) {
                relatorio.getRejeitados().add(new ImportacaoConvidadosDto.LinhaRejeitada(linha.getLinha(), linha.getErro()));
                continue;
            }
            repository.inserirConvidado(converter(linha.getForm()));
            relatorio.setImportados(relatorio.getImportados() + 1);
        }
        return relatorio;
    }

    private CasamentoConvidado convidado(Long id) {
        return repository.buscarConvidado(id)
                .orElseThrow(() -> new ValidacaoException(NordHttpEnum.HTTP_404, "Convidado não encontrado", null));
    }

    /** O principal deve existir, não pode ser o próprio convidado nem alguém ligado a ele (sem ligação circular). */
    private void validarPrincipal(Long idConvidado, Long idPrincipal) {
        if (idPrincipal == null) return;
        if (idPrincipal.equals(idConvidado)) {
            throw new ValidacaoException(NordHttpEnum.HTTP_400, "Um convidado não pode acompanhar a si mesmo", null);
        }
        Long atual = idPrincipal;
        for (int passos = 0; atual != null && passos < 1000; passos++) {
            CasamentoConvidado c = repository.buscarConvidado(atual).orElseThrow(() -> new ValidacaoException(
                    NordHttpEnum.HTTP_400, "Convidado principal não encontrado", null));
            if (idConvidado != null && idConvidado.equals(c.getIdConvidadoPrincipal())) {
                throw new ValidacaoException(NordHttpEnum.HTTP_400,
                        "Ligação circular: esse convidado já está ligado a " + c.getNmConvidado(), null);
            }
            atual = c.getIdConvidadoPrincipal();
        }
    }

    CasamentoConvidado converter(CasamentoConvidadoForm form) {
        StatusConvidadoEnum status = form.getNmStatus() == null || form.getNmStatus().trim().isEmpty()
                ? StatusConvidadoEnum.NAO_CONVIDADO
                : StatusConvidadoEnum.de(form.getNmStatus()).orElseThrow(() -> new ValidacaoException(
                        NordHttpEnum.HTTP_400, "Status inválido. Use NAO_CONVIDADO, CONVIDADO, CONFIRMADO ou NAO_IRA.", null));
        CasamentoConvidado c = new CasamentoConvidado();
        c.setNmConvidado(form.getNmConvidado().trim());
        c.setNmGrupo(CasamentoFornecedorServiceImpl.vazioParaNulo(form.getNmGrupo()));
        c.setNrTelefone(CasamentoFornecedorServiceImpl.vazioParaNulo(form.getNrTelefone()));
        c.setNmRelacao(CasamentoFornecedorServiceImpl.vazioParaNulo(form.getNmRelacao()));
        c.setNmStatus(status.name());
        c.setNrAcompanhantes(form.getNrAcompanhantes() == null ? 0 : form.getNrAcompanhantes());
        c.setNmMesa(CasamentoFornecedorServiceImpl.vazioParaNulo(form.getNmMesa()));
        c.setIdConvidadoPrincipal(form.getIdConvidadoPrincipal());
        c.setNmCortejo(CasamentoFornecedorServiceImpl.vazioParaNulo(form.getNmCortejo()));
        return c;
    }
}
