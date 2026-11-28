package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.dto.CasamentoArteDto;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.exception.NaoEncontradoException;
import br.com.nord_tool_backend.exception.NegocioException;
import br.com.nord_tool_backend.form.CasamentoArteForm;
import br.com.nord_tool_backend.repository.CasamentoArteRepository;
import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.service.CasamentoArteService;
import br.com.nord_tool_backend.storage.ArmazenamentoService;
import br.com.nord_tool_backend.storage.ArquivoDownload;
import br.com.nord_tool_backend.storage.ArquivoValidador;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Artes do casamento: convite, save the date, cardápio e outras peças, em PDF ou imagem (JPEG/PNG, até 15 MB), para
 * guardar e ver no NordTool. São do casamento (todos com acesso ao módulo veem); o arquivo fica no armazenamento
 * do NordTool, como os anexos dos fornecedores.
 */
@Service
@RequiredArgsConstructor
public class CasamentoArteServiceImpl implements CasamentoArteService {

    static final int MAX_ARTES = 300;
    static final int MAX_TITULO = 150;
    static final String OUTRO = "OUTRO";
    static final Set<String> CATEGORIAS = new LinkedHashSet<>(Arrays.asList("CONVITE", "SAVE_THE_DATE", "CARDAPIO", "PAPELARIA", "IDENTIDADE", OUTRO));

    private final CasamentoArteRepository repository;
    private final ArmazenamentoService armazenamento;
    private final AutorizacaoService autorizacao;

    @Override
    @Transactional(readOnly = true)
    public List<CasamentoArteDto> listar() {
        autorizacao.exigir(Modulo.CASAMENTO, Acao.LEITURA);
        return repository.listar();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CasamentoArteDto enviar(String nomeArquivo, byte[] bytes, String titulo, String categoria) {
        Long eu = autorizacao.exigir(Modulo.CASAMENTO, Acao.ESCRITA).getId();
        if (repository.contar() >= MAX_ARTES) {
            throw new NegocioException("Já são " + MAX_ARTES + " artes guardadas. Exclua alguma para enviar outra.");
        }
        String contentType = ArquivoValidador.validarContrato(nomeArquivo, bytes);
        String t = titulo(titulo == null || titulo.trim().isEmpty() ? semExtensao(nomeArquivo) : titulo);
        String c = categoria(categoria);
        Long idArquivo = armazenamento.salvar(nomeArquivo, contentType, bytes);
        Long id = repository.inserir(idArquivo, t, c, eu);
        return arte(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CasamentoArteDto atualizar(Long id, CasamentoArteForm form) {
        autorizacao.exigir(Modulo.CASAMENTO, Acao.ESCRITA);
        CasamentoArteDto atual = arte(id);
        String c = form.getCdCategoria() == null ? atual.getCdCategoria() : categoria(form.getCdCategoria());
        repository.atualizar(id, titulo(form.getNmTitulo()), c);
        return arte(id);
    }

    @Override
    @Transactional(readOnly = true)
    public ArquivoDownload baixar(Long id) {
        autorizacao.exigir(Modulo.CASAMENTO, Acao.LEITURA);
        CasamentoArteDto a = arte(id);
        return new ArquivoDownload(armazenamento.abrir(a.getIdArquivo()), a.getNrVersao());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void excluir(Long id) {
        autorizacao.exigir(Modulo.CASAMENTO, Acao.ESCRITA);
        CasamentoArteDto a = arte(id);
        repository.deletar(id);
        armazenamento.apagar(a.getIdArquivo());
    }

    private CasamentoArteDto arte(Long id) {
        return repository.buscar(id).orElseThrow(() -> new NaoEncontradoException("Arte não encontrada"));
    }

    static String titulo(String titulo) {
        String t = titulo == null ? "" : titulo.replaceAll("\\p{Cntrl}", " ").trim();
        if (t.isEmpty()) throw new EntradaInvalidaException("Informe o título");
        return t.length() > MAX_TITULO ? t.substring(0, MAX_TITULO) : t;
    }

    static String categoria(String categoria) {
        if (categoria == null || categoria.trim().isEmpty()) return OUTRO;
        String c = categoria.trim().toUpperCase(Locale.ROOT);
        if (!CATEGORIAS.contains(c)) throw new EntradaInvalidaException("Categoria inválida");
        return c;
    }

    private static String semExtensao(String nome) {
        String n = nome == null ? "" : nome;
        int ponto = n.lastIndexOf('.');
        return ponto > 0 ? n.substring(0, ponto) : n;
    }
}
