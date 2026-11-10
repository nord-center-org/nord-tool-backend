package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.dto.BlocoTrechoDto;
import br.com.nord_tool_backend.repository.BlocoAssistenteRepository;
import br.com.nord_tool_backend.security.UsuarioAutenticado;
import br.com.nord_tool_backend.service.AutorizacaoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BlocoAssistenteServiceImplTest {

    private static final Long EU = 7L;
    private BlocoAssistenteRepository repository;
    private BlocoAssistenteServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(BlocoAssistenteRepository.class);
        AutorizacaoService autorizacao = mock(AutorizacaoService.class);
        when(autorizacao.exigirAutenticado()).thenReturn(new UsuarioAutenticado(EU, "eu@obra.com", "USUARIO",
                Collections.emptyMap(), 0, Instant.now()));
        service = new BlocoAssistenteServiceImpl(repository, autorizacao, new ObjectMapper());
        when(repository.catalogo(eq(EU), anyInt())).thenReturn(Collections.emptyList());
        when(repository.trechos(eq(EU), anyInt())).thenReturn(Collections.emptyList());
    }

    private static BlocoTrechoDto item(String tipo, long id, String titulo, String conteudo) {
        BlocoTrechoDto t = new BlocoTrechoDto();
        t.setCdTipo(tipo);
        t.setId(id);
        t.setNmTitulo(titulo);
        t.setConteudo(conteudo);
        return t;
    }

    private static BlocoTrechoDto pagina(long idCaderno, String titulo, int nr, String texto) {
        BlocoTrechoDto t = item("PAGINA", idCaderno, titulo, "{\"v\":1,\"itens\":[{\"t\":\"tx\",\"id\":\"a\",\"txt\":\"" + texto + "\"}]}");
        t.setNrPagina(nr);
        t.setIdPagina(idCaderno * 100 + nr);
        return t;
    }

    @Test
    void textoDaPaginaJuntaAsCaixasDeTextoEIgnoraTracos() {
        String json = "{\"v\":1,\"itens\":[{\"t\":\"tr\",\"id\":\"1\",\"p\":[1,2]},{\"t\":\"tx\",\"txt\":\"Comprar cimento\"},"
                + "{\"t\":\"tx\",\"txt\":\"velho\",\"r\":[{\"t\":\"Laje \",\"b\":1},{\"t\":\"dia 12\"}]},{\"t\":\"tx\",\"txt\":\"  \"}]}";
        assertEquals("Comprar cimento\nLaje dia 12", service.textoDaPagina(json));
        assertEquals("", service.textoDaPagina("não é json"));
        assertEquals("", service.textoDaPagina(""));
    }

    @Test
    void textoDaPlanilhaUmaLinhaPorLinhaComReferenciaDaCelula() {
        String json = "[{\"name\":\"Compras\",\"celldata\":["
                + "{\"r\":0,\"c\":0,\"v\":{\"v\":\"Item\",\"m\":\"Item\"}},{\"r\":0,\"c\":1,\"v\":{\"v\":\"Qtd\"}},"
                + "{\"r\":1,\"c\":0,\"v\":{\"ct\":{\"t\":\"inlineStr\",\"s\":[{\"v\":\"Ci\"},{\"v\":\"mento\"}]}}},"
                + "{\"r\":1,\"c\":1,\"v\":{\"v\":10,\"m\":\"10\",\"f\":\"=5*2\"}},{\"r\":1,\"c\":27,\"v\":\"solto\"},"
                + "{\"r\":2,\"c\":0,\"v\":{}},{\"r\":3,\"c\":0,\"v\":null}]},{\"name\":\"Vazia\",\"celldata\":[]}]";
        assertEquals("Compras · 1: A1=Item | B1=Qtd\nCompras · 2: A2=Cimento | B2=10 (=5*2) | AB2=solto", service.textoDaPlanilha(json));
        assertEquals("", service.textoDaPlanilha("{\"nao\":\"lista\"}"));
    }

    @Test
    void textoDoHtmlTiraAsTagsEMantemAsLinhas() {
        String html = "<h1>Ata</h1><p>Concreto&nbsp;&amp; aço</p><table><tr><td>A</td><td>B</td></tr></table><p></p><p>x &lt; y</p>";
        assertEquals("Ata\nConcreto & aço\nA | B |\nx < y", BlocoAssistenteServiceImpl.textoDoHtml(html));
    }

    @Test
    void colunasComoNaPlanilha() {
        assertEquals("A", BlocoAssistenteServiceImpl.coluna(0));
        assertEquals("Z", BlocoAssistenteServiceImpl.coluna(25));
        assertEquals("AA", BlocoAssistenteServiceImpl.coluna(26));
        assertEquals("BA", BlocoAssistenteServiceImpl.coluna(52));
    }

    @Test
    void termosSemAcentoSemPalavrasComunsESemCurtas() {
        Set<String> t = BlocoAssistenteServiceImpl.termos("Onde está o orçamento da Laje no caderno?");
        assertEquals(Arrays.asList("orcamento", "laje"), new ArrayList<>(t));
        assertTrue(BlocoAssistenteServiceImpl.termos(null).isEmpty());
    }

    @Test
    void contextoTrazOCatalogoOsTrechosQueCombinamEAsAcoes() {
        BlocoTrechoDto caderno = item("CADERNO", 12, "Obra Jardim", null);
        caderno.setQtPaginas(3);
        caderno.setDhAlteracao(LocalDateTime.of(2026, 11, 5, 10, 0));
        BlocoTrechoDto umaPagina = item("CADERNO", 13, "Vazio", null);
        umaPagina.setQtPaginas(1);
        when(repository.catalogo(eq(EU), anyInt())).thenReturn(Arrays.asList(caderno, umaPagina, item("PLANILHA", 3, "Orçamento", null)));
        when(repository.trechos(eq(EU), anyInt())).thenReturn(Arrays.asList(
                pagina(12, "Obra Jardim", 2, "Concretagem da laje sexta"),
                pagina(13, "Vazio", 1, "Lista de EPI"),
                item("PLANILHA", 3, "Orçamento", "[{\"name\":\"A\",\"celldata\":[{\"r\":0,\"c\":0,\"v\":{\"v\":\"Cimento\"}}]}]"),
                item("DOCUMENTO", 4, "Ata", "<p>Reunião sobre a laje</p>"),
                item("DOCUMENTO_CADERNO", 12, "Obra Jardim", "<p>   </p>")));

        String r = service.contexto("Quando é a concretagem da laje?", null, null);

        assertTrue(r.contains("- [caderno 12] \"Obra Jardim\" · 3 páginas · alterado em 05/11/2026"));
        assertTrue(r.contains("- [caderno 13] \"Vazio\" · 1 página\n"));
        assertTrue(r.contains("- [planilha 3] \"Orçamento\""));
        assertTrue(r.contains("[caderno 12, página 2] \"Obra Jardim\"\nConcretagem da laje sexta"));
        assertTrue(r.contains("[documento 4] \"Ata\"\nReunião sobre a laje"));
        assertFalse(r.contains("Lista de EPI"), "o que não combina com a pergunta fica de fora");
        assertFalse(r.contains("A1=Cimento"));
        assertTrue(r.indexOf("caderno 12, página 2") < r.indexOf("documento 4"), "quem combina mais vem antes");
        assertTrue(r.contains("```nordtool-acoes"));
        assertFalse(r.contains("Aberto na tela"));
    }

    @Test
    void itemAbertoVemPrimeiroMesmoSemCombinar() {
        when(repository.trechos(eq(EU), anyInt())).thenReturn(Arrays.asList(
                pagina(12, "Obra", 1, "Concretagem"),
                item("DOCUMENTO_CADERNO", 13, "Diário", "<p>Chuva forte</p>"),
                item("PLANILHA", 13, "Outra coisa", "[{\"name\":\"A\",\"celldata\":[{\"r\":0,\"c\":0,\"v\":\"Chuva\"}]}]")));

        String r = service.contexto("concretagem", "CADERNO", 13L);

        assertTrue(r.contains("Aberto na tela agora: caderno 13."));
        assertTrue(r.indexOf("[caderno 13, documento do caderno]") < r.indexOf("[caderno 12, página 1]"));
        assertFalse(r.contains("[planilha 13]"), "a planilha 13 não é o caderno 13");
        assertTrue(service.contexto("x", "PLANILHA", 13L).contains("Aberto na tela agora: planilha 13."));
        assertTrue(service.contexto("x", "DOCUMENTO", 4L).contains("documento 4"));
    }

    @Test
    void semCombinacaoVaoOsMaisRecentes() {
        List<BlocoTrechoDto> paginas = new ArrayList<>();
        for (int i = 1; i <= 6; i++) paginas.add(pagina(i, "Caderno " + i, 1, "texto " + i));
        when(repository.trechos(eq(EU), anyInt())).thenReturn(paginas);

        String r = service.contexto("resuma o último", null, null);

        assertTrue(r.contains("texto 1") && r.contains("texto 4"));
        assertFalse(r.contains("texto 5"));
    }

    @Test
    void semNadaAvisaQueNaoTem() {
        String r = service.contexto("oi", null, null);
        assertTrue(r.contains("ainda não tem nenhum caderno"));
        assertTrue(r.contains("Nenhum texto encontrado"));
    }

    @Test
    void recorteComecaPertoDaPalavraEParaNoLimite() {
        StringBuilder longo = new StringBuilder();
        for (int i = 0; i < 1000; i++) longo.append("abcdefghi ");
        longo.append("PROCURADO ");
        for (int i = 0; i < 1000; i++) longo.append("jklmnopqr ");
        String r = BlocoAssistenteServiceImpl.recortar(longo.toString(), Collections.singleton("procurado"));
        assertTrue(r.startsWith("…") && r.endsWith("…"));
        assertTrue(r.contains("PROCURADO"));
        assertEquals(BlocoAssistenteServiceImpl.MAX_CARACTERES_POR_TRECHO + 2, r.length());
        assertEquals("curto", BlocoAssistenteServiceImpl.recortar("curto", Collections.emptySet()));
        assertTrue(BlocoAssistenteServiceImpl.recortar(longo.toString(), Collections.emptySet()).startsWith("abc"));
    }

    @Test
    void paraQuandoAcabaOEspaco() {
        List<BlocoTrechoDto> grandes = new ArrayList<>();
        StringBuilder texto = new StringBuilder();
        for (int i = 0; i < 400; i++) texto.append("laje laje ");
        for (int i = 1; i <= 10; i++) grandes.add(pagina(i, "C" + i, 1, texto.toString()));
        when(repository.trechos(eq(EU), anyInt())).thenReturn(grandes);

        String r = service.contexto("laje", null, null);

        int n = r.split("\\[caderno \\d+, página 1\\]", -1).length - 1;
        assertEquals(BlocoAssistenteServiceImpl.MAX_CARACTERES_TRECHOS / (BlocoAssistenteServiceImpl.MAX_CARACTERES_POR_TRECHO + 1), n);
    }
}
