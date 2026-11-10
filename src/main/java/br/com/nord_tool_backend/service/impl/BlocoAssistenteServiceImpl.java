package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.dto.BlocoTrechoDto;
import br.com.nord_tool_backend.repository.BlocoAssistenteRepository;
import br.com.nord_tool_backend.security.UsuarioAutenticado;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.service.BlocoAssistenteService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.Normalizer;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Chat dos cadernos: em vez de dar ferramentas à IA (cada provedor tem as suas, e os gratuitos nem sempre têm), o
 * Bloco de notas vai no prompt. O serviço lê o texto das páginas (caixas de texto), dos documentos e das planilhas,
 * escolhe os trechos que mais combinam com a pergunta e descreve as ações que a tela executa quando a pessoa confirma.
 */
@Service
public class BlocoAssistenteServiceImpl implements BlocoAssistenteService {

    static final int MAX_CATALOGO = 150;
    static final int MAX_TRECHOS_LIDOS = 2000;
    static final int MAX_CARACTERES_TRECHOS = 16_000;
    static final int MAX_CARACTERES_POR_TRECHO = 3_000;
    /** Sem palavra da pergunta nos textos (ex.: "resuma o último caderno"), vão os mais recentes. */
    static final int RECENTES_SEM_BUSCA = 4;
    static final String CADERNO = "CADERNO";
    static final String PAGINA = "PAGINA";
    static final String DOCUMENTO_CADERNO = "DOCUMENTO_CADERNO";
    static final String PLANILHA = "PLANILHA";
    static final String DOCUMENTO = "DOCUMENTO";
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Pattern ACENTOS = Pattern.compile("\\p{M}+");
    private static final Pattern SEPARADOR = Pattern.compile("[^a-z0-9]+");
    private static final Pattern FIM_DE_BLOCO = Pattern.compile("(?i)<br\\s*/?>|</(p|div|li|h[1-6]|tr|blockquote)>");
    private static final Pattern FIM_DE_CELULA = Pattern.compile("(?i)</t[dh]>");
    private static final Pattern TAG = Pattern.compile("<[^>]*>");
    private static final Pattern ESPACOS = Pattern.compile("[ \\t\\x0B\\f\\r]+");
    private static final Pattern LINHAS_VAZIAS = Pattern.compile("\\n\\s*\\n+");
    private static final Pattern ESPACO_NA_QUEBRA = Pattern.compile(" *\\n *");
    private static final Set<String> PALAVRAS_VAZIAS = new HashSet<>(Arrays.asList(
            "que", "para", "com", "uma", "umas", "uns", "dos", "das", "nos", "nas", "pelo", "pela", "por", "mais", "menos",
            "como", "qual", "quais", "onde", "quando", "tem", "ter", "meu", "minha", "meus", "minhas", "esse", "essa", "este",
            "esta", "isso", "isto", "aqui", "sobre", "foi", "ser", "sao", "estao", "esta", "voce", "pode", "favor", "lugia",
            "caderno", "cadernos", "pagina", "paginas", "planilha", "planilhas", "documento", "documentos", "the", "and"));

    private final BlocoAssistenteRepository repository;
    private final AutorizacaoService autorizacao;
    private final ObjectMapper json;

    public BlocoAssistenteServiceImpl(BlocoAssistenteRepository repository, AutorizacaoService autorizacao, ObjectMapper json) {
        this.repository = repository;
        this.autorizacao = autorizacao;
        this.json = json;
    }

    @Override
    public String contexto(String pergunta, String tipoAberto, Long idAberto) {
        UsuarioAutenticado u = autorizacao.exigirAutenticado();
        List<BlocoTrechoDto> catalogo = repository.catalogo(u.getId(), MAX_CATALOGO);
        List<Trecho> trechos = new ArrayList<>();
        for (BlocoTrechoDto t : repository.trechos(u.getId(), MAX_TRECHOS_LIDOS)) {
            String texto = textoDo(t);
            if (!texto.isEmpty()) trechos.add(new Trecho(t, texto));
        }
        return montar(catalogo, escolher(trechos, termos(pergunta), tipoAberto, idAberto), tipoAberto, idAberto);
    }

    // ---------- texto de cada item ----------

    String textoDo(BlocoTrechoDto t) {
        String conteudo = t.getConteudo() == null ? "" : t.getConteudo();
        if (PAGINA.equals(t.getCdTipo())) return textoDaPagina(conteudo);
        if (PLANILHA.equals(t.getCdTipo())) return textoDaPlanilha(conteudo);
        return textoDoHtml(conteudo);
    }

