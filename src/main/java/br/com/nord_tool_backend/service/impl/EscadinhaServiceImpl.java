package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.domain.EscadinhaTorre;
import br.com.nord_tool_backend.dto.EscadinhaDto;
import br.com.nord_tool_backend.dto.EscadinhaPlantaDto;
import br.com.nord_tool_backend.dto.EscadinhaTorreDto;
import br.com.nord_tool_backend.exception.ConflitoException;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.exception.FalhaInternaException;
import br.com.nord_tool_backend.exception.NaoEncontradoException;
import br.com.nord_tool_backend.form.EscadinhaModeloForm;
import br.com.nord_tool_backend.form.EscadinhaTorreForm;
import br.com.nord_tool_backend.repository.EscadinhaRepository;
import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.security.UsuarioAutenticado;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.service.ContextoFaseService;
import br.com.nord_tool_backend.service.EscadinhaService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A escadinha é da obra inteira (não de uma fase): toda operação usa a obra do cabeçalho X-Obra, validada
 * pelo ContextoFaseService. Config e ciclos são documentos JSON do frontend; aqui só conferimos forma e tamanho.
 */
@Service
public class EscadinhaServiceImpl implements EscadinhaService {

    static final int MAX_TORRES = 30;
    static final int MAX_JSON = 400_000;
    static final int MAX_PLANTA = 2_500_000;
    static final String MSG_CONFLITO = "A torre foi alterada por outra pessoa. Recarregue e tente de novo.";
    private static final Pattern PLANTA = Pattern.compile("^data:image/(jpeg|png|webp);base64,[A-Za-z0-9+/]+=*$");

    private final EscadinhaRepository repository;
    private final AutorizacaoService autorizacao;
    private final ContextoFaseService contexto;
    private final ObjectMapper json;
    private final java.time.Clock clock;

    static final int MAX_TIPOLOGIAS = 40;
    private static final java.time.ZoneId FUSO = java.time.ZoneId.of("America/Sao_Paulo");
    private static final Pattern CHAVE = Pattern.compile("^[A-Za-z0-9_.-]{1,64}\\|[AB]\\|[A-Za-z0-9_.-]{1,64}$");
    private static final Pattern DATA = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

    public EscadinhaServiceImpl(EscadinhaRepository repository, AutorizacaoService autorizacao, ContextoFaseService contexto, ObjectMapper json,
                                java.time.Clock clock) {
        this.repository = repository;
        this.autorizacao = autorizacao;
        this.contexto = contexto;
        this.json = json;
        this.clock = clock;
    }

    @Override
    public EscadinhaDto carregar() {
        autorizacao.exigir(Modulo.ESCADINHA, Acao.LEITURA);
        Long obra = contexto.obraEscolhida();
        return montar(obra);
    }

    @Override
    public EscadinhaPlantaDto planta(Long idTorre) {
        autorizacao.exigir(Modulo.ESCADINHA, Acao.LEITURA);
        Long obra = contexto.obraEscolhida();
        torre(obra, idTorre);
        return new EscadinhaPlantaDto(repository.planta(obra, idTorre).orElse(null));
    }

    @Override
    public EscadinhaTorreDto criarTorre(EscadinhaTorreForm form) {
        UsuarioAutenticado u = autorizacao.exigir(Modulo.ESCADINHA, Acao.ESCRITA);
        Long obra = contexto.obraEscolhida();
        int total = repository.contarTorres(obra);
        if (total >= MAX_TORRES) throw new EntradaInvalidaException("A obra já tem o máximo de " + MAX_TORRES + " torres");
        String planta = plantaValida(form.getPlanta());
        int ordem = form.getNrOrdem() != null ? form.getNrOrdem() : total;
        Long id = repository.inserirTorre(obra, nome(form), ordem, documento(form.getConfig(), "configuração"),
                documento(form.getCiclos(), "ciclos"), planta == null || planta.isEmpty() ? null : planta, u.getId());
        return converter(torre(obra, id));
    }

    @Override
    public EscadinhaTorreDto alterarTorre(Long idTorre, EscadinhaTorreForm form) {
        UsuarioAutenticado u = autorizacao.exigir(Modulo.ESCADINHA, Acao.ESCRITA);
        Long obra = contexto.obraEscolhida();
        if (form.getNrVersao() == null) throw new EntradaInvalidaException("Informe a versão da torre (nrVersao)");
        EscadinhaTorre atual = torre(obra, idTorre);
        String planta = plantaValida(form.getPlanta());
        int ordem = form.getNrOrdem() != null ? form.getNrOrdem() : atual.getNrOrdem();
        int linhas = repository.alterarTorre(obra, idTorre, form.getNrVersao(), nome(form), ordem,
                documento(form.getConfig(), "configuração"), documento(form.getCiclos(), "ciclos"),
                planta != null, planta == null || planta.isEmpty() ? null : planta, u.getId());
        if (linhas == 0) throw new ConflitoException(MSG_CONFLITO);
        return converter(torre(obra, idTorre));
    }

