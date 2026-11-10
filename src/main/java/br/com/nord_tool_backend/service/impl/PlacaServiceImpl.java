package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.dto.PlacaDto;
import br.com.nord_tool_backend.dto.PlacaModeloDto;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.exception.NaoEncontradoException;
import br.com.nord_tool_backend.form.PlacaForm;
import br.com.nord_tool_backend.form.PlacaModeloForm;
import br.com.nord_tool_backend.repository.PlacaRepository;
import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.security.UsuarioAutenticado;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.service.ContextoFaseService;
import br.com.nord_tool_backend.service.PlacaService;
import br.com.nord_tool_backend.storage.ArmazenamentoService;
import br.com.nord_tool_backend.storage.ArquivoDownload;
import br.com.nord_tool_backend.storage.ArquivoValidador;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Placas de obra. Os modelos (formato: fundo com marca d'água, logo, cabeçalho, quadro, rodapé) valem para todas as
 * obras e só o ADMIN do módulo PLACAS mantém; as placas são da obra escolhida (ESCRITA cria e altera, LEITURA vê).
 * O formato e os textos são JSON do frontend: o servidor só confere que é um objeto e o tamanho.
 */
@Service
@RequiredArgsConstructor
public class PlacaServiceImpl implements PlacaService {

    static final int MAX_JSON = 20_000;

    private final PlacaRepository repository;
    private final AutorizacaoService autorizacao;
    private final ContextoFaseService contexto;
    private final ArmazenamentoService armazenamento;
    private final ObjectMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<PlacaModeloDto> modelos() {
        autorizacao.exigir(Modulo.PLACAS, Acao.LEITURA);
        List<PlacaModeloDto> lista = repository.modelos();
        lista.forEach(this::lerConfig);
        return lista;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlacaModeloDto criarModelo(PlacaModeloForm form) {
        UsuarioAutenticado u = autorizacao.exigir(Modulo.PLACAS, Acao.ADMIN);
        Long id = repository.inserirModelo(form.getNmModelo().trim(), json(form.getConfig(), "o formato"), form.getInAtivo() == null || form.getInAtivo(), u.getId());
        return modelo(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlacaModeloDto alterarModelo(Long id, PlacaModeloForm form) {
        UsuarioAutenticado u = autorizacao.exigir(Modulo.PLACAS, Acao.ADMIN);
        PlacaModeloDto atual = modelo(id);
        boolean ativo = form.getInAtivo() == null ? atual.isInAtivo() : form.getInAtivo();
        repository.alterarModelo(id, form.getNmModelo().trim(), json(form.getConfig(), "o formato"), ativo, u.getId());
        return modelo(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void excluirModelo(Long id) {
        UsuarioAutenticado u = autorizacao.exigir(Modulo.PLACAS, Acao.ADMIN);
        PlacaModeloDto m = modelo(id);
        // Modelo com placas não some (as placas dependem dele): só deixa de aparecer para placas novas.
        if (repository.placasDoModelo(id) > 0) {
            repository.alterarModelo(id, m.getNmModelo(), m.getTxConfig(), false, u.getId());
            return;
        }
        repository.excluirModelo(id);
        if (m.getIdArquivoFundo() != null) armazenamento.apagar(m.getIdArquivoFundo());
        if (m.getIdArquivoLogo() != null) armazenamento.apagar(m.getIdArquivoLogo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlacaModeloDto salvarImagem(Long idModelo, String tipo, String nome, byte[] bytes) {
        UsuarioAutenticado u = autorizacao.exigir(Modulo.PLACAS, Acao.ADMIN);
        boolean fundo = ehFundo(tipo);
        PlacaModeloDto m = modelo(idModelo);
        String contentType = ArquivoValidador.validarImagem(nome, bytes);
        Long novo = armazenamento.salvar(nome, contentType, bytes);
        repository.trocarImagem(idModelo, fundo, novo, u.getId());
        Long antigo = fundo ? m.getIdArquivoFundo() : m.getIdArquivoLogo();
        if (antigo != null) armazenamento.apagar(antigo);
        return modelo(idModelo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlacaModeloDto removerImagem(Long idModelo, String tipo) {
        UsuarioAutenticado u = autorizacao.exigir(Modulo.PLACAS, Acao.ADMIN);
        boolean fundo = ehFundo(tipo);
        PlacaModeloDto m = modelo(idModelo);
        repository.trocarImagem(idModelo, fundo, null, u.getId());
        Long antigo = fundo ? m.getIdArquivoFundo() : m.getIdArquivoLogo();
        if (antigo != null) armazenamento.apagar(antigo);
        return modelo(idModelo);
    }

    @Override
    @Transactional(readOnly = true)
    public ArquivoDownload baixarImagem(Long idModelo, String tipo) {
        autorizacao.exigir(Modulo.PLACAS, Acao.LEITURA);
        boolean fundo = ehFundo(tipo);
        PlacaModeloDto m = modelo(idModelo);
        Long arquivo = fundo ? m.getIdArquivoFundo() : m.getIdArquivoLogo();
        if (arquivo == null) throw new NaoEncontradoException("O modelo não tem essa imagem");
        return new ArquivoDownload(armazenamento.abrir(arquivo), arquivo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlacaDto> placas() {
        autorizacao.exigir(Modulo.PLACAS, Acao.LEITURA);
        List<PlacaDto> lista = repository.placas(contexto.obraEscolhida());
        lista.forEach(this::lerValores);
        return lista;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlacaDto criar(PlacaForm form) {
        UsuarioAutenticado u = autorizacao.exigir(Modulo.PLACAS, Acao.ESCRITA);
        Long obra = contexto.obraEscolhida();
        modelo(form.getIdModelo());
        Long id = repository.inserirPlaca(obra, form.getIdModelo(), form.getNmTitulo().trim(), json(form.getValores(), "os textos"), u.getId());
        return placa(id, obra);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlacaDto alterar(Long id, PlacaForm form) {
        UsuarioAutenticado u = autorizacao.exigir(Modulo.PLACAS, Acao.ESCRITA);
        Long obra = contexto.obraEscolhida();
        placa(id, obra);
        modelo(form.getIdModelo());
        repository.alterarPlaca(id, form.getIdModelo(), form.getNmTitulo().trim(), json(form.getValores(), "os textos"), u.getId());
        return placa(id, obra);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void excluir(Long id) {
        autorizacao.exigir(Modulo.PLACAS, Acao.ESCRITA);
        placa(id, contexto.obraEscolhida());
        repository.excluirPlaca(id);
    }

    // ---------- v2: imagens soltas ----------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long enviarImagemSolta(String nome, byte[] bytes) {
        UsuarioAutenticado u = autorizacao.exigir(Modulo.PLACAS, Acao.ESCRITA);
        String contentType = ArquivoValidador.validarImagem(nome, bytes);
        return repository.inserirImagem(armazenamento.salvar(nome, contentType, bytes), u.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public ArquivoDownload baixarImagemSolta(Long idImagem) {
        autorizacao.exigir(Modulo.PLACAS, Acao.LEITURA);
        Long arquivo = repository.arquivoDaImagem(idImagem).orElseThrow(() -> new NaoEncontradoException("Imagem não encontrada"));
        return new ArquivoDownload(armazenamento.abrir(arquivo), arquivo);
    }

    private PlacaModeloDto modelo(Long id) {
        PlacaModeloDto m = repository.modelo(id).orElseThrow(() -> new NaoEncontradoException("Modelo de placa não encontrado"));
        lerConfig(m);
        return m;
    }

    /** Placa da obra escolhida (de outra obra é como se não existisse). */
    private PlacaDto placa(Long id, Long obra) {
        PlacaDto p = repository.placa(id).filter(x -> obra.equals(x.getIdObra()))
                .orElseThrow(() -> new NaoEncontradoException("Placa não encontrada"));
        lerValores(p);
        return p;
    }

    private void lerConfig(PlacaModeloDto m) {
        m.setConfig(ler(m.getTxConfig()));
    }

    private void lerValores(PlacaDto p) {
        p.setValores(ler(p.getTxValores()));
    }

    private JsonNode ler(String texto) {
        try {
            return texto == null ? mapper.createObjectNode() : mapper.readTree(texto);
        } catch (JsonProcessingException e) {
            return mapper.createObjectNode();
        }
    }

    private String json(JsonNode no, String rotulo) {
        if (no == null || !no.isObject()) throw new EntradaInvalidaException("Formato inválido em " + rotulo);
        String texto;
        try {
            texto = mapper.writeValueAsString(no);
        } catch (JsonProcessingException e) {
            throw new EntradaInvalidaException("Formato inválido em " + rotulo, e);
        }
        if (texto.length() > MAX_JSON) throw new EntradaInvalidaException("Texto grande demais em " + rotulo);
        return texto;
    }

    private static boolean ehFundo(String tipo) {
        if ("fundo".equals(tipo)) return true;
        if ("logo".equals(tipo)) return false;
        throw new EntradaInvalidaException("Imagem deve ser fundo ou logo");
    }
}
