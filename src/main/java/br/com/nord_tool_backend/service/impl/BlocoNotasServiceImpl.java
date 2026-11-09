package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.dto.BlocoCadernoDto;
import br.com.nord_tool_backend.dto.BlocoPaginaDto;
import br.com.nord_tool_backend.exception.ConflitoException;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.exception.NaoEncontradoException;
import br.com.nord_tool_backend.exception.NegocioException;
import br.com.nord_tool_backend.form.BlocoCadernoForm;
import br.com.nord_tool_backend.form.BlocoConteudoForm;
import br.com.nord_tool_backend.form.BlocoPaginaForm;
import br.com.nord_tool_backend.repository.BlocoNotasRepository;
import br.com.nord_tool_backend.security.UsuarioAutenticado;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.service.BlocoNotasService;
import br.com.nord_tool_backend.storage.ArmazenamentoService;
import br.com.nord_tool_backend.storage.ArquivoConteudo;
import br.com.nord_tool_backend.storage.ArquivoDownload;
import br.com.nord_tool_backend.storage.ArquivoValidador;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Bloco de notas: cada pessoa só vê e altera os próprios cadernos (qualquer usuário logado). O desenho de cada página
 * é o JSON do editor; o servidor confere só que é um objeto com a lista "itens" e o tamanho.
 */
@Service
@RequiredArgsConstructor
public class BlocoNotasServiceImpl implements BlocoNotasService {

    static final int MAX_CADERNOS = 200;
    static final int MAX_PAGINAS = 500;
    static final int MAX_CONTEUDO = 3_000_000;
    static final int LARGURA_A4 = 794;
    static final int ALTURA_A4 = 1123;
    static final String PDF = "PDF";
    static final Set<String> FUNDOS = new HashSet<>(Arrays.asList("BRANCO", "PAUTADO", "QUADRICULADO", "PONTILHADO"));
    static final String VAZIO = "{\"itens\":[]}";
    static final String MSG_CONFLITO = "A página mudou em outro aparelho. Recarregue para ver a versão mais nova.";

    private final BlocoNotasRepository repository;
    private final AutorizacaoService autorizacao;
    private final ArmazenamentoService armazenamento;
    private final ObjectMapper mapper;

    // ---------- cadernos ----------

    @Override
    @Transactional(readOnly = true)
    public List<BlocoCadernoDto> cadernos() {
        return repository.cadernos(eu().getId());
    }

