package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.dto.SacChamadoDto;
import br.com.nord_tool_backend.dto.SacImagemDto;
import br.com.nord_tool_backend.exception.AcessoNegadoException;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.exception.NaoEncontradoException;
import br.com.nord_tool_backend.exception.NegocioException;
import br.com.nord_tool_backend.repository.SacRepository;
import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.security.UsuarioAutenticado;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.service.SacService;
import br.com.nord_tool_backend.storage.ArmazenamentoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SacServiceImplTest {

    /** PNG mínimo válido (assinatura + IHDR) para passar no validador de imagem. */
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D, 'I', 'H', 'D', 'R', 0, 0, 0, 1, 0, 0, 0, 1, 8, 2, 0, 0, 0};

    private SacRepository repository;
    private AutorizacaoService autorizacao;
    private ArmazenamentoService armazenamento;
    private SacServiceImpl service;
    private final UsuarioAutenticado pessoa = new UsuarioAutenticado(5L, "p@obra.com", "COLABORADOR", Collections.emptyMap(), 0, Instant.now());
    private final UsuarioAutenticado adm = new UsuarioAutenticado(1L, "a@obra.com", "ADMIN", Collections.singletonMap(Modulo.SAC, Acao.ADMIN), 0, Instant.now());

    @BeforeEach
    void preparar() {
        repository = mock(SacRepository.class);
        autorizacao = mock(AutorizacaoService.class);
        armazenamento = mock(ArmazenamentoService.class);
        service = new SacServiceImpl(repository, autorizacao, armazenamento);
        when(repository.imagens(any())).thenReturn(Collections.emptyList());
    }

    private static SacChamadoDto chamado(long id, long dono, String status) {
        SacChamadoDto c = new SacChamadoDto();
        c.setId(id);
        c.setIdUsuario(dono);
        c.setCdStatus(status);
        return c;
    }

    @Test
    void qualquerPessoaLogadaAbreChamadoComPrints() {
        when(autorizacao.exigirAutenticado()).thenReturn(pessoa);
        when(repository.inserir(eq(5L), eq("Botão novo"), eq("Quero um botão"), eq("/gestao/financeiro"))).thenReturn(10L);
        when(armazenamento.salvar(any(), any(), any())).thenReturn(100L, 101L);
        when(repository.buscar(10L)).thenReturn(Optional.of(chamado(10, 5, "ABERTO")));

        service.abrir("  Botão novo ", "Quero um botão", "/gestao/financeiro",
                Arrays.asList(new SacService.Anexo("a.png", PNG), new SacService.Anexo("b.png", PNG)));

        verify(repository).inserirImagem(10L, 100L, 1);
        verify(repository).inserirImagem(10L, 101L, 2);
    }

    @Test
    void chamadoSemTituloOuComPrintsDemaisNaoEntra() {
        when(autorizacao.exigirAutenticado()).thenReturn(pessoa);
        assertThrows(EntradaInvalidaException.class, () -> service.abrir(" ", "x", null, Collections.emptyList()));
        List<SacService.Anexo> muitos = Collections.nCopies(7, new SacService.Anexo("a.png", PNG));
        assertThrows(EntradaInvalidaException.class, () -> service.abrir("t", "x", null, muitos));
        verify(repository, never()).inserir(anyLong(), any(), any(), any());
    }

    @Test
    void pessoaVeSoOsDelaEOAdmVeTodos() {
        when(autorizacao.exigirAutenticado()).thenReturn(pessoa);
        when(autorizacao.exigir(Modulo.SAC, Acao.ADMIN)).thenReturn(adm);
        service.listar(false);
        verify(repository).listar(5L);
        service.listar(true);
        verify(repository).listar(isNull());
    }

    @Test
    void quemAbriuSoExcluiEnquantoAberto() {
        when(autorizacao.exigirAutenticado()).thenReturn(pessoa);
        when(repository.buscar(10L)).thenReturn(Optional.of(chamado(10, 5, "EM_ANDAMENTO")));
        assertThrows(NegocioException.class, () -> service.excluir(10L));
        when(repository.buscar(11L)).thenReturn(Optional.of(chamado(11, 9, "ABERTO")));
        assertThrows(NaoEncontradoException.class, () -> service.excluir(11L));
        when(repository.buscar(12L)).thenReturn(Optional.of(chamado(12, 5, "ABERTO")));
        when(repository.arquivos(12L)).thenReturn(Collections.singletonList(300L));
        service.excluir(12L);
        verify(repository).excluir(12L);
        verify(armazenamento).apagar(300L);
    }

    @Test
    void printDeOutraPessoaSoParaOAdm() {
        SacImagemDto img = new SacImagemDto();
        img.setId(7L);
        img.setIdChamado(10L);
        img.setIdArquivo(70L);
        img.setIdUsuario(9L);
        when(repository.imagem(7L)).thenReturn(Optional.of(img));
        when(autorizacao.exigirAutenticado()).thenReturn(pessoa);
        assertThrows(AcessoNegadoException.class, () -> service.baixarImagem(7L));
        when(autorizacao.exigirAutenticado()).thenReturn(adm);
        assertEquals(70L, service.baixarImagem(7L).getVersao());
        verify(armazenamento, times(1)).abrir(70L);
        verify(repository, never()).inserirImagem(anyLong(), anyLong(), anyInt());
    }

    @Test
    void novidadesDaPessoaEAbertosSoParaOAdm() {
        when(autorizacao.exigirAutenticado()).thenReturn(pessoa);
        when(repository.novidades(5L)).thenReturn(Collections.singletonList(chamado(10, 5, "RESOLVIDO")));
        assertEquals(1, service.novidades().getChamados().size());
        assertEquals(null, service.novidades().getAbertos());
        verify(repository, never()).abertos();
        when(autorizacao.exigirAutenticado()).thenReturn(adm);
        when(repository.abertos()).thenReturn(3);
        assertEquals(Integer.valueOf(3), service.novidades().getAbertos());
    }

    @Test
    void marcarVistoSoOsDaPropriaPessoa() {
        when(autorizacao.exigirAutenticado()).thenReturn(pessoa);
        service.marcarVistos(Arrays.asList(10L, 11L));
        verify(repository).marcarVistos(5L, Arrays.asList(10L, 11L));
        service.marcarVistos(Collections.emptyList());
        verify(repository, times(1)).marcarVistos(anyLong(), any());
    }
}
