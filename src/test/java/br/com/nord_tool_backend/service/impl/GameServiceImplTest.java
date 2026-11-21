package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.dto.GameConquistaDto;
import br.com.nord_tool_backend.dto.GameCopaDto;
import br.com.nord_tool_backend.dto.GameRegistroDto;
import br.com.nord_tool_backend.dto.GameResumoDto;
import br.com.nord_tool_backend.dto.GameTotaisDto;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.form.GameCopaForm;
import br.com.nord_tool_backend.form.GamePartidaForm;
import br.com.nord_tool_backend.repository.GameRepository;
import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.security.UsuarioAutenticado;
import br.com.nord_tool_backend.service.AutorizacaoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GameServiceImplTest {

    private static final Long EU = 7L;
    private GameRepository repository;
    private AutorizacaoService autorizacao;
    private GameServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(GameRepository.class);
        autorizacao = mock(AutorizacaoService.class);
        when(autorizacao.exigir(any(), any())).thenReturn(new UsuarioAutenticado(EU, "eu@obra.com", "USUARIO",
                Collections.emptyMap(), 0, Instant.now()));
        service = new GameServiceImpl(repository, autorizacao, new ObjectMapper());
    }

    private static GameTotaisDto totais(long partidas, long vitorias, long recorde, long jogos, long dificil, long oito) {
        GameTotaisDto t = new GameTotaisDto();
        t.setNrPartidas(partidas);
        t.setNrVitorias(vitorias);
        t.setNrRecorde(recorde);
        t.setNrJogos(jogos);
        t.setNrVitoriasDificil(dificil);
        t.setNrVitoriasOito(oito);
        return t;
    }

    private static GamePartidaForm partida(int pontos, boolean vitoria, int jogadores, int bots, int colocacao) {
        GamePartidaForm f = new GamePartidaForm();
        f.setCdJogo("bombas");
        f.setCdModo("sobrevivencia");
        f.setNrPontos(pontos);
        f.setInVitoria(vitoria);
        f.setNrJogadores(jogadores);
        f.setNrBots(bots);
        f.setNrColocacao(colocacao);
        f.setCdDificuldade("dificil");
        return f;
    }

    @Test
    void primeiraPartidaVencidaDaAsConquistasEOrecorde() {
        when(repository.totais(EU, "bombas")).thenReturn(totais(0, 0, 0, 0, 0, 0));
        when(repository.totais(eq(EU), isNull())).thenReturn(totais(1, 1, 3, 1, 1, 1));
        when(repository.ganharConquista(eq(EU), anyString())).thenReturn(true);
        GameRegistroDto r = service.registrarPartida(partida(3, true, 8, 7, 1));
        verify(autorizacao).exigir(Modulo.GAMES, Acao.ESCRITA);
        verify(repository).registrarPartida(eq(EU), any());
        assertTrue(r.isInNovoRecorde());
        assertEquals(3, r.getNrRecorde());
        List<String> codigos = r.getNovas().stream().map(GameConquistaDto::getCdConquista).collect(Collectors.toList());
        assertEquals(Arrays.asList("PRIMEIRA_PARTIDA", "PRIMEIRA_VITORIA", "VENCEU_DIFICIL", "OITO_JOGADORES"), codigos);
        assertNotNull(r.getNovas().get(0).getDhCriacao());
        assertEquals("Primeiro controle", r.getNovas().get(0).getNmConquista());
    }

    @Test
    void conquistaJaGanhaNaoVoltaComoNova() {
        when(repository.totais(EU, "bombas")).thenReturn(totais(0, 0, 10, 0, 0, 0));
        when(repository.totais(eq(EU), isNull())).thenReturn(totais(100, 50, 10, 5, 0, 0));
        when(repository.ganharConquista(EU, "CEM_PARTIDAS")).thenReturn(true);
        GameRegistroDto r = service.registrarPartida(partida(4, false, 2, 1, 2));
        assertFalse(r.isInNovoRecorde());
        assertEquals(10, r.getNrRecorde());
        assertEquals(1, r.getNovas().size());
        assertEquals("CEM_PARTIDAS", r.getNovas().get(0).getCdConquista());
        verify(repository, never()).ganharConquista(EU, "CAMPEAO_COPA");
    }

    @Test
    void recusaBotsDemaisOuColocacaoImpossivel() {
        assertThrows(EntradaInvalidaException.class, () -> service.registrarPartida(partida(0, false, 2, 2, 1)));
        assertThrows(EntradaInvalidaException.class, () -> service.registrarPartida(partida(0, false, 2, 1, 3)));
        verify(repository, never()).registrarPartida(anyLong(), any());
    }

    @Test
    void resumoJuntaMeusNumerosRankingConquistasEHistorico() {
        when(repository.totais(EU, "bombas")).thenReturn(totais(3, 1, 9, 1, 0, 0));
        when(repository.ranking("bombas", GameServiceImpl.LIMITE_RANKING)).thenReturn(Collections.emptyList());
        when(repository.historico(EU, "bombas", GameServiceImpl.LIMITE_HISTORICO)).thenReturn(Collections.emptyList());
        Map<String, LocalDateTime> ganhas = new HashMap<>();
        LocalDateTime quando = LocalDateTime.of(2026, 11, 21, 10, 0);
        ganhas.put("PRIMEIRA_PARTIDA", quando);
        when(repository.conquistas(EU)).thenReturn(ganhas);
        GameResumoDto r = service.resumo("bombas");
        verify(autorizacao).exigir(Modulo.GAMES, Acao.LEITURA);
        assertEquals(9, r.getMeus().getNrRecorde());
        assertEquals(9, r.getConquistas().size());
        assertEquals(quando, r.getConquistas().get(0).getDhCriacao());
        assertNull(r.getConquistas().get(1).getDhCriacao());
        assertTrue(r.getRanking().isEmpty());
        assertTrue(r.getHistorico().isEmpty());
    }

    @Test
    void resumoRecusaJogoInvalido() {
        assertThrows(EntradaInvalidaException.class, () -> service.resumo(null));
        assertThrows(EntradaInvalidaException.class, () -> service.resumo("Bombas!"));
    }

    @Test
    void conquistasListaOCatalogoInteiro() {
        when(repository.conquistas(EU)).thenReturn(Collections.emptyMap());
        List<GameConquistaDto> l = service.conquistas();
        assertEquals(9, l.size());
        assertEquals("CAMPEAO_COPA", l.get(8).getCdConquista());
        assertTrue(l.stream().allMatch(c -> c.getDhCriacao() == null));
    }

    @Test
    void favoritosLeEToleraJsonEstragado() {
        when(repository.favoritos(EU)).thenReturn(Optional.empty());
        assertTrue(service.favoritos().isEmpty());
        when(repository.favoritos(EU)).thenReturn(Optional.of("[\"bombas\",\"sumo\"]"));
        assertEquals(Arrays.asList("bombas", "sumo"), service.favoritos());
        when(repository.favoritos(EU)).thenReturn(Optional.of("{x"));
        assertTrue(service.favoritos().isEmpty());
        when(repository.favoritos(EU)).thenReturn(Optional.of("null"));
        assertTrue(service.favoritos().isEmpty());
    }

    @Test
    void salvarFavoritosTiraRepetidosEGuardaJson() {
        assertEquals(Arrays.asList("bombas", "sumo"), service.salvarFavoritos(Arrays.asList("bombas", "sumo", "bombas")));
        verify(repository).salvarFavoritos(EU, "[\"bombas\",\"sumo\"]");
        assertTrue(service.salvarFavoritos(null).isEmpty());
    }

    @Test
    void salvarFavoritosRecusaInvalidosEExcesso() {
        assertThrows(EntradaInvalidaException.class, () -> service.salvarFavoritos(Collections.singletonList("../x")));
        assertThrows(EntradaInvalidaException.class, () -> service.salvarFavoritos(Collections.singletonList(null)));
        List<String> muitos = new java.util.ArrayList<>();
        for (int i = 0; i <= GameServiceImpl.MAX_FAVORITOS; i++) muitos.add("jogo-" + i);
        assertThrows(EntradaInvalidaException.class, () -> service.salvarFavoritos(muitos));
        verify(repository, never()).salvarFavoritos(anyLong(), anyString());
    }

    private static GameCopaForm copa(int rodadas, int colocacao, String resultado) {
        GameCopaForm f = new GameCopaForm();
        f.setNrRodadas(rodadas);
        f.setNrColocacao(colocacao);
        f.setTxResultado(resultado);
        return f;
    }

    @Test
    void copaVencidaDaAConquistaDeCampeao() {
        when(repository.registrarCopa(EU, 5, 1, "{\"podio\":[]}")).thenReturn(42L);
        GameCopaDto d = service.registrarCopa(copa(5, 1, "{\"podio\":[]}"));
        assertEquals(42L, d.getId());
        assertEquals(5, d.getNrRodadas());
        assertEquals(1, d.getNrColocacao());
        assertNotNull(d.getDhCriacao());
        verify(repository).ganharConquista(EU, "CAMPEAO_COPA");
    }

    @Test
    void copaSemVitoriaNaoDaConquista() {
        service.registrarCopa(copa(3, 2, "{}"));
        service.registrarCopa(copa(7, 4, "[]"));
        verify(repository, never()).ganharConquista(anyLong(), anyString());
    }

    @Test
    void copaRecusaRodadasOuResultadoInvalidos() {
        assertThrows(EntradaInvalidaException.class, () -> service.registrarCopa(copa(4, 1, "{}")));
        assertThrows(EntradaInvalidaException.class, () -> service.registrarCopa(copa(3, 1, "{x")));
        verify(repository, never()).registrarCopa(anyLong(), anyInt(), anyInt(), anyString());
    }

    @Test
    void copasListaAsUltimas() {
        when(repository.copas(EU, GameServiceImpl.LIMITE_COPAS)).thenReturn(Collections.emptyList());
        assertTrue(service.copas().isEmpty());
        verify(autorizacao).exigir(Modulo.GAMES, Acao.LEITURA);
    }
}