    /** Caixas de texto da página, na ordem (de cima para baixo como foram criadas). Traços e imagens ficam de fora. */
    String textoDaPagina(String conteudo) {
        JsonNode raiz = ler(conteudo);
        if (raiz == null) return "";
        List<String> linhas = new ArrayList<>();
        for (JsonNode item : raiz.path("itens")) {
            if (!"tx".equals(item.path("t").asText())) continue;
            String texto;
            if (item.path("r").isArray() && item.path("r").size() > 0) {
                StringBuilder sb = new StringBuilder();
                for (JsonNode trecho : item.path("r")) sb.append(trecho.path("t").asText(""));
                texto = sb.toString();
            } else {
                texto = item.path("txt").asText("");
            }
            if (!texto.trim().isEmpty()) linhas.add(texto.trim());
        }
        return String.join("\n", linhas);
    }

    /** Uma linha por linha da planilha: "Aba · 2: A2=Cimento | B2=10". Fórmulas vão com o resultado. */
    String textoDaPlanilha(String conteudo) {
        JsonNode raiz = ler(conteudo);
        if (raiz == null || !raiz.isArray()) return "";
        StringBuilder sb = new StringBuilder();
        for (JsonNode aba : raiz) {
            TreeMap<Integer, TreeMap<Integer, String>> linhas = new TreeMap<>();
            for (JsonNode cel : aba.path("celldata")) {
                String valor = valorDaCelula(cel.path("v"));
                if (valor.isEmpty()) continue;
                linhas.computeIfAbsent(cel.path("r").asInt(), k -> new TreeMap<>()).put(cel.path("c").asInt(), valor);
            }
            if (linhas.isEmpty()) continue;
            String nome = aba.path("name").asText("Aba");
            linhas.forEach((r, colunas) -> {
                List<String> celulas = new ArrayList<>();
                colunas.forEach((c, v) -> celulas.add(coluna(c) + (r + 1) + "=" + v));
                sb.append(nome).append(" · ").append(r + 1).append(": ").append(String.join(" | ", celulas)).append('\n');
            });
        }
        return sb.toString().trim();
    }

