package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.dto.CasamentoArteDto;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.exception.NaoEncontradoException;
import br.com.nord_tool_backend.exception.NegocioException;
import br.com.nord_tool_backend.form.CasamentoArteForm;
import br.com.nord_tool_backend.repository.CasamentoArteRepository;
import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.security.UsuarioAutenticado;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.storage.ArmazenamentoService;
import br.com.nord_tool_backend.storage.ArquivoConteudo;
import br.com.nord_tool_backend.storage.ArquivoDownload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CasamentoArteServiceImplTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};
    private static final byte[] PDF = {'%', 'P', 'D', 'F', '-', '1', '.', '7'};

    private CasamentoArteRepository repository;
    private ArmazenamentoService armazenamento;
    private AutorizacaoService autorizacao;
    private CasamentoArteServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(CasamentoArteRepository.class);
        armazenamento = mock(ArmazenamentoService.class);
        autorizacao = mock(AutorizacaoService.class);
        when(autorizacao.exigir(any(), any())).thenReturn(new UsuarioAutenticado(4L, "t@obra.com", "USUARIO", Collections.emptyMap(), 0, Instant.now()));
        service = new CasamentoArteServiceImpl(repository, armazenamento, autorizacao);
    }

    private static CasamentoArteDto arte(Long id, Long idArquivo, String categoria) {
        CasamentoArteDto a = new CasamentoArteDto();
        a.setIdArte(id);
        a.setIdArquivo(idArquivo);
        a.setNmTitulo("Convite");
        a.setCdCategoria(categoria);
        a.setDhCriacao(LocalDateTime.of(2026, 11, 24, 10, 0));
        return a;
    }

    @Test
    void listaComLeitura() {
        when(repository.listar()).thenReturn(Collections.singletonList(arte(1L, 9L, "CONVITE")));
        assertEquals(1, service.listar().size());
        verify(autorizacao).exigir(Modulo.CASAMENTO, Acao.LEITURA);
    }

    @Test
    void enviaImagemOuPdfComTituloECategoria() {
        when(armazenamento.salvar("convite.png", "image/png", PNG)).thenReturn(9L);
        when(repository.inserir(9L, "Convite final", "CONVITE", 4L)).thenReturn(1L);
        when(repository.buscar(1L)).thenReturn(Optional.of(arte(1L, 9L, "CONVITE")));
        assertEquals(1L, service.enviar("convite.png", PNG, "  Convite final ", "convite").getIdArte());
        verify(autorizacao).exigir(Modulo.CASAMENTO, Acao.ESCRITA);

        // Sem título: usa o nome do arquivo; sem categoria: OUTRO.
        when(armazenamento.salvar("Save the date.pdf", "application/pdf", PDF)).thenReturn(10L);
        when(repository.inserir(10L, "Save the date", "OUTRO", 4L)).thenReturn(2L);
        when(repository.buscar(2L)).thenReturn(Optional.of(arte(2L, 10L, "OUTRO")));
        assertEquals(2L, service.enviar("Save the date.pdf", PDF, " ", null).getIdArte());
        when(armazenamento.salvar("semextensao", "application/pdf", PDF)).thenReturn(11L);
        when(repository.inserir(11L, "semextensao", "OUTRO", 4L)).thenReturn(3L);
        when(repository.buscar(3L)).thenReturn(Optional.of(arte(3L, 11L, "OUTRO")));
        assertEquals(3L, service.enviar("semextensao", PDF, null, "").getIdArte());
    }

    @Test
    void recusaArquivoInvalidoCategoriaErradaELimite() {
        assertThrows(EntradaInvalidaException.class, () -> service.enviar("x.gif", new byte[]{'G', 'I', 'F', '8'}, "x", null));
        assertThrows(EntradaInvalidaException.class, () -> service.enviar("x.png", PNG, "x", "FESTA"));
        assertThrows(EntradaInvalidaException.class, () -> service.enviar("", PNG, "x", null));
        when(repository.contar()).thenReturn((long) CasamentoArteServiceImpl.MAX_ARTES);
        assertThrows(NegocioException.class, () -> service.enviar("x.png", PNG, "x", null));
        verify(armazenamento, never()).salvar(anyString(), anyString(), any());
    }

    @Test
    void atualizaTituloECategoria() {
        when(repository.buscar(1L)).thenReturn(Optional.of(arte(1L, 9L, "CONVITE")));
        CasamentoArteForm f = new CasamentoArteForm();
        f.setNmTitulo("Save the date oficial");
        f.setCdCategoria("save_the_date");
        service.atualizar(1L, f);
        verify(repository).atualizar(1L, "Save the date oficial", "SAVE_THE_DATE");
        f.setCdCategoria(null);
        f.setNmTitulo("Só o título");
        service.atualizar(1L, f);
        verify(repository).atualizar(1L, "Só o título", "CONVITE");
        f.setNmTitulo("  ");
        assertThrows(EntradaInvalidaException.class, () -> service.atualizar(1L, f));
        StringBuilder longo = new StringBuilder();
        for (int i = 0; i < 200; i++) longo.append('a');
        f.setNmTitulo(longo.toString());
        service.atualizar(1L, f);
        verify(repository).atualizar(1L, longo.substring(0, 150), "CONVITE");
        when(repository.buscar(2L)).thenReturn(Optional.empty());
        assertThrows(NaoEncontradoException.class, () -> service.atualizar(2L, f));
    }

    @Test
    void baixaEExcluiApagandoOArquivo() {
        CasamentoArteDto a = arte(1L, 9L, "CONVITE");
        when(repository.buscar(1L)).thenReturn(Optional.of(a));
        ArquivoConteudo conteudo = mock(ArquivoConteudo.class);
        when(armazenamento.abrir(9L)).thenReturn(conteudo);
        ArquivoDownload d = service.baixar(1L);
        assertSame(conteudo, d.getConteudo());
        assertEquals(a.getNrVersao(), d.getVersao());
        service.excluir(1L);
        verify(repository).deletar(1L);
        verify(armazenamento).apagar(9L);
        CasamentoArteDto semData = arte(5L, 1L, "OUTRO");
        semData.setDhCriacao(null);
        assertEquals(0L, semData.getNrVersao());
    }
}
