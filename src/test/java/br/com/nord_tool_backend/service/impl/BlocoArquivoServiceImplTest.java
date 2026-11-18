package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.dto.BlocoArquivoDto;
import br.com.nord_tool_backend.exception.ConflitoException;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.exception.NaoEncontradoException;
import br.com.nord_tool_backend.exception.NegocioException;
import br.com.nord_tool_backend.form.BlocoArquivoForm;
import br.com.nord_tool_backend.repository.BlocoArquivoRepository;
import br.com.nord_tool_backend.security.UsuarioAutenticado;
import br.com.nord_tool_backend.service.AutorizacaoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BlocoArquivoServiceImplTest {

    private static final Long EU = 7L;
    private BlocoArquivoRepository repository;
    private BlocoArquivoServiceImpl service;
    private AutorizacaoService autorizacao;

    @BeforeEach
    void setUp() {
        repository = mock(BlocoArquivoRepository.class);
        autorizacao = mock(AutorizacaoService.class);
        when(autorizacao.exigir(org.mockito.ArgumentMatchers.any(br.com.nord_tool_backend.security.Modulo.class), org.mockito.ArgumentMatchers.any(br.com.nord_tool_backend.security.Acao.class))).thenReturn(new UsuarioAutenticado(EU, "eu@obra.com", "USUARIO",
                Collections.emptyMap(), 0, Instant.now()));
        service = new BlocoArquivoServiceImpl(repository, autorizacao, new ObjectMapper());
        when(repository.buscar(1L, EU)).thenReturn(Optional.of(arquivo(1L, "PLANILHA")));
        when(repository.buscar(2L, EU)).thenReturn(Optional.of(arquivo(2L, "DOCUMENTO")));
    }

    private static BlocoArquivoDto arquivo(Long id, String tipo) {
        BlocoArquivoDto a = new BlocoArquivoDto();
        a.setId(id);
        a.setCdTipo(tipo);
        a.setNmTitulo("Orçamento");
        a.setCdCor("#16a34a");
        a.setNrVersao(1);
        a.setConteudo("[]");
        return a;
    }

    private static BlocoArquivoForm form(String tipo, String titulo, String conteudo, Integer versao) {
        BlocoArquivoForm f = new BlocoArquivoForm();
        f.setCdTipo(tipo);
        f.setNmTitulo(titulo);
        f.setConteudo(conteudo);
        f.setNrVersao(versao);
        return f;
    }

    @Test
    void listaEAbreSoOsMeus() {
        when(repository.listar(EU)).thenReturn(List.of(arquivo(1L, "PLANILHA")));
        assertEquals(1, service.listar().size());
        assertEquals("[]", service.abrir(1L).getConteudo());
        when(repository.buscar(9L, EU)).thenReturn(Optional.empty());
        assertThrows(NaoEncontradoException.class, () -> service.abrir(9L));
    }

    @Test
    void criaPlanilhaComCorPadraoENomeAparado() {
        when(repository.inserir(EU, "PLANILHA", "Orçamento", "#16a34a", "")).thenReturn(1L);
        assertEquals(1L, service.criar(form("PLANILHA", "  Orçamento ", null, null)).getId());
        when(repository.inserir(EU, "DOCUMENTO", "Ata", "#2563eb", "<p>oi</p>")).thenReturn(2L);
        assertEquals(2L, service.criar(form("DOCUMENTO", "Ata", "<p>oi</p>", null)).getId());
    }

    @Test
    void criarComTipoErradoNomeVazioOuNoLimite() {
        BlocoArquivoForm tipo = form("PDF", "x", null, null);
        assertThrows(EntradaInvalidaException.class, () -> service.criar(tipo));
        BlocoArquivoForm nome = form("PLANILHA", "   ", null, null);
        assertThrows(EntradaInvalidaException.class, () -> service.criar(nome));
        when(repository.contar(EU)).thenReturn(BlocoArquivoServiceImpl.MAX_ARQUIVOS);
        BlocoArquivoForm cheio = form("PLANILHA", "x", null, null);
        assertThrows(NegocioException.class, () -> service.criar(cheio));
        verify(repository, never()).inserir(any(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void salvaConteudoEVoltaSemOConteudo() {
        when(repository.alterar(1L, EU, "Orçamento", "#16a34a", "[{\"name\":\"Aba1\"}]", 1)).thenReturn(1);
        BlocoArquivoDto r = service.salvar(1L, form(null, null, "[{\"name\":\"Aba1\"}]", 1));
        assertNull(r.getConteudo());
    }

    @Test
    void renomearSemMexerNoConteudo() {
        when(repository.alterar(eq(1L), eq(EU), eq("Custos"), eq("#16a34a"), isNull(), eq(1))).thenReturn(1);
        assertEquals(1L, service.salvar(1L, form(null, "Custos", null, 1)).getId());
    }

    @Test
    void salvarSemVersaoOuVersaoVelha() {
        BlocoArquivoForm sem = form(null, null, "[]", null);
        assertThrows(EntradaInvalidaException.class, () -> service.salvar(1L, sem));
        when(repository.alterar(eq(1L), eq(EU), anyString(), anyString(), any(), anyInt())).thenReturn(0);
        BlocoArquivoForm velha = form(null, null, "[]", 1);
        assertThrows(ConflitoException.class, () -> service.salvar(1L, velha));
    }

    @Test
    void planilhaPrecisaSerJson() {
        assertThrows(EntradaInvalidaException.class, () -> service.conteudo("PLANILHA", "{quebrado"));
        assertThrows(EntradaInvalidaException.class, () -> service.conteudo("PLANILHA", "42"));
        assertEquals("", service.conteudo("PLANILHA", ""));
        String grande = "[\"" + "x".repeat(BlocoArquivoServiceImpl.MAX_PLANILHA) + "\"]";
        assertThrows(EntradaInvalidaException.class, () -> service.conteudo("PLANILHA", grande));
    }

    @Test
    void documentoSemScriptNemEventoETamanho() {
        assertThrows(EntradaInvalidaException.class, () -> service.conteudo("DOCUMENTO", "<p onclick=\"x\">a</p>"));
        assertThrows(EntradaInvalidaException.class, () -> service.conteudo("DOCUMENTO", "<script>a</script>"));
        String grande = "x".repeat(BlocoArquivoServiceImpl.MAX_DOCUMENTO + 1);
        assertThrows(EntradaInvalidaException.class, () -> service.conteudo("DOCUMENTO", grande));
        assertEquals("<p><b>ok</b></p>", service.conteudo("DOCUMENTO", "<p><b>ok</b></p>"));
    }

    @Test
    void excluiSoODoDono() {
        service.excluir(2L);
        verify(repository).excluir(2L, EU);
    }

    @Test
    void verExigeOModuloBlocoNotasComLeitura() {
        try {
            service.listar();
        } catch (RuntimeException ignorado) {
            // só interessa o acesso pedido
        }
        org.mockito.Mockito.verify(autorizacao, org.mockito.Mockito.atLeastOnce())
                .exigir(br.com.nord_tool_backend.security.Modulo.BLOCO_NOTAS, br.com.nord_tool_backend.security.Acao.LEITURA);
    }
}
