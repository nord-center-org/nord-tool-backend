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
import br.com.nord_tool_backend.storage.ArmazenamentoService;
import br.com.nord_tool_backend.storage.ArquivoConteudo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BlocoNotasServiceImplTest {

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2, 3};
    private static final Long EU = 7L;

    private BlocoNotasRepository repository;
    private ArmazenamentoService armazenamento;
    private BlocoNotasServiceImpl service;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        repository = mock(BlocoNotasRepository.class);
        armazenamento = mock(ArmazenamentoService.class);
        AutorizacaoService autorizacao = mock(AutorizacaoService.class);
        when(autorizacao.exigirAutenticado()).thenReturn(new UsuarioAutenticado(EU, "eu@obra.com", "USUARIO",
                Collections.emptyMap(), 0, java.time.Instant.now()));
        service = new BlocoNotasServiceImpl(repository, autorizacao, armazenamento, mapper);
        when(repository.caderno(1L, EU)).thenReturn(Optional.of(caderno(1L)));
    }

    private static BlocoCadernoDto caderno(Long id) {
        BlocoCadernoDto c = new BlocoCadernoDto();
        c.setId(id);
        c.setNmTitulo("Obra Norte");
        c.setNrVersao(1);
        return c;
    }

    private static BlocoPaginaDto pagina(Long id, Long arquivo) {
        BlocoPaginaDto p = new BlocoPaginaDto();
        p.setId(id);
        p.setIdCaderno(1L);
        p.setCdFundo(arquivo == null ? "PAUTADO" : "PDF");
        p.setNrLargura(794);
        p.setNrAltura(1123);
        p.setIdArquivoFundo(arquivo);
        p.setNrVersao(3);
        return p;
    }

    private static BlocoCadernoForm form(String titulo) {
        BlocoCadernoForm f = new BlocoCadernoForm();
        f.setNmTitulo(titulo);
        return f;
    }

    // ---------- cadernos ----------

    @Test
    void listaSoOsMeus() {
        service.cadernos();
        verify(repository).cadernos(EU);
    }

    @Test
    void cadernoDeOutraPessoaNaoExiste() {
        when(repository.caderno(2L, EU)).thenReturn(Optional.empty());
        assertThrows(NaoEncontradoException.class, () -> service.abrir(2L));
        assertThrows(NaoEncontradoException.class, () -> service.excluir(2L));
        assertThrows(NaoEncontradoException.class, () -> service.novaPagina(2L, new BlocoPaginaForm()));
        verify(repository, never()).excluirCaderno(any(), any());
    }

    @Test
    void criaComAPrimeiraPaginaNoFundoEscolhido() {
        when(repository.inserirCaderno(EU, "Obra Norte", "#2563eb")).thenReturn(1L);
        BlocoCadernoForm f = form("  Obra Norte ");
        f.setCdFundo("quadriculado");
        service.criar(f);
        verify(repository).inserirPagina(1L, 1, "QUADRICULADO", 794, 1123, null, "{\"itens\":[]}");
        verify(repository).paginas(1L, EU);
    }

    @Test
    void limiteDeCadernosEFundoInvalido() {
        when(repository.contarCadernos(EU)).thenReturn(BlocoNotasServiceImpl.MAX_CADERNOS);
        assertThrows(NegocioException.class, () -> service.criar(form("Mais um")));
        assertThrows(EntradaInvalidaException.class, () -> BlocoNotasServiceImpl.fundo("listrado"));
        assertEquals("PAUTADO", BlocoNotasServiceImpl.fundo(" "));
    }

    @Test
    void renomeiaComVersao() {
        BlocoCadernoForm f = form("Novo nome");
        f.setCdCor("#DC2626");
        assertThrows(EntradaInvalidaException.class, () -> service.alterar(1L, f));
        f.setNrVersao(1);
        when(repository.alterarCaderno(1L, EU, "Novo nome", "#dc2626", 1)).thenReturn(1);
        service.alterar(1L, f);
        when(repository.alterarCaderno(1L, EU, "Novo nome", "#dc2626", 1)).thenReturn(0);
        assertThrows(ConflitoException.class, () -> service.alterar(1L, f));
    }

    @Test
    void excluirApagaOsArquivosDoCaderno() {
        when(repository.arquivosDoCaderno(1L)).thenReturn(Arrays.asList(50L, 51L));
        service.excluir(1L);
        verify(repository).excluirCaderno(1L, EU);
        verify(armazenamento).apagar(50L);
        verify(armazenamento).apagar(51L);
    }

    // ---------- páginas ----------

    @Test
    void abrePaginaComODesenho() {
        when(repository.pagina(10L, EU)).thenReturn(Optional.of(pagina(10L, null)));
        when(repository.conteudo(10L, EU)).thenReturn(Optional.of("{\"itens\":[{\"t\":\"tr\"}]}"));
        assertEquals("tr", service.pagina(10L).getConteudo().path("itens").get(0).path("t").asText());
        when(repository.conteudo(10L, EU)).thenReturn(Optional.of("quebrado"));
        assertTrue(service.pagina(10L).getConteudo().path("itens").isArray());
    }

    @Test
    void paginaNovaNaPosicaoOuNoFim() {
        when(repository.paginas(1L, EU)).thenReturn(Arrays.asList(pagina(10L, null), pagina(11L, null)));
        when(repository.inserirPagina(eq(1L), anyInt(), anyString(), anyInt(), anyInt(), any(), anyString())).thenReturn(12L);
        when(repository.pagina(12L, EU)).thenReturn(Optional.of(pagina(12L, null)));
        service.novaPagina(1L, new BlocoPaginaForm());
        verify(repository).inserirPagina(1L, 3, "PAUTADO", 794, 1123, null, "{\"itens\":[]}");
        BlocoPaginaForm f = new BlocoPaginaForm();
        f.setNrPosicao(1);
        f.setCdFundo("BRANCO");
        f.setNrLargura(1123);
        f.setNrAltura(794);
        service.novaPagina(1L, f);
        verify(repository).inserirPagina(1L, 1, "BRANCO", 1123, 794, null, "{\"itens\":[]}");
        verify(repository, org.mockito.Mockito.times(2)).tocarCaderno(1L);
        assertEquals(3, BlocoNotasServiceImpl.posicao(99, 2));
    }

    @Test
    void duplicaPaginaCopiandoOFundoDePdf() {
        when(repository.paginas(1L, EU)).thenReturn(Collections.singletonList(pagina(10L, 50L)));
        when(repository.pagina(10L, EU)).thenReturn(Optional.of(pagina(10L, 50L)));
        when(repository.conteudo(10L, EU)).thenReturn(Optional.of("{\"itens\":[1]}"));
        when(armazenamento.abrir(50L)).thenReturn(new ArquivoConteudo("p.jpg", "image/jpeg", 6, JPEG));
        when(armazenamento.salvar("p.jpg", "image/jpeg", JPEG)).thenReturn(60L);
        when(repository.inserirPagina(eq(1L), anyInt(), anyString(), anyInt(), anyInt(), any(), anyString())).thenReturn(11L);
        when(repository.pagina(11L, EU)).thenReturn(Optional.of(pagina(11L, 60L)));
        BlocoPaginaForm f = new BlocoPaginaForm();
        f.setIdCopiarDe(10L);
        f.setNrPosicao(2);
        service.novaPagina(1L, f);
        verify(repository).inserirPagina(1L, 2, "PDF", 794, 1123, 60L, "{\"itens\":[1]}");
    }

    @Test
    void naoDuplicaPaginaDeOutroCaderno() {
        BlocoPaginaDto outra = pagina(20L, null);
        outra.setIdCaderno(2L);
        when(repository.pagina(20L, EU)).thenReturn(Optional.of(outra));
        BlocoPaginaForm f = new BlocoPaginaForm();
        f.setIdCopiarDe(20L);
        assertThrows(EntradaInvalidaException.class, () -> service.novaPagina(1L, f));
    }

    @Test
    void limiteDePaginas() {
        List<BlocoPaginaDto> muitas = new ArrayList<>();
        for (long i = 0; i < BlocoNotasServiceImpl.MAX_PAGINAS; i++) muitas.add(pagina(i, null));
        when(repository.paginas(1L, EU)).thenReturn(muitas);
        assertThrows(NegocioException.class, () -> service.novaPagina(1L, new BlocoPaginaForm()));
    }

    @Test
    void paginaDePdfGuardaAImagem() {
        when(repository.paginas(1L, EU)).thenReturn(Collections.singletonList(pagina(10L, null)));
        when(armazenamento.salvar(eq("p1.jpg"), anyString(), eq(JPEG))).thenReturn(50L);
        when(repository.inserirPagina(eq(1L), anyInt(), anyString(), anyInt(), anyInt(), any(), anyString())).thenReturn(11L);
        when(repository.pagina(11L, EU)).thenReturn(Optional.of(pagina(11L, 50L)));
        service.paginaDePdf(1L, null, 816, 1056, "p1.jpg", JPEG);
        verify(repository).inserirPagina(1L, 2, "PDF", 816, 1056, 50L, "{\"itens\":[]}");
        assertThrows(EntradaInvalidaException.class, () -> service.paginaDePdf(1L, null, 50, 1056, "p1.jpg", JPEG));
    }

    @Test
    void salvaODesenhoComVersaoEFundo() throws Exception {
        when(repository.pagina(10L, EU)).thenReturn(Optional.of(pagina(10L, null)));
        BlocoConteudoForm f = new BlocoConteudoForm();
        f.setConteudo(mapper.readTree("{\"itens\":[{\"t\":\"tr\"}]}"));
        f.setNrVersao(3);
        f.setCdFundo("pontilhado");
        when(repository.salvarConteudo(10L, "{\"itens\":[{\"t\":\"tr\"}]}", "PONTILHADO", 3)).thenReturn(1);
        service.salvar(10L, f);
        verify(repository).tocarCaderno(1L);

        when(repository.salvarConteudo(anyLong(), anyString(), anyString(), anyInt())).thenReturn(0);
        assertThrows(ConflitoException.class, () -> service.salvar(10L, f));
    }

    private static long anyLong() {
        return org.mockito.ArgumentMatchers.anyLong();
    }

    @Test
    void paginaDePdfContinuaPdf() throws Exception {
        when(repository.pagina(10L, EU)).thenReturn(Optional.of(pagina(10L, 50L)));
        BlocoConteudoForm f = new BlocoConteudoForm();
        f.setConteudo(mapper.readTree("{\"itens\":[]}"));
        f.setNrVersao(3);
        f.setCdFundo("BRANCO");
        when(repository.salvarConteudo(10L, "{\"itens\":[]}", "PDF", 3)).thenReturn(1);
        service.salvar(10L, f);
        verify(repository).salvarConteudo(10L, "{\"itens\":[]}", "PDF", 3);
    }

    @Test
    void conteudoInvalidoOuGrandeDemais() throws Exception {
        when(repository.pagina(10L, EU)).thenReturn(Optional.of(pagina(10L, null)));
        BlocoConteudoForm f = new BlocoConteudoForm();
        f.setNrVersao(3);
        f.setConteudo(mapper.readTree("[1,2]"));
        assertThrows(EntradaInvalidaException.class, () -> service.salvar(10L, f));
        f.setConteudo(mapper.readTree("{\"outra\":1}"));
        assertThrows(EntradaInvalidaException.class, () -> service.salvar(10L, f));
        StringBuilder grande = new StringBuilder("{\"itens\":[\"");
        for (int i = 0; i < BlocoNotasServiceImpl.MAX_CONTEUDO; i++) grande.append('x');
        f.setConteudo(mapper.readTree(grande.append("\"]}").toString()));
        assertThrows(NegocioException.class, () -> service.salvar(10L, f));
        verify(repository, never()).salvarConteudo(any(), any(), any(), anyInt());
    }

    @Test
    void reordenaSoComTodasAsPaginas() {
        when(repository.paginas(1L, EU)).thenReturn(Arrays.asList(pagina(10L, null), pagina(11L, null)));
        service.reordenar(1L, Arrays.asList(11L, 10L));
        verify(repository).reordenar(1L, Arrays.asList(11L, 10L));
        assertThrows(EntradaInvalidaException.class, () -> service.reordenar(1L, Collections.singletonList(10L)));
        assertThrows(EntradaInvalidaException.class, () -> service.reordenar(1L, Arrays.asList(10L, 10L)));
    }

    @Test
    void excluiPaginaERenumeraMasDeixaUma() {
        when(repository.pagina(10L, EU)).thenReturn(Optional.of(pagina(10L, 50L)));
        when(repository.paginas(1L, EU)).thenReturn(Arrays.asList(pagina(9L, null), pagina(10L, 50L), pagina(11L, null)));
        service.excluirPagina(10L);
        verify(repository).excluirPagina(10L);
        verify(repository).reordenar(1L, Arrays.asList(9L, 11L));
        verify(armazenamento).apagar(50L);

        when(repository.paginas(1L, EU)).thenReturn(Collections.singletonList(pagina(10L, 50L)));
        assertThrows(NegocioException.class, () -> service.excluirPagina(10L));
    }

    // ---------- imagens ----------

    @Test
    void imagensDoCaderno() {
        when(armazenamento.salvar(eq("foto.jpg"), anyString(), eq(JPEG))).thenReturn(70L);
        when(repository.inserirImagem(1L, 70L)).thenReturn(5L);
        assertEquals(5L, service.imagem(1L, "foto.jpg", JPEG));
        assertThrows(RuntimeException.class, () -> service.imagem(1L, "foto.txt", new byte[]{1, 2, 3}));

        when(repository.arquivoDaImagem(5L, EU)).thenReturn(Optional.of(70L));
        when(armazenamento.abrir(70L)).thenReturn(new ArquivoConteudo("foto.jpg", "image/jpeg", 6, JPEG));
        assertEquals(70L, service.baixarImagem(5L).getVersao());
        when(repository.arquivoDaImagem(6L, EU)).thenReturn(Optional.empty());
        assertThrows(NaoEncontradoException.class, () -> service.baixarImagem(6L));
    }

    @Test
    void fundoSoEmPaginaDePdf() {
        when(repository.pagina(10L, EU)).thenReturn(Optional.of(pagina(10L, 50L)));
        when(armazenamento.abrir(50L)).thenReturn(new ArquivoConteudo("p.jpg", "image/jpeg", 6, JPEG));
        assertEquals(50L, service.baixarFundo(10L).getVersao());
        when(repository.pagina(11L, EU)).thenReturn(Optional.of(pagina(11L, null)));
        assertThrows(NaoEncontradoException.class, () -> service.baixarFundo(11L));
        assertNull(pagina(11L, null).getIdArquivoFundo());
    }

    @Test
    void criarGuardaCorPadrao() {
        when(repository.inserirCaderno(eq(EU), anyString(), anyString())).thenReturn(1L);
        service.criar(form("Sem cor"));
        ArgumentCaptor<String> cor = ArgumentCaptor.forClass(String.class);
        verify(repository).inserirCaderno(eq(EU), eq("Sem cor"), cor.capture());
        assertEquals("#2563eb", cor.getValue());
        verify(repository).inserirPagina(eq(1L), eq(1), eq("PAUTADO"), anyInt(), anyInt(), isNull(), anyString());
    }
}
