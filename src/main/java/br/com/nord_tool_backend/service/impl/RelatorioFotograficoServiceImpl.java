package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.dto.RelatorioFotoDto;
import br.com.nord_tool_backend.dto.RelatorioFotograficoDto;
import br.com.nord_tool_backend.dto.RelatorioItemDto;
import br.com.nord_tool_backend.dto.RelatorioModeloDto;
import br.com.nord_tool_backend.dto.RelatorioModeloItemDto;
import br.com.nord_tool_backend.exception.ConflitoException;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.exception.NaoEncontradoException;
import br.com.nord_tool_backend.exception.NegocioException;
import br.com.nord_tool_backend.form.RelatorioFotoForm;
import br.com.nord_tool_backend.form.RelatorioFotograficoForm;
import br.com.nord_tool_backend.form.RelatorioItemForm;
import br.com.nord_tool_backend.form.RelatorioModeloForm;
import br.com.nord_tool_backend.form.RelatorioStatusForm;
import br.com.nord_tool_backend.repository.RelatorioFotograficoRepository;
import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.security.UsuarioAutenticado;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.service.ContextoFaseService;
import br.com.nord_tool_backend.service.RelatorioFotograficoService;
import br.com.nord_tool_backend.storage.ArmazenamentoService;
import br.com.nord_tool_backend.storage.ArquivoDownload;
import br.com.nord_tool_backend.storage.ArquivoValidador;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Relatório fotográfico: modelos (ADMIN do módulo), relatórios por fase (ESCRITA) e fotos no
 * armazenamento comum. Relatório concluído não muda até ser reaberto.
 */
@Service
public class RelatorioFotograficoServiceImpl implements RelatorioFotograficoService {

    static final String RASCUNHO = "RASCUNHO";
    static final String CONCLUIDO = "CONCLUIDO";
    static final Set<Integer> LAYOUTS = new HashSet<>(Arrays.asList(1, 2, 4, 6));
    static final int MAX_FOTOS = 200;
    static final String MSG_CONFLITO = "O relatório mudou. Sincronize e tente novamente.";

    private final RelatorioFotograficoRepository repository;
    private final AutorizacaoService autorizacao;
    private final ContextoFaseService contexto;
    private final ArmazenamentoService armazenamento;

    public RelatorioFotograficoServiceImpl(RelatorioFotograficoRepository repository, AutorizacaoService autorizacao,
                                           ContextoFaseService contexto, ArmazenamentoService armazenamento) {
        this.repository = repository;
        this.autorizacao = autorizacao;
        this.contexto = contexto;
        this.armazenamento = armazenamento;
    }

    // ---------- modelos ----------

