package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.dto.RepertorioMusicaDto;
import br.com.nord_tool_backend.dto.RepertorioProjetoDto;
import br.com.nord_tool_backend.dto.RepertorioUsoDto;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.exception.NaoEncontradoException;
import br.com.nord_tool_backend.exception.NegocioException;
import br.com.nord_tool_backend.form.RepertorioMusicaForm;
import br.com.nord_tool_backend.form.RepertorioProjetoForm;
import br.com.nord_tool_backend.repository.RepertorioRepository;
import br.com.nord_tool_backend.security.UsuarioAutenticado;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.service.RepertorioService;
import br.com.nord_tool_backend.storage.ArmazenamentoService;
import br.com.nord_tool_backend.storage.ArquivoDownload;
import br.com.nord_tool_backend.storage.ArquivoValidador;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Repertório v7: as músicas (dados + áudio) e as criações do estúdio na conta, para abrir em qualquer aparelho. O
 * aparelho dá a chave (m.../p...) e continua com a cópia local; aqui ficam os limites: áudio de até 15 MB conferido pelo
 * conteúdo, espaço total por pessoa (configurável, padrão 300 MB), até 300 músicas e 200 criações, e os dados em JSON.
 */
@Service
public class RepertorioServiceImpl implements RepertorioService {

    static final Pattern CHAVE = Pattern.compile("^[A-Za-z0-9_-]{1,40}$");
    static final int MAX_MUSICAS = 300;
    static final int MAX_PROJETOS = 200;
    static final int MAX_AUDIO = 15 * 1024 * 1024;
    static final int MAX_DADOS_MUSICA = 3_000_000;
    static final int MAX_DADOS_PROJETO = 2_000_000;
    static final long MB = 1024L * 1024L;

    private final RepertorioRepository repository;
    private final AutorizacaoService autorizacao;
    private final ArmazenamentoService armazenamento;
    private final ObjectMapper mapper;
    private final long cotaBytes;

    public RepertorioServiceImpl(RepertorioRepository repository, AutorizacaoService autorizacao, ArmazenamentoService armazenamento,
                                 ObjectMapper mapper, @Value("${nord-tool.repertorio.cota-mb:300}") long cotaMb) {
        this.repository = repository;
        this.autorizacao = autorizacao;
        this.armazenamento = armazenamento;
        this.mapper = mapper;
        this.cotaBytes = Math.max(0, cotaMb) * MB;
    }

    // ---------------------------------------------------------------- músicas

    @Override
    @Transactional(readOnly = true)
    public List<RepertorioMusicaDto> listarMusicas() {
        return repository.listarMusicas(eu().getId());
    }

