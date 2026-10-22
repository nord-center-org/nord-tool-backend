package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.domain.Fase;
import br.com.nord_tool_backend.domain.Obra;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.repository.ContextoFaseRepository;
import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.EscopoFase;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.security.UsuarioAutenticado;
import br.com.nord_tool_backend.service.AutorizacaoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContextoFaseServiceImplTest {

    private ContextoFaseRepository repository;
    private AutorizacaoService autorizacao;
    private ContextoFaseServiceImpl service;
    private MockHttpServletRequest req;

    @BeforeEach
    void setUp() {
        repository = mock(ContextoFaseRepository.class);
        autorizacao = mock(AutorizacaoService.class);
        service = new ContextoFaseServiceImpl(repository, autorizacao);
        req = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));
        when(repository.existeFase()).thenReturn(true);
    }

    @AfterEach
    void limpar() {
        RequestContextHolder.resetRequestAttributes();
    }

    private void logado(long id, boolean admin) {
        Map<Modulo, Acao> p = new EnumMap<>(Modulo.class);
        if (admin) p.put(Modulo.ADMINISTRACAO, Acao.ADMIN);
        when(autorizacao.exigirAutenticado()).thenReturn(new UsuarioAutenticado(id, "u@o.com", admin ? "ADMIN" : "USER", p, 0, Instant.now()));
    }

    private static Fase fase(long id, long obra, String sigla) {
        Fase f = new Fase();
        f.setId(id); f.setIdObra(obra); f.setNmFase("Fase " + id); f.setCdFase(sigla); f.setNrOrdem(1); f.setInAtivo(true); f.setNrVersao(1);
        return f;
    }

    @Test
    void semNenhumaFaseCadastradaTudoContinuaVisivel() {
        logado(5, false);
        when(repository.existeFase()).thenReturn(false);
        EscopoFase e = service.escopo();
        assertTrue(e.isTodas());
        assertNull(service.faseParaCriar("N1-01-0101"));
    }

    @Test
    void usuarioVeSoAsFasesDeleNaObraDoCabecalho() {
        logado(5, false);
        req.addHeader("X-Obra", "1");
        when(repository.fasesDoUsuario(5L, 1L)).thenReturn(Arrays.asList(fase(11, 1, "N1"), fase(12, 1, "N2")));
        EscopoFase e = service.escopo();
        assertFalse(e.isTodas());
        assertFalse(e.isVerSemFase());
        assertEquals("11,12", e.csv());
        assertTrue(e.permite(11L));
        assertFalse(e.permite(99L));
        assertFalse(e.permite(null));
        service.escopo();
        verify(repository, times(1)).fasesDoUsuario(5L, 1L); // memorizado na requisição
    }

    @Test
    void adminSemObraVeTodasEOsAntigosSemFase() {
        logado(1, true);
        when(repository.fasesAtivas(null)).thenReturn(Collections.singletonList(fase(11, 1, "N1")));
        EscopoFase e = service.escopo();
        assertTrue(e.isVerSemFase());
        assertTrue(e.permite(null));
    }

    @Test
    void faseParaCriarPeloCabecalhoPelaSiglaOuUnica() {
        logado(5, false);
        List<Fase> minhas = Arrays.asList(fase(11, 1, "N1"), fase(12, 1, "N2"));
        when(repository.fasesDoUsuario(5L, null)).thenReturn(minhas);
        assertEquals(12L, service.faseParaCriar("n2-01-0101"));
        assertThrows(EntradaInvalidaException.class, () -> service.faseParaCriar("EN-01-0101"));
        req.addHeader("X-Fase", "11");
        assertEquals(11L, service.faseParaCriar("n2-01-0101"));
    }

    @Test
    void faseDoCabecalhoQueAPessoaNaoTemEhRecusada() {
        logado(5, false);
        req.addHeader("X-Fase", "99");
        when(repository.fasesDoUsuario(5L, null)).thenReturn(Collections.singletonList(fase(11, 1, "N1")));
        assertThrows(EntradaInvalidaException.class, () -> service.faseParaCriar(null));
    }

    @Test
    void unicaFaseEntraSozinhaESemFaseAvisa() {
        logado(5, false);
        when(repository.fasesDoUsuario(5L, null)).thenReturn(Collections.singletonList(fase(11, 1, "N1")));
        assertEquals(11L, service.faseParaCriar(null));
        when(repository.fasesDoUsuario(5L, null)).thenReturn(Collections.emptyList());
        assertThrows(EntradaInvalidaException.class, () -> service.faseParaCriar(null));
    }

    @Test
    void minhasObrasSoComAsFasesDoUsuarioESemCodigo() {
        logado(5, false);
        Obra o1 = new Obra(); o1.setId(1L); o1.setNmObra("Nord"); o1.setCdAcesso("NORD"); o1.setInAtivo(true);
        Obra o2 = new Obra(); o2.setId(2L); o2.setNmObra("Outra"); o2.setCdAcesso("OUT"); o2.setInAtivo(true);
        when(repository.obrasAtivas()).thenReturn(Arrays.asList(o1, o2));
        when(repository.fasesDoUsuario(5L, null)).thenReturn(Collections.singletonList(fase(11, 1, "N1")));
        var obras = service.minhasObras();
        assertEquals(1, obras.size());
        assertEquals("Nord", obras.get(0).getNmObra());
        assertNull(obras.get(0).getCdAcesso());
        assertEquals(1, obras.get(0).getFases().size());
    }

    @Test
    void cabecalhoInvalidoDaErroClaro() {
        logado(5, false);
        req.addHeader("X-Obra", "abc");
        assertThrows(EntradaInvalidaException.class, () -> service.escopo());
    }
}
