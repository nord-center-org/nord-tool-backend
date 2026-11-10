package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.dto.CubicagemDto;
import br.com.nord_tool_backend.exception.ConflitoException;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.exception.NaoEncontradoException;
import br.com.nord_tool_backend.exception.NegocioException;
import br.com.nord_tool_backend.form.CubicagemForm;
import br.com.nord_tool_backend.repository.CubicagemRepository;
import br.com.nord_tool_backend.security.UsuarioAutenticado;
import br.com.nord_tool_backend.service.AutorizacaoService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CubicagemServiceImplTest {

    private static final Long EU = 7L;

    private CubicagemRepository repository;
    private CubicagemServiceImpl service;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        repository = mock(CubicagemRepository.class);
        AutorizacaoService autorizacao = mock(AutorizacaoService.class);
        when(autorizacao.exigirAutenticado()).thenReturn(new UsuarioAutenticado(EU, "eu@obra.com", "USUARIO",
                Collections.emptyMap(), 0, Instant.now()));
        service = new CubicagemServiceImpl(repository, autorizacao, mapper);
        when(repository.buscar(1L, EU)).thenReturn(Optional.of(linha(1L, "[{\"codigo\":\"V12\"}]")));
    }

    private static CubicagemDto linha(Long id, String pecas) {
        CubicagemDto c = new CubicagemDto();
        c.setId(id);
        c.setNmTitulo("Torre A - 3º pavimento");
        c.setNrVersao(1);
        c.setTxPecas(pecas);
        return c;
    }

    private CubicagemForm form(String pecas, Integer versao) throws Exception {
        CubicagemForm f = new CubicagemForm();
        f.setNmTitulo("  Torre A  ");
        f.setNrPerda(new BigDecimal("5"));
        f.setPecas(pecas == null ? null : mapper.readTree(pecas));
        f.setNrVersao(versao);
        return f;
    }

    @Test
    void listaSoAsMinhas() {
        when(repository.listar(EU)).thenReturn(List.of(linha(1L, null)));
        assertEquals(1, service.listar().size());
    }

    @Test
    void abreComAsPecasEmJsonSemOTextoCru() {
        CubicagemDto c = service.abrir(1L);
        assertEquals("V12", c.getPecas().get(0).get("codigo").asText());
        assertNull(c.getTxPecas());
    }

    @Test
    void abrirDeOutraPessoaNaoAcha() {
        when(repository.buscar(9L, EU)).thenReturn(Optional.empty());
        assertThrows(NaoEncontradoException.class, () -> service.abrir(9L));
    }

    @Test
    void jsonEstragadoNoBancoAbreVazio() {
        when(repository.buscar(2L, EU)).thenReturn(Optional.of(linha(2L, "{quebrado")));
        assertEquals(0, service.abrir(2L).getPecas().size());
        when(repository.buscar(3L, EU)).thenReturn(Optional.of(linha(3L, null)));
        assertTrue(service.abrir(3L).getPecas().isArray());
    }

    @Test
    void criaComNomeAparadoEPerda() throws Exception {
        when(repository.inserir(eq(EU), eq("Torre A"), eq(new BigDecimal("5")), eq("[{\"codigo\":\"P5\"}]"))).thenReturn(1L);
        assertEquals(1L, service.criar(form("[{\"codigo\":\"P5\"}]", null)).getId());
    }

    @Test
    void criaSemPecasESemPerda() throws Exception {
        CubicagemForm f = form(null, null);
        f.setNrPerda(null);
        when(repository.inserir(EU, "Torre A", BigDecimal.ZERO, "[]")).thenReturn(1L);
        assertEquals(1L, service.criar(f).getId());
    }

    @Test
    void limiteDeCubicagens() throws Exception {
        when(repository.contar(EU)).thenReturn(CubicagemServiceImpl.MAX_CUBICAGENS);
        CubicagemForm f = form("[]", null);
        assertThrows(NegocioException.class, () -> service.criar(f));
        verify(repository, never()).inserir(any(), anyString(), any(), anyString());
    }

    @Test
    void salvaComAVersao() throws Exception {
        when(repository.alterar(1L, EU, "Torre A", new BigDecimal("5"), "[]", 1)).thenReturn(1);
        assertEquals(1L, service.salvar(1L, form("[]", 1)).getId());
    }

    @Test
    void salvarSemVersaoOuComVersaoVelha() throws Exception {
        CubicagemForm sem = form("[]", null);
        assertThrows(EntradaInvalidaException.class, () -> service.salvar(1L, sem));
        when(repository.alterar(eq(1L), eq(EU), anyString(), any(), anyString(), anyInt())).thenReturn(0);
        CubicagemForm velha = form("[]", 1);
        assertThrows(ConflitoException.class, () -> service.salvar(1L, velha));
    }

    @Test
    void pecasPrecisamSerListaDeObjetos() throws Exception {
        assertThrows(EntradaInvalidaException.class, () -> service.pecas(mapper.readTree("{\"a\":1}")));
        assertThrows(EntradaInvalidaException.class, () -> service.pecas(mapper.readTree("[1]")));
        assertEquals("[]", service.pecas(mapper.readTree("null")));
    }

    @Test
    void limiteDePecasEDeTamanho() {
        ArrayNode muitas = mapper.createArrayNode();
        for (int i = 0; i <= CubicagemServiceImpl.MAX_PECAS; i++) muitas.addObject();
        assertThrows(EntradaInvalidaException.class, () -> service.pecas(muitas));
        ArrayNode grande = mapper.createArrayNode();
        grande.addObject().put("obs", "x".repeat(CubicagemServiceImpl.MAX_JSON));
        JsonNode g = grande;
        assertThrows(EntradaInvalidaException.class, () -> service.pecas(g));
    }

    @Test
    void excluiSoADoDono() {
        service.excluir(1L);
        verify(repository).excluir(1L, EU);
        when(repository.buscar(9L, EU)).thenReturn(Optional.empty());
        assertThrows(NaoEncontradoException.class, () -> service.excluir(9L));
    }
}
