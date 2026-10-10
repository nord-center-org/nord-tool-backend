package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.exception.NaoEncontradoException;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.domain.CasamentoConvidado;
import br.com.nord_tool_backend.domain.enums.StatusConvidadoEnum;
import br.com.nord_tool_backend.dto.CasamentoConvidadoDto;
import br.com.nord_tool_backend.dto.ImportacaoConvidadosDto;
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

    private final AutorizacaoService autorizacao;

    @Override
    @Transactional(readOnly = true)
    public List<CasamentoConvidadoDto> listar() {
        autorizacao.exigir(Modulo.CASAMENTO, Acao.LEITURA);
        return repository.listarConvidados().stream().map(CasamentoConvidadoDto::de).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CasamentoConvidadoDto buscar(Long id) {
        autorizacao.exigir(Modulo.CASAMENTO, Acao.LEITURA);
        return CasamentoConvidadoDto.de(convidado(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CasamentoConvidadoDto criar(CasamentoConvidadoForm form) {
        autorizacao.exigir(Modulo.CASAMENTO, Acao.ESCRITA);
        validarPrincipal(null, form.getIdConvidadoPrincipal());
        Long id = repository.inserirConvidado(converter(form));
        return CasamentoConvidadoDto.de(convidado(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CasamentoConvidadoDto alterar(Long id, CasamentoConvidadoForm form) {
        autorizacao.exigir(Modulo.CASAMENTO, Acao.ESCRITA);
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
        autorizacao.exigir(Modulo.CASAMENTO, Acao.ESCRITA);
        convidado(id);
        repository.deletarConvidado(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportacaoConvidadosDto importar(String nomeArquivo, byte[] bytes) {
        autorizacao.exigir(Modulo.CASAMENTO, Acao.ESCRITA);
        if (bytes == null || bytes.length == 0) {
            throw new EntradaInvalidaException("Planilha vazia");
        }
        if (bytes.length > MAX_BYTES_PLANILHA) {
            throw new EntradaInvalidaException("A planilha deve ter no máximo 5 MB");
        }
        if (nomeArquivo == null || !nomeArquivo.toLowerCase().endsWith(".xlsx")) {
            throw new EntradaInvalidaException("Envie um arquivo .xlsx");
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
                .orElseThrow(() -> new NaoEncontradoException("Convidado não encontrado"));
    }

    /** O principal deve existir, não pode ser o próprio convidado nem alguém ligado a ele (sem ligação circular). */
    private void validarPrincipal(Long idConvidado, Long idPrincipal) {
        if (idPrincipal == null) return;
        if (idPrincipal.equals(idConvidado)) {
            throw new EntradaInvalidaException("Um convidado não pode acompanhar a si mesmo");
        }
        Long atual = idPrincipal;
        for (int passos = 0; atual != null && passos < 1000; passos++) {
            CasamentoConvidado c = repository.buscarConvidado(atual)
                    .orElseThrow(() -> new EntradaInvalidaException("Convidado principal não encontrado"));
            if (idConvidado != null && idConvidado.equals(c.getIdConvidadoPrincipal())) {
                throw new EntradaInvalidaException("Ligação circular: esse convidado já está ligado a " + c.getNmConvidado());
            }
            atual = c.getIdConvidadoPrincipal();
        }
    }

    CasamentoConvidado converter(CasamentoConvidadoForm form) {
        StatusConvidadoEnum status = form.getNmStatus() == null || form.getNmStatus().trim().isEmpty()
                ? StatusConvidadoEnum.NAO_CONVIDADO
                : StatusConvidadoEnum.de(form.getNmStatus()).orElseThrow(() -> new EntradaInvalidaException("Status inválido. Use NAO_CONVIDADO, CONVIDADO, CONFIRMADO ou NAO_IRA."));
        CasamentoConvidado c = new CasamentoConvidado();
        c.setNmConvidado(form.getNmConvidado().trim());
        c.setNmGrupo(CasamentoFornecedorServiceImpl.vazioParaNulo(form.getNmGrupo()));
        c.setNrTelefone(CasamentoFornecedorServiceImpl.vazioParaNulo(form.getNrTelefone()));
        c.setNmRelacao(CasamentoFornecedorServiceImpl.vazioParaNulo(form.getNmRelacao()));
        c.setNmStatus(status.name());
        c.setNrAcompanhantes(form.getNrAcompanhantes() == null ? 0 : form.getNrAcompanhantes());
        c.setNmMesa(CasamentoFornecedorServiceImpl.vazioParaNulo(form.getNmMesa()));
        c.setIdConvidadoPrincipal(form.getIdConvidadoPrincipal());
        return c;
    }
}