    @Override
    public void excluirTorre(Long idTorre) {
        autorizacao.exigir(Modulo.ESCADINHA, Acao.ESCRITA);
        Long obra = contexto.obraEscolhida();
        if (repository.excluirTorre(obra, idTorre) == 0) throw new NaoEncontradoException("Torre não encontrada nesta obra");
    }

    @Override
    public EscadinhaDto salvarModelo(EscadinhaModeloForm form) {
        // Ciclos, atividades, dias e metas valem para a obra inteira: só o admin da escadinha muda.
        UsuarioAutenticado u = autorizacao.exigir(Modulo.ESCADINHA, Acao.ADMIN);
        Long obra = contexto.obraEscolhida();
        repository.gravarModelo(obra, documento(form.getModelo(), "modelo de ciclos"), u.getId());
        return montar(obra);
    }

    @Override
    public br.com.nord_tool_backend.dto.EscadinhaGradeDto marcarCelulas(Long idTorre, br.com.nord_tool_backend.form.EscadinhaCelulasForm form) {
        UsuarioAutenticado u = autorizacao.exigir(Modulo.ESCADINHA, Acao.ESCRITA);
        Long obra = contexto.obraEscolhida();
        torre(obra, idTorre);
        java.time.LocalDateTime agora = java.time.LocalDateTime.now(clock.withZone(FUSO)).withNano(0);
        java.util.List<String[]> validas = new java.util.ArrayList<>();
        for (br.com.nord_tool_backend.form.EscadinhaCelulasForm.Celula c : form.getAlteracoes()) {
            if (c == null || c.getChave() == null || !CHAVE.matcher(c.getChave()).matches()) throw new EntradaInvalidaException("Célula inválida");
            String status = c.getStatus() == null ? "" : c.getStatus().trim().toUpperCase(java.util.Locale.ROOT);
            if (status.isEmpty()) { validas.add(new String[]{c.getChave(), null}); continue; }
            if (!status.equals("E") && !status.equals("X") && !status.equals("N")) throw new EntradaInvalidaException("Status inválido: " + c.getStatus());
            String data = dataValida(c.getData(), agora.toLocalDate());
            com.fasterxml.jackson.databind.node.ObjectNode v = json.createObjectNode();
            v.put("s", status);
            v.put("d", data);
            v.put("u", u.getId());
            v.put("h", agora.toString());
            validas.add(new String[]{c.getChave(), v.toString()});
        }
        for (String[] c : validas) {
            if (repository.gravarCelula(obra, idTorre, c[0], c[1], u.getId()) == 0) throw new NaoEncontradoException("Torre não encontrada nesta obra");
        }
        return new br.com.nord_tool_backend.dto.EscadinhaGradeDto(ler(repository.grade(obra, idTorre).orElse("{}")));
    }

    /** Data da execução: hoje quando não vem; não aceita futuro nem antes de 2000. */
    static String dataValida(String data, java.time.LocalDate hoje) {
        if (data == null || data.isBlank()) return hoje.toString();
        if (!DATA.matcher(data.trim()).matches()) throw new EntradaInvalidaException("Data inválida: " + data);
        java.time.LocalDate d;
        try {
            d = java.time.LocalDate.parse(data.trim());
        } catch (java.time.format.DateTimeParseException e) {
            throw new EntradaInvalidaException("Data inválida: " + data);
        }
        if (d.isAfter(hoje) || d.getYear() < 2000) throw new EntradaInvalidaException("A data da execução não pode ser no futuro");
        return d.toString();
    }

    @Override
    public java.util.List<br.com.nord_tool_backend.dto.EscadinhaTipologiaDto> tipologias(Long idTorre) {
        autorizacao.exigir(Modulo.ESCADINHA, Acao.LEITURA);
        Long obra = contexto.obraEscolhida();
        torre(obra, idTorre);
        return repository.tipologias(obra, idTorre, null);
    }

    @Override
    public EscadinhaPlantaDto imagemTipologia(Long idTipologia) {
        autorizacao.exigir(Modulo.ESCADINHA, Acao.LEITURA);
        Long obra = contexto.obraEscolhida();
        tipologia(obra, idTipologia);
        return new EscadinhaPlantaDto(repository.imagemTipologia(obra, idTipologia).orElse(null));
    }

    @Override
    public br.com.nord_tool_backend.dto.EscadinhaTipologiaDto criarTipologia(Long idTorre, br.com.nord_tool_backend.form.EscadinhaTipologiaForm form) {
        UsuarioAutenticado u = autorizacao.exigir(Modulo.ESCADINHA, Acao.ESCRITA);
        Long obra = contexto.obraEscolhida();
        torre(obra, idTorre);
        String nome = nomeTipologia(form);
        if (repository.tipologias(obra, idTorre, null).size() >= MAX_TIPOLOGIAS) throw new EntradaInvalidaException("A torre já tem o máximo de " + MAX_TIPOLOGIAS + " tipologias");
        if (repository.nomeTipologiaEmUso(idTorre, nome, null)) throw new ConflitoException("Já existe uma tipologia com esse nome nesta torre");
        String imagem = plantaValida(form.getImagem());
        Long id = repository.inserirTipologia(idTorre, nome, imagem == null || imagem.isEmpty() ? null : imagem, u.getId());
        return tipologia(obra, id);
    }

