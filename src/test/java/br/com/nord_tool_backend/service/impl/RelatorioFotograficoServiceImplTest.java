package br.com.nord_tool_backend.service.impl;

import static org.junit.jupiter.api.Assertions.assertTrue;
import br.com.nord_tool_backend.dto.RelatorioFotoDto;
import br.com.nord_tool_backend.dto.RelatorioFotograficoDto;
import br.com.nord_tool_backend.dto.RelatorioItemDto;
import br.com.nord_tool_backend.dto.RelatorioModeloDto;
import br.com.nord_tool_backend.dto.RelatorioModeloItemDto;
import br.com.nord_tool_backend.exception.AcessoNegadoException;
import br.com.nord_tool_backend.exception.ConflitoException;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.exception.NaoEncontradoException;
import br.com.nord_tool_backend.exception.NegocioException;
import br.com.nord_tool_backend.form.RelatorioFotoForm;
import br.com.nord_tool_backend.form.RelatorioFotograficoForm;
import br.com.nord_tool_backend.form.RelatorioItemForm;
import br.com.nord_tool_backend.form.RelatorioModeloForm;
import br.com.nord_tool_backend.form.RelatorioStatusForm;
import br.com.nord_tool_backend.form.RelatorioTrocaModeloForm;
import br.com.nord_tool_backend.repository.RelatorioFotograficoRepository;
import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.security.UsuarioAutenticado;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.storage.ArmazenamentoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RelatorioFotograficoServiceImplTest {

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2, 3};

    private AutorizacaoService autorizacao;
    private RelatorioFotograficoRepository repository;
    private br.com.nord_tool_backend.repository.CronogramaSemanalRepository pessoas;
    private br.com.nord_tool_backend.repository.CronogramaEquipeRepository equipes;
    private ArmazenamentoService armazenamento;
    private RelatorioFotograficoServiceImpl service;

    @BeforeEach
    void setUp() {
        autorizacao = mock(AutorizacaoService.class);
        repository = mock(RelatorioFotograficoRepository.class);
        pessoas = mock(br.com.nord_tool_backend.repository.CronogramaSemanalRepository.class);
        equipes = mock(br.com.nord_tool_backend.repository.CronogramaEquipeRepository.class);
        armazenamento = mock(ArmazenamentoService.class);
        service = new RelatorioFotograficoServiceImpl(repository, autorizacao, armazenamento, pessoas, equipes);
        logar(7L, Acao.ESCRITA);
    }

    private void logar(Long id, Acao acao) {
        UsuarioAutenticado u = new UsuarioAutenticado(id, "u" + id + "@obra.com", "USUARIO",
                java.util.Collections.singletonMap(Modulo.RELATORIO_FOTOGRAFICO, acao), 0, java.time.Instant.now());
        when(autorizacao.exigir(any(), any())).thenReturn(u);
    }

    private static RelatorioFotograficoDto relatorio(Long id, String status) {
        RelatorioFotograficoDto r = new RelatorioFotograficoDto();
        r.setId(id);
        r.setIdAutor(7L);
        r.setInProprio(true);
        r.setCdStatus(status);
        r.setNrFotosPagina(2);
        r.setNrVersao(1);
        return r;
    }

    private static RelatorioModeloDto modelo(boolean ativo) {
        RelatorioModeloDto m = new RelatorioModeloDto();
        m.setId(3L);
        m.setNmModelo("Vistoria de fachada");
        m.setNrFotosPagina(4);
        m.setCdLayout("R4");
        m.setInAtivo(ativo);
        return m;
    }

    private static RelatorioModeloItemDto itemModelo(int ordem, String nome) {
        RelatorioModeloItemDto i = new RelatorioModeloItemDto();
        i.setIdModelo(3L);
        i.setNrOrdem(ordem);
        i.setNmItem(nome);
        return i;
    }

    private static RelatorioItemForm itemForm(Long id, String nome) {
        RelatorioItemForm f = new RelatorioItemForm();
        f.setId(id);
        f.setNmItem(nome);
        return f;
    }

    private static RelatorioItemDto item(Long id) {
        RelatorioItemDto i = new RelatorioItemDto();
        i.setId(id);
        i.setIdRelatorio(1L);
        return i;
    }

    private static RelatorioFotoDto foto(Long id, Long idItem, Long idArquivo) {
        RelatorioFotoDto f = new RelatorioFotoDto();
        f.setId(id);
        f.setIdItem(idItem);
        f.setIdArquivo(idArquivo);
        f.setIdRelatorio(1L);
        f.setNrOrdem(1);
        return f;
    }

    private RelatorioFotograficoForm form(String titulo) {
        RelatorioFotograficoForm f = new RelatorioFotograficoForm();
        f.setNmTitulo(titulo);
        f.setDtRelatorio(LocalDate.of(2026, 10, 21));
        return f;
    }

    @Test
    void modeloExigeAdminDoModulo() {
        when(autorizacao.exigir(Modulo.RELATORIO_FOTOGRAFICO, Acao.ADMIN)).thenThrow(new AcessoNegadoException("negado"));
        RelatorioModeloForm f = new RelatorioModeloForm();
        f.setNmModelo("X");
        f.setNrFotosPagina(2);
        assertThrows(AcessoNegadoException.class, () -> service.criarModelo(f));
        verify(repository, never()).inserirModelo(any());
    }

    @Test
    void modeloRecusaLayoutInvalido() {
        RelatorioModeloForm f = new RelatorioModeloForm();
        f.setNmModelo("X");
        f.setNrFotosPagina(3);
        assertThrows(EntradaInvalidaException.class, () -> service.criarModelo(f));
    }

    @Test
    @SuppressWarnings("unchecked")
    void modeloGravaItensNaOrdemDaLista() {
        RelatorioModeloForm f = new RelatorioModeloForm();
        f.setNmModelo(" Fachada ");
        f.setNrFotosPagina(4);
        f.setItens(Arrays.asList(itemForm(null, "Norte"), itemForm(null, "Sul")));
        when(repository.inserirModelo(any())).thenReturn(3L);
        when(repository.listarModelos()).thenReturn(Collections.singletonList(modelo(true)));
        service.criarModelo(f);
        ArgumentCaptor<List<RelatorioModeloItemDto>> itens = ArgumentCaptor.forClass(List.class);
        verify(repository).substituirItensDoModelo(eq(3L), itens.capture());
        assertEquals(Arrays.asList("Norte", "Sul"), Arrays.asList(itens.getValue().get(0).getNmItem(), itens.getValue().get(1).getNmItem()));
        assertEquals(2, itens.getValue().get(1).getNrOrdem());
    }

    @Test
    void criarComModeloCopiaItensELayoutNaFaseDoContexto() {
        when(repository.listarModelos()).thenReturn(Collections.singletonList(modelo(true)));
        when(repository.listarItensDeModelos()).thenReturn(Arrays.asList(itemModelo(1, "Norte"), itemModelo(2, "Sul")));
        when(repository.inserir(any(), eq(null))).thenReturn(1L);
        when(repository.buscar(1L, 7L, false)).thenReturn(Optional.of(relatorio(1L, "RASCUNHO")));
        RelatorioFotograficoForm f = form("Fachada bloco A");
        f.setIdModelo(3L);
        service.criar(f);
        ArgumentCaptor<RelatorioFotograficoDto> r = ArgumentCaptor.forClass(RelatorioFotograficoDto.class);
        verify(repository).inserir(r.capture(), eq(null));
        assertEquals(4, r.getValue().getNrFotosPagina());
        assertEquals("R4", r.getValue().getCdLayout());
        assertEquals(7L, r.getValue().getIdAutor());
        verify(repository, times(2)).inserirItem(any());
    }

    @Test
    void criarComModeloDesativadoFalha() {
        when(repository.listarModelos()).thenReturn(Collections.singletonList(modelo(false)));
        RelatorioFotograficoForm f = form("X");
        f.setIdModelo(3L);
        assertThrows(EntradaInvalidaException.class, () -> service.criar(f));
        verify(repository, never()).inserir(any(), any());
    }

    @Test
    void relatorioConcluidoNaoAceitaEdicaoNemFoto() {
        when(repository.buscar(1L, 7L, false)).thenReturn(Optional.of(relatorio(1L, "CONCLUIDO")));
        RelatorioFotograficoForm f = form("X");
        f.setNrVersao(1);
        assertThrows(NegocioException.class, () -> service.alterar(1L, f));
        when(repository.relatorioDoItem(5L)).thenReturn(Optional.of(1L));
        assertThrows(NegocioException.class, () -> service.adicionarFoto(5L, "a.jpg", JPEG, null));
        verify(armazenamento, never()).salvar(anyString(), anyString(), any());
    }

    @Test
    void edicaoExigeVersaoEConflitoVira409() {
        assertThrows(EntradaInvalidaException.class, () -> service.alterar(1L, form("X")));
        when(repository.buscar(1L, 7L, false)).thenReturn(Optional.of(relatorio(1L, "RASCUNHO")));
        when(repository.alterar(any(), anyInt())).thenReturn(0);
        RelatorioFotograficoForm f = form("X");
        f.setNrVersao(1);
        assertThrows(ConflitoException.class, () -> service.alterar(1L, f));
    }

    @Test
    void itensQueSaemDaListaSaoExcluidosComOsArquivos() {
        when(repository.buscar(1L, 7L, false)).thenReturn(Optional.of(relatorio(1L, "RASCUNHO")));
        when(repository.alterar(any(), anyInt())).thenReturn(1);
        when(repository.listarItens(1L)).thenReturn(Arrays.asList(item(20L), item(21L)));
        when(repository.listarFotos(1L)).thenReturn(Arrays.asList(foto(90L, 21L, 500L), foto(91L, 20L, 501L)));
        RelatorioFotograficoForm f = form("X");
        f.setNrVersao(1);
        f.setItens(new ArrayList<>(Arrays.asList(itemForm(20L, "Fica"), itemForm(null, "Novo"))));
        service.alterar(1L, f);
        verify(repository).deletarItem(21L, 1L);
        verify(repository, never()).deletarItem(eq(20L), anyLong());
        verify(repository).alterarItem(any());
        verify(repository).inserirItem(any());
        verify(armazenamento).apagar(500L);
        verify(armazenamento, never()).apagar(501L);
    }

    @Test
    void itemDeOutroRelatorioERecusado() {
        when(repository.buscar(1L, 7L, false)).thenReturn(Optional.of(relatorio(1L, "RASCUNHO")));
        when(repository.alterar(any(), anyInt())).thenReturn(1);
        when(repository.listarItens(1L)).thenReturn(Collections.singletonList(item(20L)));
        RelatorioFotograficoForm f = form("X");
        f.setNrVersao(1);
        f.setItens(Collections.singletonList(itemForm(99L, "Intruso")));
        assertThrows(EntradaInvalidaException.class, () -> service.alterar(1L, f));
        verify(repository, never()).deletarItem(anyLong(), anyLong());
    }

    @Test
    void fotoValidaImagemENaoMexeNaVersao() {
        when(repository.relatorioDoItem(5L)).thenReturn(Optional.of(1L));
        when(repository.buscar(1L, 7L, false)).thenReturn(Optional.of(relatorio(1L, "RASCUNHO")));
        assertThrows(EntradaInvalidaException.class, () -> service.adicionarFoto(5L, "a.txt", "texto".getBytes(), null));
        when(armazenamento.salvar("a.jpg", "image/jpeg", JPEG)).thenReturn(500L);
        when(repository.inserirFoto(5L, 500L, "Fachada norte")).thenReturn(90L);
        when(repository.buscarFoto(90L)).thenReturn(Optional.of(foto(90L, 5L, 500L)));
        service.adicionarFoto(5L, "a.jpg", JPEG, " Fachada norte ");
        verify(repository).inserirFoto(5L, 500L, "Fachada norte");
        verify(repository, never()).alterar(any(), anyInt());
    }

    @Test
    void fotoForaDoEscopoNaoAparece() {
        when(repository.buscarFoto(90L)).thenReturn(Optional.of(foto(90L, 5L, 500L)));
        when(repository.buscar(1L, 7L, false)).thenReturn(Optional.empty());
        assertThrows(NaoEncontradoException.class, () -> service.baixarFoto(90L));
        assertThrows(NaoEncontradoException.class, () -> service.alterarFoto(90L, new RelatorioFotoForm()));
        verify(armazenamento, never()).abrir(anyLong());
    }

    @Test
    void excluirFotoApagaArquivo() {
        when(repository.buscarFoto(90L)).thenReturn(Optional.of(foto(90L, 5L, 500L)));
        when(repository.buscar(1L, 7L, false)).thenReturn(Optional.of(relatorio(1L, "RASCUNHO")));
        service.excluirFoto(90L);
        verify(repository).deletarFoto(90L);
        verify(armazenamento).apagar(500L);
    }

    @Test
    void statusSoAceitaRascunhoOuConcluido() {
        RelatorioStatusForm f = new RelatorioStatusForm();
        f.setCdStatus("APROVADO");
        f.setNrVersao(1);
        assertThrows(EntradaInvalidaException.class, () -> service.alterarStatus(1L, f));
        when(repository.buscar(1L, 7L, false)).thenReturn(Optional.of(relatorio(1L, "RASCUNHO")));
        when(repository.alterarStatus(1L, "CONCLUIDO", 1)).thenReturn(1);
        f.setCdStatus("concluido");
        service.alterarStatus(1L, f);
        verify(repository).alterarStatus(1L, "CONCLUIDO", 1);
    }

    @Test
    void excluirRelatorioApagaTodasAsFotos() {
        when(repository.buscar(1L, 7L, false)).thenReturn(Optional.of(relatorio(1L, "CONCLUIDO")));
        when(repository.listarFotos(1L)).thenReturn(Arrays.asList(foto(90L, 5L, 500L), foto(91L, 6L, 501L)));
        when(repository.deletar(1L, 1)).thenReturn(1);
        service.excluir(1L, 1);
        verify(armazenamento).apagar(500L);
        verify(armazenamento).apagar(501L);
    }

    @Test
    void layoutPorCodigoDefineQuantidadeDeFotos() {
        when(repository.inserir(any(), eq(null))).thenReturn(1L);
        when(repository.buscar(1L, 7L, false)).thenReturn(Optional.of(relatorio(1L, "RASCUNHO")));
        RelatorioFotograficoForm f = form("Paisagem");
        f.setCdLayout("p3");
        service.criar(f);
        ArgumentCaptor<RelatorioFotograficoDto> r = ArgumentCaptor.forClass(RelatorioFotograficoDto.class);
        verify(repository).inserir(r.capture(), eq(null));
        assertEquals("P3", r.getValue().getCdLayout());
        assertEquals(3, r.getValue().getNrFotosPagina());
        RelatorioFotograficoForm ruim = form("X");
        ruim.setCdLayout("Z9");
        assertThrows(EntradaInvalidaException.class, () -> service.criar(ruim));
    }

    @Test
    void modeloValidaFonteEOpacidade() {
        RelatorioModeloForm f = new RelatorioModeloForm();
        f.setNmModelo("X");
        f.setCdLayout("R3L");
        f.setCdFonte("comic");
        assertThrows(EntradaInvalidaException.class, () -> service.criarModelo(f));
        f.setCdFonte("times");
        f.setNrOpacidadeFundo(90);
        assertThrows(EntradaInvalidaException.class, () -> service.criarModelo(f));
        f.setNrOpacidadeFundo(20);
        when(repository.inserirModelo(any())).thenReturn(3L);
        when(repository.listarModelos()).thenReturn(Collections.singletonList(modelo(true)));
        service.criarModelo(f);
        ArgumentCaptor<RelatorioModeloDto> m = ArgumentCaptor.forClass(RelatorioModeloDto.class);
        verify(repository).inserirModelo(m.capture());
        assertEquals("TIMES", m.getValue().getCdFonte());
        assertEquals(3, m.getValue().getNrFotosPagina());
        assertEquals(Boolean.TRUE, m.getValue().getInCabecalho());
    }

    @Test
    void trocarImagemDaFotoApagaOArquivoAntigo() {
        when(repository.buscarFoto(90L)).thenReturn(Optional.of(foto(90L, 5L, 500L)));
        when(repository.buscar(1L, 7L, false)).thenReturn(Optional.of(relatorio(1L, "RASCUNHO")));
        when(armazenamento.salvar("a.jpg", "image/jpeg", JPEG)).thenReturn(600L);
        service.trocarImagemFoto(90L, "a.jpg", JPEG);
        verify(repository).trocarImagemFoto(90L, 600L);
        verify(armazenamento).apagar(500L);
        when(repository.buscar(1L, 7L, false)).thenReturn(Optional.of(relatorio(1L, "CONCLUIDO")));
        assertThrows(NegocioException.class, () -> service.trocarImagemFoto(90L, "a.jpg", JPEG));
    }

    @Test
    void imagemDoModeloSubstituiARemoveETipoEValidado() {
        RelatorioModeloDto m = modelo(true);
        m.setIdArquivoIcone(700L);
        when(repository.listarModelos()).thenReturn(Collections.singletonList(m));
        when(armazenamento.salvar("logo.jpg", "image/jpeg", JPEG)).thenReturn(800L);
        service.salvarImagemModelo(3L, "icone", "logo.jpg", JPEG);
        verify(repository).alterarImagemModelo(3L, false, 800L);
        verify(armazenamento).apagar(700L);
        assertThrows(EntradaInvalidaException.class, () -> service.salvarImagemModelo(3L, "capa", "logo.jpg", JPEG));
        assertThrows(NaoEncontradoException.class, () -> service.baixarImagemModelo(3L, "fundo"));
        service.removerImagemModelo(3L, "icone");
        verify(repository).alterarImagemModelo(3L, false, null);
    }

    // ---------- v2: por usuário, administrador vê tudo e compartilhamento ----------

    private static RelatorioFotograficoDto deOutro(Long id) {
        RelatorioFotograficoDto r = relatorio(id, "RASCUNHO");
        r.setIdAutor(9L);
        r.setInProprio(false);
        return r;
    }

    @Test
    void listaOsDaPessoaEOsCompartilhadosEOAdminVeTudo() {
        service.listar();
        verify(repository).listar(7L, false);
        logar(8L, Acao.ADMIN);
        service.listar();
        verify(repository).listar(8L, true);
    }

    @Test
    void relatorioCompartilhadoComigoESoParaVer() {
        when(repository.buscar(1L, 7L, false)).thenReturn(Optional.of(deOutro(1L)));
        assertEquals(1L, service.detalhar(1L).getId());
        RelatorioFotograficoForm f = new RelatorioFotograficoForm();
        f.setNmTitulo("Outro título");
        f.setDtRelatorio(java.time.LocalDate.of(2026, 11, 5));
        f.setNrVersao(1);
        assertThrows(br.com.nord_tool_backend.exception.AcessoNegadoException.class, () -> service.alterar(1L, f));
        assertThrows(br.com.nord_tool_backend.exception.AcessoNegadoException.class, () -> service.excluir(1L, 1));
        assertThrows(br.com.nord_tool_backend.exception.AcessoNegadoException.class, () -> service.compartilhar(1L, Collections.singletonList(3L)));
        verify(repository, never()).alterar(any(), anyInt());
        verify(repository, never()).deletar(any(), anyInt());
        verify(repository, never()).compartilhar(any(), any());
    }

    @Test
    void adminAlteraOuExcluiORelatorioDeOutraPessoa() {
        logar(8L, Acao.ADMIN);
        when(repository.buscar(1L, 8L, true)).thenReturn(Optional.of(deOutro(1L)));
        when(repository.deletar(1L, 1)).thenReturn(1);
        service.excluir(1L, 1);
        verify(repository).deletar(1L, 1);
    }

    @Test
    void relatorioQueNaoEnxergoNaoExiste() {
        when(repository.buscar(1L, 7L, false)).thenReturn(Optional.empty());
        assertThrows(NaoEncontradoException.class, () -> service.detalhar(1L));
    }

    @Test
    void compartilhaSemOAutorSemRepetirESoComQuemEstaAtivo() {
        when(repository.buscar(1L, 7L, false)).thenReturn(Optional.of(relatorio(1L, "RASCUNHO")));
        when(pessoas.contarUsuariosAtivos(any())).thenReturn(2);
        service.compartilhar(1L, Arrays.asList(3L, 7L, 3L, null, 4L));
        verify(repository).compartilhar(1L, Arrays.asList(3L, 4L));

        when(pessoas.contarUsuariosAtivos(any())).thenReturn(1);
        assertThrows(EntradaInvalidaException.class, () -> service.compartilhar(1L, Arrays.asList(3L, 4L)));

        service.compartilhar(1L, Collections.emptyList());
        verify(repository).compartilhar(1L, Collections.emptyList());
    }

    @Test
    void limiteDeCompartilhamentos() {
        when(repository.buscar(1L, 7L, false)).thenReturn(Optional.of(relatorio(1L, "RASCUNHO")));
        List<Long> muitos = new ArrayList<>();
        for (long i = 100; i < 100 + RelatorioFotograficoServiceImpl.MAX_COMPARTILHAMENTOS + 1; i++) muitos.add(i);
        assertThrows(EntradaInvalidaException.class, () -> service.compartilhar(1L, muitos));
    }

    @Test
    void pessoasEEquipesParaCompartilhar() {
        br.com.nord_tool_backend.domain.CronogramaEquipe e = new br.com.nord_tool_backend.domain.CronogramaEquipe();
        e.setId(5L);
        e.setNmEquipe("Acabamento");
        e.setTxMembros("3,4");
        when(equipes.listar(null, 7L, false)).thenReturn(Collections.singletonList(e));
        assertEquals(Arrays.asList(3L, 4L), service.equipes().get(0).getIdsMembros());
        service.pessoas();
        verify(pessoas).listarPessoas();
    }

    @Test
    void idsCompartilhadosVemDoTexto() {
        RelatorioFotograficoDto r = new RelatorioFotograficoDto();
        assertTrue(r.getIdsCompartilhados().isEmpty());
        r.setTxCompartilhados("3, 9");
        assertEquals(Arrays.asList(3L, 9L), r.getIdsCompartilhados());
    }

    // ---------- v3: trocar o modelo ----------

    private static RelatorioTrocaModeloForm troca(Long idModelo, boolean itens, boolean layout) {
        RelatorioTrocaModeloForm f = new RelatorioTrocaModeloForm();
        f.setIdModelo(idModelo);
        f.setNrVersao(1);
        f.setInAdicionarItens(itens);
        f.setInUsarLayout(layout);
        return f;
    }

    private static RelatorioItemDto itemComNome(Long id, int ordem, String nome) {
        RelatorioItemDto i = item(id);
        i.setNrOrdem(ordem);
        i.setNmItem(nome);
        return i;
    }

    @Test
    void trocarModeloAcrescentaOsItensQueFaltamEUsaOLayout() {
        RelatorioFotograficoDto r = relatorio(1L, "RASCUNHO");
        r.setCdLayout("R2");
        when(repository.buscar(1L, 7L, false)).thenReturn(Optional.of(r));
        when(repository.listarModelos()).thenReturn(Collections.singletonList(modelo(true)));
        when(repository.listarItensDeModelos()).thenReturn(Arrays.asList(itemModelo(1, " norte "), itemModelo(2, "Sul"), itemModelo(3, "Leste")));
        when(repository.listarItens(1L)).thenReturn(Arrays.asList(itemComNome(10L, 1, "Norte"), itemComNome(11L, 4, "Cobertura")));
        when(repository.trocarModelo(1L, 3L, "R4", 4, 1)).thenReturn(1);

        service.trocarModelo(1L, troca(3L, true, true));

        verify(repository).trocarModelo(1L, 3L, "R4", 4, 1);
        org.mockito.ArgumentCaptor<RelatorioItemDto> novos = org.mockito.ArgumentCaptor.forClass(RelatorioItemDto.class);
        verify(repository, times(2)).inserirItem(novos.capture());
        assertEquals(Arrays.asList("Sul", "Leste"), novos.getAllValues().stream().map(RelatorioItemDto::getNmItem).collect(java.util.stream.Collectors.toList()));
        assertEquals(Arrays.asList(5, 6), novos.getAllValues().stream().map(RelatorioItemDto::getNrOrdem).collect(java.util.stream.Collectors.toList()));
        verify(repository, never()).deletarItem(anyLong(), anyLong());
    }

    @Test
    void trocarModeloSemItensNemLayoutMantemOLayoutAtual() {
        RelatorioFotograficoDto r = relatorio(1L, "RASCUNHO");
        r.setCdLayout("R6");
        when(repository.buscar(1L, 7L, false)).thenReturn(Optional.of(r));
        when(repository.listarModelos()).thenReturn(Collections.singletonList(modelo(true)));
        when(repository.trocarModelo(1L, 3L, "R6", 6, 1)).thenReturn(1);

        service.trocarModelo(1L, troca(3L, false, false));

        verify(repository).trocarModelo(1L, 3L, "R6", 6, 1);
        verify(repository, never()).inserirItem(any());
    }

    @Test
    void tirarOModeloDeixaSemModelo() {
        RelatorioFotograficoDto r = relatorio(1L, "RASCUNHO");
        r.setIdModelo(3L);
        r.setCdLayout("R4");
        when(repository.buscar(1L, 7L, false)).thenReturn(Optional.of(r));
        when(repository.trocarModelo(1L, null, "R4", 4, 1)).thenReturn(1);

        service.trocarModelo(1L, troca(null, true, true));

        verify(repository).trocarModelo(1L, null, "R4", 4, 1);
        verify(repository, never()).listarModelos();
        verify(repository, never()).inserirItem(any());
    }

    @Test
    void trocarModeloRecusaDesativadoConcluidoEVersaoVelha() {
        RelatorioFotograficoDto r = relatorio(1L, "RASCUNHO");
        when(repository.buscar(1L, 7L, false)).thenReturn(Optional.of(r));
        when(repository.listarModelos()).thenReturn(Collections.singletonList(modelo(false)));
        assertThrows(EntradaInvalidaException.class, () -> service.trocarModelo(1L, troca(3L, false, false)));

        r.setIdModelo(3L);
        r.setCdLayout("R2");
        when(repository.trocarModelo(1L, 3L, "R2", 2, 1)).thenReturn(0);
        assertThrows(ConflitoException.class, () -> service.trocarModelo(1L, troca(3L, false, false)), "desativado vale se já era o dele");

        when(repository.buscar(2L, 7L, false)).thenReturn(Optional.of(relatorio(2L, "CONCLUIDO")));
        assertThrows(NegocioException.class, () -> service.trocarModelo(2L, troca(null, false, false)));

        RelatorioTrocaModeloForm semVersao = troca(null, false, false);
        semVersao.setNrVersao(null);
        assertThrows(EntradaInvalidaException.class, () -> service.trocarModelo(1L, semVersao));
    }

    @Test
    void trocarModeloRespeitaOLimiteDeItens() {
        RelatorioFotograficoDto r = relatorio(1L, "RASCUNHO");
        r.setCdLayout("R2");
        when(repository.buscar(1L, 7L, false)).thenReturn(Optional.of(r));
        when(repository.listarModelos()).thenReturn(Collections.singletonList(modelo(true)));
        when(repository.listarItensDeModelos()).thenReturn(Arrays.asList(itemModelo(1, "Novo A"), itemModelo(2, "Novo B")));
        List<RelatorioItemDto> cheios = new java.util.ArrayList<>();
        for (int i = 0; i < RelatorioFotograficoServiceImpl.MAX_ITENS - 1; i++) cheios.add(itemComNome((long) i, i + 1, "Item " + i));
        when(repository.listarItens(1L)).thenReturn(cheios);
        when(repository.trocarModelo(eq(1L), eq(3L), anyString(), any(), anyInt())).thenReturn(1);

        service.trocarModelo(1L, troca(3L, true, false));

        verify(repository, times(1)).inserirItem(any());
        assertEquals("", RelatorioFotograficoServiceImpl.chaveDoItem(null));
        assertEquals("fachada norte", RelatorioFotograficoServiceImpl.chaveDoItem("  Fachada   NORTE "));
    }
}
