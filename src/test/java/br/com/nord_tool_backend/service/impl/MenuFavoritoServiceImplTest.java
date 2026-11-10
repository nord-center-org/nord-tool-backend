package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.repository.MenuFavoritoRepository;
import br.com.nord_tool_backend.security.UsuarioAutenticado;
import br.com.nord_tool_backend.service.AutorizacaoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MenuFavoritoServiceImplTest {

    private static final Long EU = 7L;
    private MenuFavoritoRepository repository;
    private MenuFavoritoServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(MenuFavoritoRepository.class);
        AutorizacaoService autorizacao = mock(AutorizacaoService.class);
        when(autorizacao.exigirAutenticado()).thenReturn(new UsuarioAutenticado(EU, "eu@obra.com", "USUARIO",
                Collections.emptyMap(), 0, Instant.now()));
        service = new MenuFavoritoServiceImpl(repository, autorizacao, new ObjectMapper());
    }

    @Test
    void semNadaSalvoVoltaVazio() {
        when(repository.buscar(EU)).thenReturn(Optional.empty());
        assertTrue(service.listar().isEmpty());
    }

    @Test
    void listaOsSalvosNaOrdem() {
        when(repository.buscar(EU)).thenReturn(Optional.of("[\"/entregas\",\"/mapeamentos/bloco-notas\"]"));
        assertEquals(Arrays.asList("/entregas", "/mapeamentos/bloco-notas"), service.listar());
    }

    @Test
    void jsonEstragadoOuNuloVoltaVazio() {
        when(repository.buscar(EU)).thenReturn(Optional.of("{x"));
        assertTrue(service.listar().isEmpty());
        when(repository.buscar(EU)).thenReturn(Optional.of("null"));
        assertTrue(service.listar().isEmpty());
    }

    @Test
    void salvaSemRepetirNaOrdem() {
        List<String> r = service.salvar(Arrays.asList("/sac", "/entregas", "/sac"));
        assertEquals(Arrays.asList("/sac", "/entregas"), r);
        verify(repository).salvar(EU, "[\"/sac\",\"/entregas\"]");
        assertTrue(service.salvar(null).isEmpty());
    }

    @Test
    void recusaCaminhoEstranho() {
        for (String c : Arrays.asList("javascript:alert(1)", "/Maiuscula", "sem-barra", "/../<x>")) {
            List<String> l = Collections.singletonList(c);
            assertThrows(EntradaInvalidaException.class, () -> service.salvar(l), c);
        }
        List<String> comNulo = new ArrayList<>();
        comNulo.add(null);
        assertThrows(EntradaInvalidaException.class, () -> service.salvar(comNulo));
        verify(repository, never()).salvar(anyLong(), anyString());
    }

    @Test
    void limiteDeFavoritos() {
        List<String> muitos = new ArrayList<>();
        for (int i = 0; i <= MenuFavoritoServiceImpl.MAX_FAVORITOS; i++) muitos.add("/tela-" + i);
        assertThrows(EntradaInvalidaException.class, () -> service.salvar(muitos));
    }
}
