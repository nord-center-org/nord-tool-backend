package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.domain.Medicao;
import br.com.nord_tool_backend.domain.MedicaoStatus;
import br.com.nord_tool_backend.dto.MedicaoDto;
import br.com.nord_tool_backend.dto.MedicaoListaDto;
import br.com.nord_tool_backend.dto.MedicaoStatusDto;
import br.com.nord_tool_backend.exception.ConflitoException;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.exception.NaoEncontradoException;
import br.com.nord_tool_backend.form.MedicaoForm;
import br.com.nord_tool_backend.form.MedicaoStatusForm;
import br.com.nord_tool_backend.repository.MedicaoRepository;
import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.service.MedicaoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MedicaoServiceImpl implements MedicaoService {

    static final String STATUS_PADRAO = "PENDENTE";
    static final String MSG_CONFLITO = "A medição mudou. Sincronize e tente novamente.";

    private final MedicaoRepository repository;
    private final AutorizacaoService autorizacao;

    public MedicaoServiceImpl(MedicaoRepository repository, AutorizacaoService autorizacao) {
        this.repository = repository;
        this.autorizacao = autorizacao;
    }

    @Override
    @Transactional(readOnly = true)
    public MedicaoListaDto listar() {
        autorizacao.exigir(Modulo.MEDICOES, Acao.LEITURA);
        List<MedicaoDto> medicoes = repository.listar().stream().map(MedicaoDto::de).collect(Collectors.toList());
        List<MedicaoStatusDto> status = repository.listarStatus().stream().map(MedicaoStatusDto::de).collect(Collectors.toList());
        return new MedicaoListaDto(medicoes, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MedicaoDto criar(MedicaoForm form) {
        autorizacao.exigir(Modulo.MEDICOES, Acao.ESCRITA);
        Medicao nova = converter(form);
        nova.setCdStatus(statusAtivo(vazio(form.getCdStatus()) ? STATUS_PADRAO : form.getCdStatus()));
        return MedicaoDto.de(medicao(repository.inserir(nova)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MedicaoDto alterar(Long id, MedicaoForm form) {
        autorizacao.exigir(Modulo.MEDICOES, Acao.ESCRITA);
        int versao = versaoObrigatoria(form.getNrVersao());
        Medicao atual = medicao(id);
        Medicao nova = converter(form);
        nova.setId(id);
        // Sem status no formulário mantém o atual (mesmo que o status tenha sido desativado depois).
        nova.setCdStatus(vazio(form.getCdStatus()) || form.getCdStatus().trim().equalsIgnoreCase(atual.getCdStatus())
                ? atual.getCdStatus() : statusAtivo(form.getCdStatus()));
        if (repository.alterar(nova, versao) == 0) throw conflito();
        return MedicaoDto.de(medicao(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MedicaoDto alterarStatus(Long id, MedicaoStatusForm form) {
        autorizacao.exigir(Modulo.MEDICOES, Acao.ESCRITA);
        int versao = versaoObrigatoria(form.getNrVersao());
        medicao(id);
        if (repository.alterarStatus(id, statusAtivo(form.getCdStatus()), versao) == 0) throw conflito();
        return MedicaoDto.de(medicao(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void excluir(Long id, Integer nrVersao) {
        autorizacao.exigir(Modulo.MEDICOES, Acao.ESCRITA);
        int versao = versaoObrigatoria(nrVersao);
        medicao(id);
        if (repository.deletar(id, versao) == 0) throw conflito();
    }

    // ---------- apoio ----------

    private Medicao medicao(Long id) {
        return repository.buscar(id).orElseThrow(() -> new NaoEncontradoException("Medição não encontrada"));
    }

    private Medicao converter(MedicaoForm f) {
        Medicao m = new Medicao();
        m.setNrMedicao(f.getNrMedicao());
        m.setCdContrato(f.getCdContrato().trim());
        m.setNmRazaoSocial(f.getNmRazaoSocial().trim());
        m.setNrContrato(f.getNrContrato().trim());
        m.setDtMedicao(f.getDtMedicao());
        m.setVlMedicao(f.getVlMedicao());
        m.setTxMemorial(vazio(f.getTxMemorial()) ? null : f.getTxMemorial().trim());
        return m;
    }

    /** O status precisa existir e estar ativo para ser atribuído. */
    private String statusAtivo(String cdStatus) {
        String codigo = cdStatus == null ? "" : cdStatus.trim();
        return repository.listarStatus().stream()
                .filter(s -> s.getCdStatus().equalsIgnoreCase(codigo) && !Boolean.FALSE.equals(s.getInAtivo()))
                .map(MedicaoStatus::getCdStatus)
                .findFirst()
                .orElseThrow(() -> new EntradaInvalidaException("Status inválido"));
    }

    private static int versaoObrigatoria(Integer nrVersao) {
        if (nrVersao == null) throw new EntradaInvalidaException("Informe a versão da medição (nrVersao)");
        return nrVersao;
    }

    private static ConflitoException conflito() {
        return new ConflitoException(MSG_CONFLITO);
    }

    private static boolean vazio(String s) {
        return s == null || s.trim().isEmpty();
    }
}