    @Override
    @Transactional(readOnly = true)
    public RepertorioMusicaDto abrirMusica(String chave) {
        return musica(eu().getId(), chave(chave));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RepertorioMusicaDto salvarMusica(String chave, RepertorioMusicaForm form) {
        Long u = eu().getId();
        String c = chave(chave);
        if (repository.buscarMusica(u, c).isEmpty() && repository.contarMusicas(u) >= MAX_MUSICAS) {
            throw new NegocioException("Você chegou ao limite de " + MAX_MUSICAS + " músicas na conta. Exclua alguma para guardar outra.");
        }
        RepertorioMusicaDto d = new RepertorioMusicaDto();
        d.setNmTitulo(texto(form.getNmTitulo(), 200, "Música"));
        d.setNmArtista(texto(form.getNmArtista(), 200, ""));
        d.setNmArquivo(texto(form.getNmArquivo(), 180, ""));
        d.setNrDuracao(faixa(form.getNrDuracao(), BigDecimal.ZERO, new BigDecimal("86400"), "Duração inválida", BigDecimal.ZERO));
        d.setNmTom(form.getNmTom() == null || form.getNmTom().trim().isEmpty() ? null : texto(form.getNmTom(), 12, null));
        d.setNrBpm(faixa(form.getNrBpm(), BigDecimal.ZERO, new BigDecimal("400"), "BPM inválido", null));
        d.setCdProjeto(form.getCdProjeto() == null || form.getCdProjeto().isEmpty() ? null : chave(form.getCdProjeto()));
        d.setTxDados(json(form.getTxDados(), MAX_DADOS_MUSICA, "Os dados da música passaram do tamanho máximo (3 MB)"));
        repository.salvarMusica(u, c, d);
        RepertorioMusicaDto salvo = musica(u, c);
        // a tela já tem os dados: volta só o resumo
        salvo.setTxDados(null);
        return salvo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RepertorioMusicaDto enviarAudio(String chave, String nome, byte[] bytes) {
        Long u = eu().getId();
        String c = chave(chave);
        RepertorioMusicaDto atual = musica(u, c);
        ArquivoValidador.validarNome(nome);
        if (bytes == null || bytes.length == 0) throw new EntradaInvalidaException("O áudio está vazio");
        if (bytes.length > MAX_AUDIO) throw new EntradaInvalidaException("O áudio deve ter no máximo 15 MB para ir para a conta");
        String tipo = tipoDoAudio(bytes);
        long usados = repository.bytesUsados(u, c);
        if (usados + bytes.length > cotaBytes) {
            throw new NegocioException("O espaço do Repertório na conta acabou (" + (cotaBytes / MB) + " MB). Exclua alguma música para guardar outra.");
        }
        Long novo = armazenamento.salvar(nome, tipo, bytes);
        repository.ligarAudio(u, c, novo, nome, bytes.length);
        if (atual.getIdArquivo() != null) armazenamento.apagar(atual.getIdArquivo());
        RepertorioMusicaDto salvo = musica(u, c);
        salvo.setTxDados(null);
        return salvo;
    }

    @Override
    @Transactional(readOnly = true)
    public ArquivoDownload baixarAudio(String chave) {
        RepertorioMusicaDto m = musica(eu().getId(), chave(chave));
        if (m.getIdArquivo() == null) throw new NaoEncontradoException("O áudio dessa música não está na conta");
        return new ArquivoDownload(armazenamento.abrir(m.getIdArquivo()), m.getIdArquivo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void excluirMusica(String chave) {
        Long u = eu().getId();
        String c = chave(chave);
        RepertorioMusicaDto m = repository.buscarMusica(u, c).orElse(null);
        if (m == null) return;
        repository.excluirMusica(u, c);
        if (m.getIdArquivo() != null) armazenamento.apagar(m.getIdArquivo());
    }

    @Override
    @Transactional(readOnly = true)
    public RepertorioUsoDto uso() {
        Long u = eu().getId();
        return new RepertorioUsoDto(repository.bytesUsados(u, null), cotaBytes, repository.contarMusicas(u), MAX_MUSICAS, MAX_AUDIO);
    }

    // ---------------------------------------------------------------- criações do estúdio

    @Override
    @Transactional(readOnly = true)
    public List<RepertorioProjetoDto> listarProjetos() {
        return repository.listarProjetos(eu().getId());
    }

    @Override
    @Transactional(readOnly = true)
    public RepertorioProjetoDto abrirProjeto(String chave) {
        return projeto(eu().getId(), chave(chave));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RepertorioProjetoDto salvarProjeto(String chave, RepertorioProjetoForm form) {
        Long u = eu().getId();
        String c = chave(chave);
        if (repository.buscarProjeto(u, c).isEmpty() && repository.contarProjetos(u) >= MAX_PROJETOS) {
            throw new NegocioException("Você chegou ao limite de " + MAX_PROJETOS + " criações na conta. Exclua alguma para guardar outra.");
        }
        String dados = json(form.getTxDados(), MAX_DADOS_PROJETO, "A criação passou do tamanho máximo (2 MB)");
        repository.salvarProjeto(u, c, texto(form.getNmTitulo(), 80, "Música nova"), dados);
        RepertorioProjetoDto salvo = projeto(u, c);
        salvo.setTxDados(null);
        return salvo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void excluirProjeto(String chave) {
        repository.excluirProjeto(eu().getId(), chave(chave));
    }

    // ---------------------------------------------------------------- apoio

    private UsuarioAutenticado eu() {
        return autorizacao.exigirAutenticado();
    }

    private RepertorioMusicaDto musica(Long u, String c) {
        return repository.buscarMusica(u, c).orElseThrow(() -> new NaoEncontradoException("Música não encontrada"));
    }

    private RepertorioProjetoDto projeto(Long u, String c) {
        return repository.buscarProjeto(u, c).orElseThrow(() -> new NaoEncontradoException("Criação não encontrada"));
    }

    static String chave(String c) {
        if (c == null || !CHAVE.matcher(c).matches()) throw new EntradaInvalidaException("Identificador inválido");
        return c;
    }

    /** Texto aparado e cortado no máximo; vazio vira o padrão (sem caracteres de controle). */
    static String texto(String t, int max, String padrao) {
        String s = t == null ? "" : t.replaceAll("\\p{Cntrl}", " ").trim();
        if (s.isEmpty()) return padrao;
        return s.length() > max ? s.substring(0, max) : s;
    }

    static BigDecimal faixa(BigDecimal v, BigDecimal min, BigDecimal max, String erro, BigDecimal padrao) {
        if (v == null) return padrao;
        if (v.compareTo(min) < 0 || v.compareTo(max) > 0) throw new EntradaInvalidaException(erro);
        return v;
    }

    /** JSON objeto, até `max` bytes; vazio vira "{}". */
    String json(String dados, int max, String erroTamanho) {
        if (dados == null || dados.trim().isEmpty()) return "{}";
        if (dados.getBytes(StandardCharsets.UTF_8).length > max) throw new EntradaInvalidaException(erroTamanho);
        try {
            JsonNode n = mapper.readTree(dados);
            if (n == null || !n.isObject()) throw new EntradaInvalidaException("Dados inválidos");
        } catch (JsonProcessingException e) {
            throw new EntradaInvalidaException("Dados inválidos", e);
        }
        return dados;
    }

    /** Tipo do áudio pelo conteúdo (MP3, AAC, WAV, OGG/Opus, FLAC, M4A, WebM); outro conteúdo é recusado. */
    static String tipoDoAudio(byte[] b) {
        if (comeca(b, 0, 'I', 'D', '3')) return "audio/mpeg";
        if (b.length >= 2 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xE0) == 0xE0) return (b[1] & 0x06) == 0 ? "audio/aac" : "audio/mpeg";
        if (comeca(b, 0, 'R', 'I', 'F', 'F') && comeca(b, 8, 'W', 'A', 'V', 'E')) return "audio/wav";
        if (comeca(b, 0, 'O', 'g', 'g', 'S')) return "audio/ogg";
        if (comeca(b, 0, 'f', 'L', 'a', 'C')) return "audio/flac";
        if (comeca(b, 4, 'f', 't', 'y', 'p')) return "audio/mp4";
        if (comeca(b, 0, 0x1A, 0x45, 0xDF, 0xA3)) return "audio/webm";
        throw new EntradaInvalidaException("O arquivo enviado não é um áudio suportado (MP3, WAV, OGG, FLAC, M4A)");
    }

    private static boolean comeca(byte[] b, int de, int... esperado) {
        if (b.length < de + esperado.length) return false;
        for (int i = 0; i < esperado.length; i++) if ((b[de + i] & 0xFF) != esperado[i]) return false;
        return true;
    }
}
