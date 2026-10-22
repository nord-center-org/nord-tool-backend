package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.dto.MapeamentoDto;
import br.com.nord_tool_backend.dto.MapeamentoLinhaDto;
import br.com.nord_tool_backend.exception.ConflitoException;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.exception.NaoEncontradoException;
import br.com.nord_tool_backend.exception.NegocioException;
import br.com.nord_tool_backend.form.MapeamentoForm;
import br.com.nord_tool_backend.form.MapeamentoLinhaForm;
import br.com.nord_tool_backend.form.MapeamentoStatusForm;
import br.com.nord_tool_backend.repository.MapeamentoRepository;
import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.security.UsuarioAutenticado;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.service.ContextoFaseService;
import br.com.nord_tool_backend.service.MapeamentoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Mapeamentos: contagem em campo (atividades, materiais, efetivo) e levantamento de quantitativos
 * (projeto × medido × solicitado). Linhas são gravadas em bloco; concluído não muda até reabrir.
 */
@Service
public class MapeamentoServiceImpl implements MapeamentoService {

    static final List<String> TIPOS = Arrays.asList("CONTAGEM", "QUANTITATIVO");
    static final BigDecimal TOLERANCIA_PADRAO = new BigDecimal("5");
    static final String MSG_CONFLITO = "O mapeamento mudou. Sincronize e tente novamente.";

    private final MapeamentoRepository repository;
    private final AutorizacaoService autorizacao;
    private final ContextoFaseService contexto;

    public MapeamentoServiceImpl(MapeamentoRepository repository, AutorizacaoService autorizacao, ContextoFaseService contexto) {
        this.repository = repository;
        this.autorizacao = autorizacao;
        this.contexto = contexto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MapeamentoDto> listar() {
        autorizacao.exigir(Modulo.MAPEAMENTOS, Acao.LEITURA);
        return repository.listar();
    }

    @Override
    @Transactional(readOnly = true)
    public MapeamentoDto detalhar(Long id) {
        autorizacao.exigir(Modulo.MAPEAMENTOS, Acao.LEITURA);
        return completo(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MapeamentoDto criar(MapeamentoForm form) {
        UsuarioAutenticado u = autorizacao.exigir(Modulo.MAPEAMENTOS, Acao.ESCRITA);
        String tipo = form.getCdTipo() == null ? "" : form.getCdTipo().trim().toUpperCase(Locale.ROOT);
        if (!TIPOS.contains(tipo)) throw new EntradaInvalidaException("Tipo deve ser CONTAGEM ou QUANTITATIVO");
        MapeamentoDto m = cabecalho(form);
        m.setCdTipo(tipo);
        m.setIdAutor(u.getId());
        Long id = repository.inserir(m, contexto.faseParaCriar(null));
        repository.substituirLinhas(id, linhas(form.getLinhas()));
        return completo(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MapeamentoDto alterar(Long id, MapeamentoForm form) {
        autorizacao.exigir(Modulo.MAPEAMENTOS, Acao.ESCRITA);
        int versao = versaoObrigatoria(form.getNrVersao());
        MapeamentoDto atual = mapeamento(id);
        if ("CONCLUIDO".equals(atual.getCdStatus())) throw new NegocioException("Mapeamento concluído. Reabra para editar.");
        MapeamentoDto m = cabecalho(form);
        m.setId(id);
        // Sem tolerância no formulário mantém a atual (o padrão de 5% só vale na criação).
        if (form.getPcTolerancia() == null) m.setPcTolerancia(atual.getPcTolerancia());
        if (repository.alterar(m, versao) == 0) throw new ConflitoException(MSG_CONFLITO);
        if (form.getLinhas() != null) repository.substituirLinhas(id, linhas(form.getLinhas()));
        return completo(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MapeamentoDto alterarStatus(Long id, MapeamentoStatusForm form) {
        autorizacao.exigir(Modulo.MAPEAMENTOS, Acao.ESCRITA);
        int versao = versaoObrigatoria(form.getNrVersao());
        String status = form.getCdStatus().trim().toUpperCase(Locale.ROOT);
        if (!"RASCUNHO".equals(status) && !"CONCLUIDO".equals(status)) throw new EntradaInvalidaException("Status inválido");
        mapeamento(id);
        if (repository.alterarStatus(id, status, versao) == 0) throw new ConflitoException(MSG_CONFLITO);
        return completo(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void excluir(Long id, Integer nrVersao) {
        autorizacao.exigir(Modulo.MAPEAMENTOS, Acao.ESCRITA);
        int versao = versaoObrigatoria(nrVersao);
        mapeamento(id);
        if (repository.deletar(id, versao) == 0) throw new ConflitoException(MSG_CONFLITO);
    }

    // ---------- apoio ----------

    private MapeamentoDto completo(Long id) {
        MapeamentoDto m = mapeamento(id);
        m.setLinhas(repository.listarLinhas(id));
        return m;
    }

    private MapeamentoDto mapeamento(Long id) {
        return repository.buscar(id).orElseThrow(() -> new NaoEncontradoException("Mapeamento não encontrado"));
    }

    private static MapeamentoDto cabecalho(MapeamentoForm f) {
        MapeamentoDto m = new MapeamentoDto();
        m.setNmTitulo(f.getNmTitulo().trim());
        m.setNmLocal(texto(f.getNmLocal()));
        m.setDtMapeamento(f.getDtMapeamento());
        m.setPcTolerancia(f.getPcTolerancia() == null ? TOLERANCIA_PADRAO : f.getPcTolerancia());
        m.setTxObservacao(texto(f.getTxObservacao()));
        return m;
    }

    private static List<MapeamentoLinhaDto> linhas(List<MapeamentoLinhaForm> lista) {
        List<MapeamentoLinhaDto> saida = new ArrayList<>();
        if (lista == null) return saida;
        int ordem = 1;
        for (MapeamentoLinhaForm f : lista) {
            MapeamentoLinhaDto l = new MapeamentoLinhaDto();
            l.setNrOrdem(ordem++);
            l.setNmGrupo(texto(f.getNmGrupo()));
            l.setNmItem(f.getNmItem().trim());
            String unidade = texto(f.getNmUnidade());
            l.setNmUnidade(unidade == null ? "un" : unidade);
            l.setQtProjeto(f.getQtProjeto());
            l.setQtMedida(f.getQtMedida());
            l.setQtSolicitada(f.getQtSolicitada());
            l.setTxObservacao(texto(f.getTxObservacao()));
            saida.add(l);
        }
        return saida;
    }

    private static String texto(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }

    private static int versaoObrigatoria(Integer nrVersao) {
        if (nrVersao == null) throw new EntradaInvalidaException("Informe a versão (nrVersao)");
        return nrVersao;
    }
}
