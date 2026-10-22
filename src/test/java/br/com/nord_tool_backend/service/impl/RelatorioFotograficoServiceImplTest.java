package br.com.nord_tool_backend.service.impl;

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
import br.com.nord_tool_backend.repository.RelatorioFotograficoRepository;
import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.security.UsuarioAutenticado;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.service.ContextoFaseService;
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
    private ContextoFaseService contexto;
    private ArmazenamentoService armazenamento;
    private RelatorioFotograficoServiceImpl service;

    @BeforeEach
    void setUp() {
        autorizacao = mock(AutorizacaoService.class);
        repository = mock(RelatorioFotograficoRepository.class);
        contexto = mock(ContextoFaseService.class);
        armazenamento = mock(ArmazenamentoService.class);
        service = new RelatorioFotograficoServiceImpl(repository, autorizacao, contexto, armazenamento);
        UsuarioAutenticado u = mock(UsuarioAutenticado.class);
        when(u.getId()).thenReturn(7L);
        when(autorizacao.exigir(any(), any())).thenReturn(u);
        when(contexto.faseParaCriar(null)).thenReturn(11L);
    }

    private static RelatorioFotograficoDto relatorio(Long id, String status) {
        RelatorioFotograficoDto r = new RelatorioFotograficoDto();
        r.setId(id);
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
        when(repository.inserir(any(), eq(11L))).thenReturn(1L);
        when(repository.buscar(1L)).thenReturn(Optional.of(relatorio(1L, "RASCUNHO")));
        RelatorioFotograficoForm f = form("Fachada bloco A");
        f.setIdModelo(3L);
        service.criar(f);
        ArgumentCaptor<RelatorioFotograficoDto> r = ArgumentCaptor.forClass(RelatorioFotograficoDto.class);
        verify(repository).inserir(r.capture(), eq(11L));
        assertEquals(4, r.getValue().getNrFotosPagina());
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
        when(repository.buscar(1L)).thenReturn(Optional.of(relatorio(1L, "CONCLUIDO")));
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
        when(repository.buscar(1L)).thenReturn(Optional.of(relatorio(1L, "RASCUNHO")));
        when(repository.alterar(any(), anyInt())).thenReturn(0);
        RelatorioFotograficoForm f = form("X");
        f.setNrVersao(1);
        assertThrows(ConflitoException.class, () -> service.alterar(1L, f));
    }

    @Test
    void itensQueSaemDaListaSaoExcluidosComOsArquivos() {
        when(repository.buscar(1L)).thenReturn(Optional.of(relatorio(1L, "RASCUNHO")));
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
        when(repository.buscar(1L)).thenReturn(Optional.of(relatorio(1L, "RASCUNHO")));
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
        when(repository.buscar(1L)).thenReturn(Optional.of(relatorio(1L, "RASCUNHO")));
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
        when(repository.buscar(1L)).thenReturn(Optional.empty());
        assertThrows(NaoEncontradoException.class, () -> service.baixarFoto(90L));
        assertThrows(NaoEncontradoException.class, () -> service.alterarFoto(90L, new RelatorioFotoForm()));
        verify(armazenamento, never()).abrir(anyLong());
    }

    @Test
    void excluirFotoApagaArquivo() {
        when(repository.buscarFoto(90L)).thenReturn(Optional.of(foto(90L, 5L, 500L)));
        when(repository.buscar(1L)).thenReturn(Optional.of(relatorio(1L, "RASCUNHO")));
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
        when(repository.buscar(1L)).thenReturn(Optional.of(relatorio(1L, "RASCUNHO")));
        when(repository.alterarStatus(1L, "CONCLUIDO", 1)).thenReturn(1);
        f.setCdStatus("concluido");
        service.alterarStatus(1L, f);
        verify(repository).alterarStatus(1L, "CONCLUIDO", 1);
    }

    @Test
    void excluirRelatorioApagaTodasAsFotos() {
        when(repository.buscar(1L)).thenReturn(Optional.of(relatorio(1L, "CONCLUIDO")));
        when(repository.listarFotos(1L)).thenReturn(Arrays.asList(foto(90L, 5L, 500L), foto(91L, 6L, 501L)));
        when(repository.deletar(1L, 1)).thenReturn(1);
        service.excluir(1L, 1);
        verify(armazenamento).apagar(500L);
        verify(armazenamento).apagar(501L);
    }
}