    private static String valorDaCelula(JsonNode v) {
        if (v.isMissingNode() || v.isNull()) return "";
        if (v.isValueNode()) return v.asText("").trim();
        String texto = v.path("m").isValueNode() ? v.path("m").asText("") : "";
        if (texto.isEmpty() && v.path("v").isValueNode()) texto = v.path("v").asText("");
        if (texto.isEmpty() && v.path("ct").path("s").isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode s : v.path("ct").path("s")) sb.append(s.path("v").asText(""));
            texto = sb.toString();
        }
        texto = texto.trim();
        String formula = v.path("f").asText("");
        return formula.isEmpty() || texto.isEmpty() ? texto : texto + " (" + formula + ")";
    }

    /** A, B, … Z, AA, AB… */
    static String coluna(int c) {
        StringBuilder sb = new StringBuilder();
        for (int n = c + 1; n > 0; n = (n - 1) / 26) sb.insert(0, (char) ('A' + (n - 1) % 26));
        return sb.toString();
    }

    static String textoDoHtml(String html) {
        String t = FIM_DE_BLOCO.matcher(html).replaceAll("\n");
        t = FIM_DE_CELULA.matcher(t).replaceAll(" | ");
        t = TAG.matcher(t).replaceAll("");
        t = t.replace("&nbsp;", " ").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'").replace("&amp;", "&");
        t = ESPACOS.matcher(t).replaceAll(" ");
        t = ESPACO_NA_QUEBRA.matcher(t).replaceAll("\n");
        return LINHAS_VAZIAS.matcher(t).replaceAll("\n").trim();
    }

    private JsonNode ler(String conteudo) {
        if (conteudo == null || conteudo.trim().isEmpty()) return null;
        try {
            return json.readTree(conteudo);
        } catch (IOException e) {
            // Conteúdo estragado não impede o chat: o item só fica sem texto.
            return null;
        }
    }

    // ---------- busca ----------

    static String normalizar(String s) {
        return ACENTOS.matcher(Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD)).replaceAll("").toLowerCase(Locale.ROOT);
    }

    /** Palavras da pergunta que valem para a busca (3 letras ou mais, sem as muito comuns). */
    static Set<String> termos(String pergunta) {
        Set<String> termos = new LinkedHashSet<>();
        for (String p : SEPARADOR.split(normalizar(pergunta))) {
            if (p.length() >= 3 && !PALAVRAS_VAZIAS.contains(p)) termos.add(p);
        }
        return termos;
    }

    static int ocorrencias(String texto, String termo) {
        int n = 0;
        for (int i = texto.indexOf(termo); i >= 0 && n < 10; i = texto.indexOf(termo, i + termo.length())) n++;
        return n;
    }

    /**
     * O item aberto vem primeiro; depois os que mais combinam com a pergunta (título vale mais); sem nenhuma combinação,
     * os mais recentes. Para quando acaba o espaço.
     */
    List<Trecho> escolher(List<Trecho> trechos, Set<String> termos, String tipoAberto, Long idAberto) {
        for (int i = 0; i < trechos.size(); i++) {
            Trecho t = trechos.get(i);
            String titulo = normalizar(t.dto.getNmTitulo());
            String texto = normalizar(t.texto);
            int pontos = 0;
            for (String termo : termos) pontos += ocorrencias(texto, termo) + (titulo.contains(termo) ? 5 : 0);
            t.pontos = pontos;
            t.aberto = idAberto != null && idAberto.equals(t.dto.getId()) && mesmoTipo(tipoAberto, t.dto.getCdTipo());
            t.recencia = i;
        }
        boolean achou = trechos.stream().anyMatch(t -> t.pontos > 0);
        List<Trecho> ordem = new ArrayList<>(trechos);
        ordem.sort(Comparator.<Trecho>comparingInt(t -> t.aberto ? 0 : 1)
                .thenComparing(Comparator.<Trecho>comparingInt(t -> t.pontos).reversed())
                .thenComparingInt(t -> t.recencia));
        List<Trecho> escolhidos = new ArrayList<>();
        int usados = 0;
        int recentes = 0;
        for (Trecho t : ordem) {
            if (!t.aberto && t.pontos == 0 && (achou || recentes >= RECENTES_SEM_BUSCA)) continue;
            if (!t.aberto && t.pontos == 0) recentes++;
            String recorte = recortar(t.texto, termos);
            if (usados + recorte.length() > MAX_CARACTERES_TRECHOS) continue;
            t.recorte = recorte;
            escolhidos.add(t);
            usados += recorte.length();
        }
        return escolhidos;
    }

    /** Páginas e o documento do caderno contam como o caderno aberto. */
    private static boolean mesmoTipo(String tipoAberto, String tipoTrecho) {
        if (CADERNO.equals(tipoAberto)) return PAGINA.equals(tipoTrecho) || DOCUMENTO_CADERNO.equals(tipoTrecho);
        return tipoAberto != null && tipoAberto.equals(tipoTrecho);
    }

    /** Até {@link #MAX_CARACTERES_POR_TRECHO}, começando um pouco antes da primeira palavra encontrada. */
    static String recortar(String texto, Set<String> termos) {
        if (texto.length() <= MAX_CARACTERES_POR_TRECHO) return texto;
        String normal = normalizar(texto);
        int inicio = termos.stream().mapToInt(normal::indexOf).filter(i -> i >= 0).min().orElse(0);
        inicio = Math.max(0, Math.min(inicio - 300, texto.length() - MAX_CARACTERES_POR_TRECHO));
        String recorte = texto.substring(inicio, inicio + MAX_CARACTERES_POR_TRECHO);
        return (inicio > 0 ? "…" : "") + recorte + (inicio + MAX_CARACTERES_POR_TRECHO < texto.length() ? "…" : "");
    }

    // ---------- prompt ----------

    static String referencia(String tipo, Long id) {
        if (PLANILHA.equals(tipo)) return "planilha " + id;
        if (DOCUMENTO.equals(tipo)) return "documento " + id;
        return "caderno " + id;
    }

    String montar(List<BlocoTrechoDto> catalogo, List<Trecho> trechos, String tipoAberto, Long idAberto) {
        StringBuilder sb = new StringBuilder();
        sb.append("## Bloco de notas da pessoa\n")
                .append("Você está no chat dos cadernos, dentro do Bloco de notas do NordTool. Responda com base no conteúdo abaixo ")
                .append("(cadernos escritos à mão, PDFs anotados, planilhas e documentos da própria pessoa). Cite de onde tirou ")
                .append("a informação, como \"caderno Obra X, página 3\". Se não estiver aqui, diga que não encontrou: o texto ")
                .append("escrito à mão (traços) e o conteúdo das imagens não são lidos, só as caixas de texto, os documentos e as planilhas.\n");
        if (tipoAberto != null && idAberto != null) {
            sb.append("Aberto na tela agora: ").append(referencia(tipoAberto, idAberto)).append(".\n");
        }
        sb.append("\n### Itens (mais recentes primeiro)\n");
        if (catalogo.isEmpty()) sb.append("A pessoa ainda não tem nenhum caderno, planilha ou documento.\n");
        for (BlocoTrechoDto c : catalogo) {
            sb.append("- [").append(referencia(c.getCdTipo(), c.getId())).append("] \"").append(c.getNmTitulo()).append('"');
            if (CADERNO.equals(c.getCdTipo()) && c.getQtPaginas() != null) {
                sb.append(" · ").append(c.getQtPaginas()).append(c.getQtPaginas() == 1 ? " página" : " páginas");
            }
            if (c.getDhAlteracao() != null) sb.append(" · alterado em ").append(DATA.format(c.getDhAlteracao()));
            sb.append('\n');
        }
        sb.append("\n### Trechos\n");
        if (trechos.isEmpty()) sb.append("Nenhum texto encontrado para esta pergunta.\n");
        for (Trecho t : trechos) {
            BlocoTrechoDto d = t.dto;
            sb.append("[").append(referencia(d.getCdTipo(), d.getId()));
            if (PAGINA.equals(d.getCdTipo())) sb.append(", página ").append(d.getNrPagina());
            if (DOCUMENTO_CADERNO.equals(d.getCdTipo())) sb.append(", documento do caderno");
            sb.append("] \"").append(d.getNmTitulo()).append("\"\n").append(t.recorte).append("\n\n");
        }
        sb.append(ACOES);
        return sb.toString();
    }

    static final String ACOES = String.join("\n",
            "### Ações",
            "Quando a pessoa pedir para fazer algo no Bloco de notas (abrir, criar, anotar, montar uma tabela, renomear), diga em uma frase",
            "o que vai ser feito e termine a resposta com um bloco de código com a linguagem nordtool-acoes, contendo uma lista JSON válida.",
            "A tela mostra cada ação com um botão \"Fazer\": nada acontece sem a pessoa confirmar. Exemplo:",
            "```nordtool-acoes",
            "[{\"acao\":\"abrir\",\"tipo\":\"caderno\",\"id\":12,\"pagina\":3}]",
            "```",
            "Ações possíveis (use só ids da lista de itens):",
            "- {\"acao\":\"abrir\",\"tipo\":\"caderno|planilha|documento\",\"id\":N,\"pagina\":N} (pagina só para caderno, opcional)",
            "- {\"acao\":\"criar_caderno\",\"titulo\":\"...\",\"texto\":\"...\"} (texto opcional, vai numa caixa de texto da primeira página)",
            "- {\"acao\":\"anotar\",\"id\":N,\"texto\":\"...\"} (página nova no fim do caderno N, com o texto numa caixa de texto)",
            "- {\"acao\":\"criar_documento\",\"titulo\":\"...\",\"html\":\"<h1>...</h1><p>...</p>\"} (HTML simples: h1-h3, p, ul, ol, li, b, i, u, table)",
            "- {\"acao\":\"criar_planilha\",\"titulo\":\"...\",\"linhas\":[[\"Item\",\"Qtd\"],[\"Cimento\",10],[\"Total\",\"=SOMA(B2:B2)\"]]}",
            "  (a primeira linha é o cabeçalho; números como número; fórmulas em português começando com =)",
            "- {\"acao\":\"renomear\",\"tipo\":\"caderno|planilha|documento\",\"id\":N,\"titulo\":\"...\"}",
            "Não existe ação de excluir nem de apagar conteúdo: se pedirem, explique como fazer na tela.",
            "Com foto ou PDF anexado (quadro, caderno de papel, nota, tabela), leia o conteúdo e, se a pessoa pedir, use criar_planilha,",
            "criar_documento ou anotar para passar para o Bloco de notas.");

    /** Um item com texto, a pontuação da busca e o recorte que vai no prompt. */
    static final class Trecho {
        final BlocoTrechoDto dto;
        final String texto;
        int pontos;
        int recencia;
        boolean aberto;
        String recorte;

        Trecho(BlocoTrechoDto dto, String texto) {
            this.dto = dto;
            this.texto = texto;
        }
    }
}