    @Override
    @Transactional(readOnly = true)
    public BlocoCadernoDto abrir(Long id) {
        return comPaginas(caderno(id, eu()), eu());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BlocoCadernoDto criar(BlocoCadernoForm form) {
        UsuarioAutenticado u = eu();
        if (repository.contarCadernos(u.getId()) >= MAX_CADERNOS) {
            throw new NegocioException("Você chegou ao limite de " + MAX_CADERNOS + " cadernos. Exclua algum para criar outro.");
        }
        Long id = repository.inserirCaderno(u.getId(), form.getNmTitulo().trim(), cor(form.getCdCor()));
        repository.inserirPagina(id, 1, fundo(form.getCdFundo()), LARGURA_A4, ALTURA_A4, null, VAZIO);
        return comPaginas(caderno(id, u), u);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BlocoCadernoDto alterar(Long id, BlocoCadernoForm form) {
        UsuarioAutenticado u = eu();
        if (form.getNrVersao() == null) throw new EntradaInvalidaException("Informe a versão (nrVersao)");
        caderno(id, u);
        if (repository.alterarCaderno(id, u.getId(), form.getNmTitulo().trim(), cor(form.getCdCor()), form.getNrVersao()) == 0) {
            throw new ConflitoException("O caderno mudou em outro aparelho. Recarregue e tente de novo.");
        }
        return caderno(id, u);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void excluir(Long id) {
        UsuarioAutenticado u = eu();
        caderno(id, u);
        List<Long> arquivos = repository.arquivosDoCaderno(id);
        repository.excluirCaderno(id, u.getId());
        arquivos.forEach(armazenamento::apagar);
    }

    // ---------- páginas ----------

    @Override
    @Transactional(readOnly = true)
    public BlocoPaginaDto pagina(Long id) {
        UsuarioAutenticado u = eu();
        BlocoPaginaDto p = paginaDoDono(id, u);
        p.setConteudo(ler(repository.conteudo(id, u.getId()).orElse(VAZIO)));
        return p;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BlocoPaginaDto novaPagina(Long idCaderno, BlocoPaginaForm form) {
        UsuarioAutenticado u = eu();
        caderno(idCaderno, u);
        List<BlocoPaginaDto> paginas = cabeNaoMais(idCaderno, u);
        int ordem = posicao(form.getNrPosicao(), paginas.size());
        Long id;
        if (form.getIdCopiarDe() != null) {
            BlocoPaginaDto origem = paginaDoDono(form.getIdCopiarDe(), u);
            if (!idCaderno.equals(origem.getIdCaderno())) throw new EntradaInvalidaException("A página copiada é de outro caderno");
            // O fundo de PDF é copiado: apagar uma das páginas não pode tirar o fundo da outra.
            Long arquivo = null;
            if (origem.getIdArquivoFundo() != null) {
                ArquivoConteudo c = armazenamento.abrir(origem.getIdArquivoFundo());
                arquivo = armazenamento.salvar(c.getNome(), c.getContentType(), c.getBytes());
            }
            id = repository.inserirPagina(idCaderno, ordem, origem.getCdFundo(), origem.getNrLargura(), origem.getNrAltura(), arquivo,
                    repository.conteudo(origem.getId(), u.getId()).orElse(VAZIO));
        } else {
            int largura = form.getNrLargura() == null ? LARGURA_A4 : form.getNrLargura();
            int altura = form.getNrAltura() == null ? ALTURA_A4 : form.getNrAltura();
            id = repository.inserirPagina(idCaderno, ordem, fundo(form.getCdFundo()), largura, altura, null, VAZIO);
        }
        repository.tocarCaderno(idCaderno);
        return paginaDoDono(id, u);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BlocoPaginaDto paginaDePdf(Long idCaderno, Integer posicao, int largura, int altura, String nome, byte[] bytes) {
        UsuarioAutenticado u = eu();
        caderno(idCaderno, u);
        if (largura < 100 || largura > 4000 || altura < 100 || altura > 4000) throw new EntradaInvalidaException("Tamanho de página inválido");
        List<BlocoPaginaDto> paginas = cabeNaoMais(idCaderno, u);
        String tipo = ArquivoValidador.validarImagem(nome, bytes);
        Long arquivo = armazenamento.salvar(nome, tipo, bytes);
        Long id = repository.inserirPagina(idCaderno, posicao(posicao, paginas.size()), PDF, largura, altura, arquivo, VAZIO);
        repository.tocarCaderno(idCaderno);
        return paginaDoDono(id, u);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BlocoPaginaDto salvar(Long idPagina, BlocoConteudoForm form) {
        UsuarioAutenticado u = eu();
        BlocoPaginaDto p = paginaDoDono(idPagina, u);
        JsonNode c = form.getConteudo();
        if (c == null || !c.isObject() || !c.path("itens").isArray()) throw new EntradaInvalidaException("Conteúdo da página inválido");
        String texto;
        try {
            texto = mapper.writeValueAsString(c);
        } catch (JsonProcessingException e) {
            throw new EntradaInvalidaException("Conteúdo da página inválido", e);
        }
        if (texto.length() > MAX_CONTEUDO) {
            throw new NegocioException("A página ficou grande demais. Continue numa página nova.");
        }
        // Página de PDF continua PDF; as outras trocam entre os fundos de papel.
        String fundo = PDF;
        if (p.getIdArquivoFundo() == null) fundo = form.getCdFundo() == null ? p.getCdFundo() : fundo(form.getCdFundo());
        if (repository.salvarConteudo(idPagina, texto, fundo, form.getNrVersao()) == 0) throw new ConflitoException(MSG_CONFLITO);
        repository.tocarCaderno(p.getIdCaderno());
        return paginaDoDono(idPagina, u);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BlocoCadernoDto reordenar(Long idCaderno, List<Long> ids) {
        UsuarioAutenticado u = eu();
        BlocoCadernoDto c = caderno(idCaderno, u);
        Set<Long> atuais = repository.paginas(idCaderno, u.getId()).stream().map(BlocoPaginaDto::getId).collect(Collectors.toSet());
        if (ids.size() != atuais.size() || !new HashSet<>(ids).equals(atuais)) {
            throw new EntradaInvalidaException("A lista deve ter todas as páginas do caderno, uma vez cada");
        }
        repository.reordenar(idCaderno, ids);
        repository.tocarCaderno(idCaderno);
        return comPaginas(c, u);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BlocoCadernoDto excluirPagina(Long idPagina) {
        UsuarioAutenticado u = eu();
        BlocoPaginaDto p = paginaDoDono(idPagina, u);
        List<BlocoPaginaDto> paginas = repository.paginas(p.getIdCaderno(), u.getId());
        if (paginas.size() <= 1) throw new NegocioException("O caderno precisa de pelo menos uma página");
        repository.excluirPagina(idPagina);
        // Renumera as que ficaram (1, 2, 3…) para as posições baterem com a tela.
        repository.reordenar(p.getIdCaderno(), paginas.stream().map(BlocoPaginaDto::getId).filter(id -> !id.equals(idPagina)).collect(Collectors.toList()));
        if (p.getIdArquivoFundo() != null) armazenamento.apagar(p.getIdArquivoFundo());
        repository.tocarCaderno(p.getIdCaderno());
        return comPaginas(caderno(p.getIdCaderno(), u), u);
    }

    // ---------- imagens ----------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long imagem(Long idCaderno, String nome, byte[] bytes) {
        UsuarioAutenticado u = eu();
        caderno(idCaderno, u);
        String tipo = ArquivoValidador.validarImagem(nome, bytes);
        return repository.inserirImagem(idCaderno, armazenamento.salvar(nome, tipo, bytes));
    }

    @Override
    @Transactional(readOnly = true)
    public ArquivoDownload baixarImagem(Long idImagem) {
        Long arquivo = repository.arquivoDaImagem(idImagem, eu().getId()).orElseThrow(() -> new NaoEncontradoException("Imagem não encontrada"));
        return new ArquivoDownload(armazenamento.abrir(arquivo), arquivo);
    }

    @Override
    @Transactional(readOnly = true)
    public ArquivoDownload baixarFundo(Long idPagina) {
        BlocoPaginaDto p = paginaDoDono(idPagina, eu());
        if (p.getIdArquivoFundo() == null) throw new NaoEncontradoException("A página não tem fundo de PDF");
        return new ArquivoDownload(armazenamento.abrir(p.getIdArquivoFundo()), p.getIdArquivoFundo());
    }

    // ---------- apoio ----------

    private UsuarioAutenticado eu() {
        return autorizacao.exigirAutenticado();
    }

    private BlocoCadernoDto caderno(Long id, UsuarioAutenticado u) {
        return repository.caderno(id, u.getId()).orElseThrow(() -> new NaoEncontradoException("Caderno não encontrado"));
    }

    private BlocoPaginaDto paginaDoDono(Long id, UsuarioAutenticado u) {
        return repository.pagina(id, u.getId()).orElseThrow(() -> new NaoEncontradoException("Página não encontrada"));
    }

    private BlocoCadernoDto comPaginas(BlocoCadernoDto c, UsuarioAutenticado u) {
        c.setPaginas(repository.paginas(c.getId(), u.getId()));
        return c;
    }

    private List<BlocoPaginaDto> cabeNaoMais(Long idCaderno, UsuarioAutenticado u) {
        List<BlocoPaginaDto> paginas = repository.paginas(idCaderno, u.getId());
        if (paginas.size() >= MAX_PAGINAS) throw new NegocioException("O caderno chegou ao limite de " + MAX_PAGINAS + " páginas. Comece outro caderno.");
        return paginas;
    }

    /** Posição pedida (1 = primeira) limitada ao fim; vazia = no fim. */
    static int posicao(Integer pedida, int total) {
        return pedida == null ? total + 1 : Math.max(1, Math.min(pedida, total + 1));
    }

    static String fundo(String f) {
        if (f == null || f.trim().isEmpty()) return "PAUTADO";
        String v = f.trim().toUpperCase();
        if (!FUNDOS.contains(v)) throw new EntradaInvalidaException("Fundo de página inválido");
        return v;
    }

    static String cor(String c) {
        return c == null || c.trim().isEmpty() ? "#2563eb" : c.trim().toLowerCase();
    }

    private JsonNode ler(String texto) {
        try {
            return mapper.readTree(texto);
        } catch (JsonProcessingException e) {
            return mapper.createObjectNode().set("itens", mapper.createArrayNode());
        }
    }
}
