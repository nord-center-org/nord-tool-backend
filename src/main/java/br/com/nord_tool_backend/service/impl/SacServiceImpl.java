package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.dto.SacChamadoDto;
import br.com.nord_tool_backend.dto.SacImagemDto;
import br.com.nord_tool_backend.exception.AcessoNegadoException;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.exception.NaoEncontradoException;
import br.com.nord_tool_backend.exception.NegocioException;
import br.com.nord_tool_backend.form.SacRespostaForm;
import br.com.nord_tool_backend.repository.SacRepository;
import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.security.UsuarioAutenticado;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.service.SacService;
import br.com.nord_tool_backend.storage.ArmazenamentoService;
import br.com.nord_tool_backend.storage.ArquivoDownload;
import br.com.nord_tool_backend.storage.ArquivoValidador;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SAC: qualquer pessoa logada abre chamados (título, descrição e até {@value #MAX_IMAGENS} prints) e vê os dela;
 * quem tem ADMIN no módulo SAC (os administradores, pelo curinga) vê todos, responde e muda o status.
 * Quem abriu pode excluir o chamado enquanto ele está aberto.
 */
@Service
@RequiredArgsConstructor
public class SacServiceImpl implements SacService {

    static final int MAX_IMAGENS = 6;
    static final int MAX_TITULO = 120;
    static final int MAX_DESCRICAO = 4000;
    static final int MAX_TELA = 200;

    private final SacRepository repository;
    private final AutorizacaoService autorizacao;
    private final ArmazenamentoService armazenamento;

    @Override
    @Transactional(readOnly = true)
    public List<SacChamadoDto> listar(boolean todos) {
        UsuarioAutenticado u = todos ? autorizacao.exigir(Modulo.SAC, Acao.ADMIN) : autorizacao.exigirAutenticado();
        List<SacChamadoDto> lista = repository.listar(todos ? null : u.getId());
        completarImagens(lista);
        return lista;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SacChamadoDto abrir(String nmTitulo, String txDescricao, String txTela, List<Anexo> anexos) {
        UsuarioAutenticado u = autorizacao.exigirAutenticado();
        String titulo = obrigatorio(nmTitulo, "Escreva um título para o chamado", MAX_TITULO, "O título");
        String descricao = obrigatorio(txDescricao, "Descreva o que precisa mudar", MAX_DESCRICAO, "A descrição");
        String tela = texto(txTela);
        if (tela != null && tela.length() > MAX_TELA) tela = tela.substring(0, MAX_TELA);
        if (anexos.size() > MAX_IMAGENS) throw new EntradaInvalidaException("Envie no máximo " + MAX_IMAGENS + " imagens");
        Long id = repository.inserir(u.getId(), titulo, descricao, tela);
        int ordem = 1;
        for (Anexo a : anexos) {
            String tipo = ArquivoValidador.validarImagem(a.nome, a.bytes);
            Long idArquivo = armazenamento.salvar(a.nome, tipo, a.bytes);
            repository.inserirImagem(id, idArquivo, ordem++);
        }
        return chamado(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SacChamadoDto responder(Long id, SacRespostaForm form) {
        UsuarioAutenticado u = autorizacao.exigir(Modulo.SAC, Acao.ADMIN);
        String resposta = texto(form.getTxResposta());
        if (resposta != null && resposta.length() > MAX_DESCRICAO) throw new EntradaInvalidaException("A resposta deve ter no máximo " + MAX_DESCRICAO + " caracteres");
        if (repository.responder(id, form.getCdStatus(), resposta, u.getId()) == 0) throw new NaoEncontradoException("Chamado não encontrado");
        return chamado(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void excluir(Long id) {
        UsuarioAutenticado u = autorizacao.exigirAutenticado();
        SacChamadoDto c = repository.buscar(id).orElseThrow(() -> new NaoEncontradoException("Chamado não encontrado"));
        boolean admin = u.acao(Modulo.SAC).atende(Acao.ADMIN);
        if (!admin) {
            if (!c.getIdUsuario().equals(u.getId())) throw new NaoEncontradoException("Chamado não encontrado");
            if (!"ABERTO".equals(c.getCdStatus())) throw new NegocioException("Só dá para excluir um chamado que ainda não foi atendido");
        }
        List<Long> arquivos = repository.arquivos(id);
        repository.excluir(id);
        arquivos.forEach(armazenamento::apagar);
    }

    @Override
    @Transactional(readOnly = true)
    public ArquivoDownload baixarImagem(Long idImagem) {
        UsuarioAutenticado u = autorizacao.exigirAutenticado();
        SacImagemDto i = repository.imagem(idImagem).orElseThrow(() -> new NaoEncontradoException("Imagem não encontrada"));
        if (!i.getIdUsuario().equals(u.getId()) && !u.acao(Modulo.SAC).atende(Acao.ADMIN)) {
            throw new AcessoNegadoException("Você não tem permissão para esta ação");
        }
        // O arquivo da imagem nunca muda: o id serve de versão para o cache.
        return new ArquivoDownload(armazenamento.abrir(i.getIdArquivo()), i.getIdArquivo());
    }

    private SacChamadoDto chamado(Long id) {
        SacChamadoDto c = repository.buscar(id).orElseThrow(() -> new NaoEncontradoException("Chamado não encontrado"));
        completarImagens(java.util.Collections.singletonList(c));
        return c;
    }

    private void completarImagens(List<SacChamadoDto> lista) {
        Map<Long, List<Long>> porChamado = repository.imagens(lista.stream().map(SacChamadoDto::getId).collect(Collectors.toList())).stream()
                .collect(Collectors.groupingBy(SacImagemDto::getIdChamado, Collectors.mapping(SacImagemDto::getId, Collectors.toList())));
        lista.forEach(c -> c.setImagens(porChamado.getOrDefault(c.getId(), new java.util.ArrayList<>())));
    }

    private static String texto(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String obrigatorio(String s, String vazio, int max, String rotulo) {
        String t = texto(s);
        if (t == null) throw new EntradaInvalidaException(vazio);
        if (t.length() > max) throw new EntradaInvalidaException(rotulo + " deve ter no máximo " + max + " caracteres");
        return t;
    }
}
