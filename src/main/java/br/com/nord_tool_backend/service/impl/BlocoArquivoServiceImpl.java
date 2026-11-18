package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.dto.BlocoArquivoDto;
import br.com.nord_tool_backend.exception.ConflitoException;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.exception.NaoEncontradoException;
import br.com.nord_tool_backend.exception.NegocioException;
import br.com.nord_tool_backend.form.BlocoArquivoForm;
import br.com.nord_tool_backend.repository.BlocoArquivoRepository;
import br.com.nord_tool_backend.security.UsuarioAutenticado;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.service.BlocoArquivoService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Planilhas (estilo Google Sheets) e documentos (estilo Word) avulsos do bloco de notas. Cada pessoa só vê os seus.
 * A planilha é o JSON das abas do editor; o documento é HTML, limpo na tela e conferido aqui como no documento do caderno.
 */
@Service
@RequiredArgsConstructor
public class BlocoArquivoServiceImpl implements BlocoArquivoService {

    static final String PLANILHA = "PLANILHA";
    static final String DOCUMENTO = "DOCUMENTO";
    static final int MAX_ARQUIVOS = 500;
    static final int MAX_PLANILHA = 5_000_000;
    static final int MAX_DOCUMENTO = 2_000_000;
    static final String COR_PLANILHA = "#16a34a";
    static final String COR_DOCUMENTO = "#2563eb";

    private final BlocoArquivoRepository repository;
    private final AutorizacaoService autorizacao;
    private final ObjectMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<BlocoArquivoDto> listar() {
        return repository.listar(eu().getId());
    }

    @Override
    @Transactional(readOnly = true)
    public BlocoArquivoDto abrir(Long id) {
        return buscar(id, eu());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BlocoArquivoDto criar(BlocoArquivoForm form) {
        UsuarioAutenticado u = euEditando();
        String tipo = form.getCdTipo();
        if (!PLANILHA.equals(tipo) && !DOCUMENTO.equals(tipo)) throw new EntradaInvalidaException("Tipo de arquivo inválido");
        if (repository.contar(u.getId()) >= MAX_ARQUIVOS) {
            throw new NegocioException("Você chegou ao limite de " + MAX_ARQUIVOS + " planilhas e documentos. Exclua algum para criar outro.");
        }
        String conteudo = form.getConteudo() == null ? "" : conteudo(tipo, form.getConteudo());
        String cor = form.getCdCor() != null ? form.getCdCor() : PLANILHA.equals(tipo) ? COR_PLANILHA : COR_DOCUMENTO;
        Long id = repository.inserir(u.getId(), tipo, titulo(form.getNmTitulo()), cor, conteudo);
        return buscar(id, u);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BlocoArquivoDto salvar(Long id, BlocoArquivoForm form) {
        UsuarioAutenticado u = euEditando();
        if (form.getNrVersao() == null) throw new EntradaInvalidaException("Informe a versão (nrVersao)");
        BlocoArquivoDto atual = buscar(id, u);
        String titulo = form.getNmTitulo() == null ? atual.getNmTitulo() : titulo(form.getNmTitulo());
        String cor = form.getCdCor() == null ? atual.getCdCor() : form.getCdCor();
        String conteudo = form.getConteudo() == null ? null : conteudo(atual.getCdTipo(), form.getConteudo());
        if (repository.alterar(id, u.getId(), titulo, cor, conteudo, form.getNrVersao()) == 0) {
            throw new ConflitoException("O arquivo mudou em outro aparelho. Recarregue para ver a versão mais nova.");
        }
        BlocoArquivoDto salvo = buscar(id, u);
        // A tela já tem o conteúdo: volta só a versão nova (planilhas podem ter megabytes).
        salvo.setConteudo(null);
        return salvo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void excluir(Long id) {
        UsuarioAutenticado u = euEditando();
        buscar(id, u);
        repository.excluir(id, u.getId());
    }

    /** Ver: o módulo BLOCO_NOTAS com LEITURA. */
    private UsuarioAutenticado eu() {
        return autorizacao.exigir(Modulo.BLOCO_NOTAS, Acao.LEITURA);
    }

    /** Criar, alterar e excluir: o módulo BLOCO_NOTAS com ESCRITA. */
    private UsuarioAutenticado euEditando() {
        return autorizacao.exigir(Modulo.BLOCO_NOTAS, Acao.ESCRITA);
    }

    private BlocoArquivoDto buscar(Long id, UsuarioAutenticado u) {
        return repository.buscar(id, u.getId()).orElseThrow(() -> new NaoEncontradoException("Arquivo não encontrado"));
    }

    private static String titulo(String t) {
        String s = t == null ? "" : t.trim();
        if (s.isEmpty()) throw new EntradaInvalidaException("Dê um nome ao arquivo");
        return s;
    }

    /** Planilha: JSON (lista de abas ou objeto) até 5 MB. Documento: HTML até 2 MB, sem script nem eventos. */
    String conteudo(String tipo, String conteudo) {
        int bytes = conteudo.getBytes(StandardCharsets.UTF_8).length;
        if (PLANILHA.equals(tipo)) {
            if (bytes > MAX_PLANILHA) throw new EntradaInvalidaException("A planilha passou do tamanho máximo (5 MB)");
            if (conteudo.isEmpty()) return conteudo;
            try {
                JsonNode n = mapper.readTree(conteudo);
                if (n == null || !(n.isArray() || n.isObject())) throw new EntradaInvalidaException("Planilha inválida");
            } catch (JsonProcessingException e) {
                throw new EntradaInvalidaException("Planilha inválida", e);
            }
            return conteudo;
        }
        if (bytes > MAX_DOCUMENTO) throw new EntradaInvalidaException("O documento passou do tamanho máximo (2 MB)");
        if (BlocoNotasServiceImpl.PERIGOSO.matcher(conteudo).find()) throw new EntradaInvalidaException("O documento tem conteúdo que não é permitido");
        return conteudo;
    }
}