    @Override
    public br.com.nord_tool_backend.dto.EscadinhaTipologiaDto alterarTipologia(Long idTipologia, br.com.nord_tool_backend.form.EscadinhaTipologiaForm form) {
        UsuarioAutenticado u = autorizacao.exigir(Modulo.ESCADINHA, Acao.ESCRITA);
        Long obra = contexto.obraEscolhida();
        br.com.nord_tool_backend.dto.EscadinhaTipologiaDto atual = tipologia(obra, idTipologia);
        String nome = nomeTipologia(form);
        if (repository.nomeTipologiaEmUso(atual.getIdTorre(), nome, idTipologia)) throw new ConflitoException("Já existe uma tipologia com esse nome nesta torre");
        String imagem = plantaValida(form.getImagem());
        repository.alterarTipologia(obra, idTipologia, nome, imagem != null, imagem == null || imagem.isEmpty() ? null : imagem, u.getId());
        return tipologia(obra, idTipologia);
    }

    @Override
    public void excluirTipologia(Long idTipologia) {
        autorizacao.exigir(Modulo.ESCADINHA, Acao.ESCRITA);
        Long obra = contexto.obraEscolhida();
        if (repository.excluirTipologia(obra, idTipologia) == 0) throw new NaoEncontradoException("Tipologia não encontrada nesta obra");
    }

    private br.com.nord_tool_backend.dto.EscadinhaTipologiaDto tipologia(Long obra, Long id) {
        return repository.tipologias(obra, null, id).stream().findFirst()
                .orElseThrow(() -> new NaoEncontradoException("Tipologia não encontrada nesta obra"));
    }

    private static String nomeTipologia(br.com.nord_tool_backend.form.EscadinhaTipologiaForm form) {
        String n = form.getNmTipologia() == null ? "" : form.getNmTipologia().trim();
        if (n.isEmpty()) throw new EntradaInvalidaException("Informe o nome da tipologia");
        return n;
    }

    private EscadinhaDto montar(Long obra) {
        List<EscadinhaTorreDto> torres = repository.listarTorres(obra).stream().map(this::converter).collect(Collectors.toList());
        JsonNode modelo = repository.modelo(obra).map(this::ler).orElse(null);
        return new EscadinhaDto(torres, modelo);
    }

    private EscadinhaTorre torre(Long obra, Long idTorre) {
        return repository.buscarTorre(obra, idTorre).orElseThrow(() -> new NaoEncontradoException("Torre não encontrada nesta obra"));
    }

    private EscadinhaTorreDto converter(EscadinhaTorre t) {
        EscadinhaTorreDto d = new EscadinhaTorreDto();
        d.setIdTorre(t.getIdTorre());
        d.setNmTorre(t.getNmTorre());
        d.setNrOrdem(t.getNrOrdem());
        d.setConfig(ler(t.getTxConfig()));
        d.setCiclos(ler(t.getTxCiclos()));
        d.setGrade(t.getTxGrade() == null ? json.createObjectNode() : ler(t.getTxGrade()));
        d.setTemPlanta(Boolean.TRUE.equals(t.getTemPlanta()));
        d.setNrVersao(t.getNrVersao());
        d.setDhAtualizacao(t.getDhAtualizacao());
        return d;
    }

    private static String nome(EscadinhaTorreForm form) {
        String n = form.getNmTorre() == null ? "" : form.getNmTorre().trim();
        if (n.isEmpty()) throw new EntradaInvalidaException("Informe o nome da torre");
        return n;
    }

    /** Documento JSON (objeto ou lista) dentro do limite de tamanho. */
    String documento(JsonNode no, String rotulo) {
        if (no == null || !(no.isObject() || no.isArray())) throw new EntradaInvalidaException("Formato inválido em " + rotulo);
        String texto;
        try {
            texto = json.writeValueAsString(no);
        } catch (JsonProcessingException e) {
            throw new EntradaInvalidaException("Formato inválido em " + rotulo);
        }
        if (texto.length() > MAX_JSON) throw new EntradaInvalidaException("Dados grandes demais em " + rotulo);
        return texto;
    }

    /** null = não mexer; "" = remover; senão precisa ser uma imagem em data URL dentro do limite. */
    static String plantaValida(String planta) {
        if (planta == null || planta.isEmpty()) return planta;
        if (planta.length() > MAX_PLANTA) throw new EntradaInvalidaException("A imagem da prancha é grande demais");
        if (!PLANTA.matcher(planta).matches()) throw new EntradaInvalidaException("A prancha deve ser uma imagem JPEG, PNG ou WEBP");
        return planta;
    }

    private JsonNode ler(String texto) {
        if (texto == null) return null;
        try {
            return json.readTree(texto);
        } catch (JsonProcessingException e) {
            throw new FalhaInternaException(e);
        }
    }
}