    @Override
    @Transactional(readOnly = true)
    public List<RelatorioModeloDto> listarModelos() {
        autorizacao.exigir(Modulo.RELATORIO_FOTOGRAFICO, Acao.LEITURA);
        Map<Long, List<RelatorioModeloItemDto>> itens = repository.listarItensDeModelos().stream()
                .collect(Collectors.groupingBy(RelatorioModeloItemDto::getIdModelo));
        List<RelatorioModeloDto> modelos = repository.listarModelos();
        modelos.forEach(m -> m.setItens(itens.getOrDefault(m.getId(), new ArrayList<>())));
        return modelos;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RelatorioModeloDto criarModelo(RelatorioModeloForm form) {
        autorizacao.exigir(Modulo.RELATORIO_FOTOGRAFICO, Acao.ADMIN);
        RelatorioModeloDto m = modeloDe(form);
        Long id = repository.inserirModelo(m);
        repository.substituirItensDoModelo(id, itensDoModelo(form.getItens()));
        return modelo(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RelatorioModeloDto alterarModelo(Long id, RelatorioModeloForm form) {
        autorizacao.exigir(Modulo.RELATORIO_FOTOGRAFICO, Acao.ADMIN);
        int versao = versaoObrigatoria(form.getNrVersao());
        modelo(id);
        RelatorioModeloDto m = modeloDe(form);
        m.setId(id);
        if (repository.alterarModelo(m, versao) == 0) throw new ConflitoException("O modelo mudou. Sincronize e tente novamente.");
        repository.substituirItensDoModelo(id, itensDoModelo(form.getItens()));
        return modelo(id);
    }

    // ---------- relatórios ----------

    @Override
    @Transactional(readOnly = true)
    public List<RelatorioFotograficoDto> listar() {
        autorizacao.exigir(Modulo.RELATORIO_FOTOGRAFICO, Acao.LEITURA);
        return repository.listar();
    }

    @Override
    @Transactional(readOnly = true)
    public RelatorioFotograficoDto detalhar(Long id) {
        autorizacao.exigir(Modulo.RELATORIO_FOTOGRAFICO, Acao.LEITURA);
        return completo(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RelatorioFotograficoDto criar(RelatorioFotograficoForm form) {
        UsuarioAutenticado u = autorizacao.exigir(Modulo.RELATORIO_FOTOGRAFICO, Acao.ESCRITA);
        RelatorioModeloDto modelo = form.getIdModelo() == null ? null : modelo(form.getIdModelo());
        if (modelo != null && Boolean.FALSE.equals(modelo.getInAtivo())) throw new EntradaInvalidaException("Este modelo está desativado");

        RelatorioFotograficoDto r = cabecalho(form);
        if (form.getNrFotosPagina() == null) r.setNrFotosPagina(modelo == null ? 2 : modelo.getNrFotosPagina());
        r.setIdModelo(modelo == null ? null : modelo.getId());
        r.setIdAutor(u.getId());
        Long id = repository.inserir(r, contexto.faseParaCriar(null));

        List<RelatorioItemForm> itens = form.getItens() == null ? new ArrayList<>() : form.getItens();
        if (itens.isEmpty() && modelo != null) {
            for (RelatorioModeloItemDto mi : modelo.getItens()) {
                repository.inserirItem(item(id, mi.getNrOrdem(), mi.getNmItem(), mi.getTxOrientacao(), null));
            }
        } else {
            int ordem = 1;
            for (RelatorioItemForm f : itens) {
                repository.inserirItem(item(id, ordem++, f.getNmItem().trim(), texto(f.getTxOrientacao()), texto(f.getTxResposta())));
            }
        }
        return completo(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RelatorioFotograficoDto alterar(Long id, RelatorioFotograficoForm form) {
        autorizacao.exigir(Modulo.RELATORIO_FOTOGRAFICO, Acao.ESCRITA);
        int versao = versaoObrigatoria(form.getNrVersao());
        RelatorioFotograficoDto atual = editavel(id);
        RelatorioFotograficoDto r = cabecalho(form);
        r.setId(id);
        if (r.getNrFotosPagina() == null) r.setNrFotosPagina(atual.getNrFotosPagina());
        if (repository.alterar(r, versao) == 0) throw new ConflitoException(MSG_CONFLITO);
        if (form.getItens() != null) sincronizarItens(id, form.getItens());
        return completo(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RelatorioFotograficoDto alterarStatus(Long id, RelatorioStatusForm form) {
        autorizacao.exigir(Modulo.RELATORIO_FOTOGRAFICO, Acao.ESCRITA);
        int versao = versaoObrigatoria(form.getNrVersao());
        String status = form.getCdStatus().trim().toUpperCase();
        if (!RASCUNHO.equals(status) && !CONCLUIDO.equals(status)) throw new EntradaInvalidaException("Status inválido");
        relatorio(id);
        if (repository.alterarStatus(id, status, versao) == 0) throw new ConflitoException(MSG_CONFLITO);
        return completo(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void excluir(Long id, Integer nrVersao) {
        autorizacao.exigir(Modulo.RELATORIO_FOTOGRAFICO, Acao.ESCRITA);
        int versao = versaoObrigatoria(nrVersao);
        relatorio(id);
        List<Long> arquivos = repository.listarFotos(id).stream().map(RelatorioFotoDto::getIdArquivo).collect(Collectors.toList());
        if (repository.deletar(id, versao) == 0) throw new ConflitoException(MSG_CONFLITO);
        apagarArquivos(arquivos);
    }

    // ---------- fotos ----------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RelatorioFotoDto adicionarFoto(Long idItem, String nome, byte[] bytes, String txLegenda) {
        autorizacao.exigir(Modulo.RELATORIO_FOTOGRAFICO, Acao.ESCRITA);
        Long idRelatorio = repository.relatorioDoItem(idItem).orElseThrow(() -> new NaoEncontradoException("Item não encontrado"));
        editavel(idRelatorio);
        if (repository.listarFotos(idRelatorio).size() >= MAX_FOTOS) {
            throw new NegocioException("O relatório pode ter no máximo " + MAX_FOTOS + " fotos");
        }
        String legenda = texto(txLegenda);
        if (legenda != null && legenda.length() > 300) throw new EntradaInvalidaException("A legenda deve ter no máximo 300 caracteres");
        String contentType = ArquivoValidador.validarImagem(nome, bytes);
        Long idArquivo = armazenamento.salvar(nome, contentType, bytes);
        Long idFoto = repository.inserirFoto(idItem, idArquivo, legenda);
        repository.tocar(idRelatorio);
        return foto(idFoto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RelatorioFotoDto alterarFoto(Long idFoto, RelatorioFotoForm form) {
        autorizacao.exigir(Modulo.RELATORIO_FOTOGRAFICO, Acao.ESCRITA);
        RelatorioFotoDto f = fotoNoEscopo(idFoto);
        editavel(f.getIdRelatorio());
        int ordem = form.getNrOrdem() == null ? f.getNrOrdem() : form.getNrOrdem();
        repository.alterarFoto(idFoto, texto(form.getTxLegenda()), ordem);
        repository.tocar(f.getIdRelatorio());
        return foto(idFoto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void excluirFoto(Long idFoto) {
        autorizacao.exigir(Modulo.RELATORIO_FOTOGRAFICO, Acao.ESCRITA);
        RelatorioFotoDto f = fotoNoEscopo(idFoto);
        editavel(f.getIdRelatorio());
        repository.deletarFoto(idFoto);
        repository.tocar(f.getIdRelatorio());
        apagarArquivos(java.util.Collections.singletonList(f.getIdArquivo()));
    }

    @Override
    @Transactional(readOnly = true)
    public ArquivoDownload baixarFoto(Long idFoto) {
        autorizacao.exigir(Modulo.RELATORIO_FOTOGRAFICO, Acao.LEITURA);
        RelatorioFotoDto f = fotoNoEscopo(idFoto);
        long versao = f.getDhCriacao() == null ? 0L : f.getDhCriacao().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        return new ArquivoDownload(armazenamento.abrir(f.getIdArquivo()), versao);
    }

    // ---------- apoio ----------

    /** Itens: com id existente são atualizados, sem id são criados, os que sumiram da lista são excluídos (com fotos). */
    private void sincronizarItens(Long idRelatorio, List<RelatorioItemForm> lista) {
        List<RelatorioItemDto> atuais = repository.listarItens(idRelatorio);
        Set<Long> idsAtuais = atuais.stream().map(RelatorioItemDto::getId).collect(Collectors.toSet());
        Set<Long> mantidos = new HashSet<>();
        for (RelatorioItemForm f : lista) {
            if (f.getId() != null && !idsAtuais.contains(f.getId())) throw new EntradaInvalidaException("Item não pertence a este relatório");
            if (f.getId() != null && !mantidos.add(f.getId())) throw new EntradaInvalidaException("Item repetido na lista");
        }
        List<RelatorioFotoDto> fotos = repository.listarFotos(idRelatorio);
        List<Long> arquivos = new ArrayList<>();
        for (RelatorioItemDto a : atuais) {
            if (mantidos.contains(a.getId())) continue;
            fotos.stream().filter(ft -> a.getId().equals(ft.getIdItem())).forEach(ft -> arquivos.add(ft.getIdArquivo()));
            repository.deletarItem(a.getId(), idRelatorio);
        }
        int ordem = 1;
        for (RelatorioItemForm f : lista) {
            RelatorioItemDto i = item(idRelatorio, ordem++, f.getNmItem().trim(), texto(f.getTxOrientacao()), texto(f.getTxResposta()));
            if (f.getId() == null) {
                repository.inserirItem(i);
            } else {
                i.setId(f.getId());
                repository.alterarItem(i);
            }
        }
        apagarArquivos(arquivos);
    }

    /**
     * Apaga os arquivos depois que as linhas que apontam para eles saíram. Na mesma transação: no provedor
     * Postgres o rollback desfaz tudo junto (mesmo padrão dos comprovantes da Caixinha).
     */
    private void apagarArquivos(List<Long> ids) {
        ids.forEach(armazenamento::apagar);
    }

    private RelatorioFotograficoDto completo(Long id) {
        RelatorioFotograficoDto r = relatorio(id);
        List<RelatorioItemDto> itens = repository.listarItens(id);
        Map<Long, List<RelatorioFotoDto>> fotos = repository.listarFotos(id).stream()
                .collect(Collectors.groupingBy(RelatorioFotoDto::getIdItem));
        itens.forEach(i -> i.setFotos(fotos.getOrDefault(i.getId(), new ArrayList<>())));
        r.setItens(itens);
        return r;
    }

    private RelatorioFotograficoDto relatorio(Long id) {
        return repository.buscar(id).orElseThrow(() -> new NaoEncontradoException("Relatório não encontrado"));
    }

    private RelatorioFotograficoDto editavel(Long id) {
        RelatorioFotograficoDto r = relatorio(id);
        if (CONCLUIDO.equals(r.getCdStatus())) throw new NegocioException("Relatório concluído. Reabra para editar.");
        return r;
    }

    /** A foto só é vista por quem enxerga o relatório (escopo de fase). */
    private RelatorioFotoDto fotoNoEscopo(Long idFoto) {
        RelatorioFotoDto f = repository.buscarFoto(idFoto).orElseThrow(() -> new NaoEncontradoException("Foto não encontrada"));
        relatorio(f.getIdRelatorio());
        return f;
    }

    private RelatorioFotoDto foto(Long idFoto) {
        return repository.buscarFoto(idFoto).orElseThrow(() -> new NaoEncontradoException("Foto não encontrada"));
    }

    private RelatorioModeloDto modelo(Long id) {
        Map<Long, List<RelatorioModeloItemDto>> itens = repository.listarItensDeModelos().stream()
                .filter(i -> id.equals(i.getIdModelo()))
                .collect(Collectors.groupingBy(RelatorioModeloItemDto::getIdModelo));
        RelatorioModeloDto m = repository.listarModelos().stream().filter(x -> id.equals(x.getId())).findFirst()
                .orElseThrow(() -> new NaoEncontradoException("Modelo não encontrado"));
        m.setItens(itens.getOrDefault(id, new ArrayList<>()));
        return m;
    }

    private static RelatorioModeloDto modeloDe(RelatorioModeloForm f) {
        RelatorioModeloDto m = new RelatorioModeloDto();
        m.setNmModelo(f.getNmModelo().trim());
        m.setTxDescricao(texto(f.getTxDescricao()));
        m.setNrFotosPagina(layout(f.getNrFotosPagina()));
        m.setInAtivo(f.getInAtivo() == null || f.getInAtivo());
        return m;
    }

    private static List<RelatorioModeloItemDto> itensDoModelo(List<RelatorioItemForm> lista) {
        List<RelatorioModeloItemDto> itens = new ArrayList<>();
        int ordem = 1;
        for (RelatorioItemForm f : lista == null ? new ArrayList<RelatorioItemForm>() : lista) {
            RelatorioModeloItemDto i = new RelatorioModeloItemDto();
            i.setNrOrdem(ordem++);
            i.setNmItem(f.getNmItem().trim());
            i.setTxOrientacao(texto(f.getTxOrientacao()));
            itens.add(i);
        }
        return itens;
    }

    private static RelatorioFotograficoDto cabecalho(RelatorioFotograficoForm f) {
        RelatorioFotograficoDto r = new RelatorioFotograficoDto();
        r.setNmTitulo(f.getNmTitulo().trim());
        r.setNmLocal(texto(f.getNmLocal()));
        r.setDtRelatorio(f.getDtRelatorio());
        r.setNrFotosPagina(f.getNrFotosPagina() == null ? null : layout(f.getNrFotosPagina()));
        return r;
    }

    private static RelatorioItemDto item(Long idRelatorio, int ordem, String nome, String orientacao, String resposta) {
        RelatorioItemDto i = new RelatorioItemDto();
        i.setIdRelatorio(idRelatorio);
        i.setNrOrdem(ordem);
        i.setNmItem(nome);
        i.setTxOrientacao(orientacao);
        i.setTxResposta(resposta);
        return i;
    }

    private static int layout(Integer n) {
        if (n == null || !LAYOUTS.contains(n)) throw new EntradaInvalidaException("Fotos por página deve ser 1, 2, 4 ou 6");
        return n;
    }

    private static String texto(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }

    private static int versaoObrigatoria(Integer nrVersao) {
        if (nrVersao == null) throw new EntradaInvalidaException("Informe a versão (nrVersao)");
        return nrVersao;
    }
}
