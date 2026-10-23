package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.dto.DaviJobPlacarDto;
import br.com.nord_tool_backend.dto.DaviJobRankingDto;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.exception.NaoAutenticadoException;
import br.com.nord_tool_backend.form.DaviJobPontuacaoForm;
import br.com.nord_tool_backend.repository.DaviJobRepository;
import br.com.nord_tool_backend.security.UsuarioAutenticado;
import br.com.nord_tool_backend.service.AutorizacaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DaviJobServiceImplTest {

    private static final Long EU = 7L;

    private DaviJobRepository repository;
    private AutorizacaoService autorizacao;
    private DaviJobServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(DaviJobRepository.class);
        autorizacao = mock(AutorizacaoService.class);
        UsuarioAutenticado u = mock(UsuarioAutenticado.class);
        when(u.getId()).thenReturn(EU);
        when(autorizacao.exigirAutenticado()).thenReturn(u);
        service = new DaviJobServiceImpl(repository, autorizacao);
    }

    private static DaviJobPontuacaoForm form(Integer pontos) {
        DaviJobPontuacaoForm f = new DaviJobPontuacaoForm();
        f.setNrPontos(pontos);
        return f;
    }

    @Test
    void registraParaOUsuarioLogadoEDevolveOPlacar() {
        DaviJobRankingDto r = new DaviJobRankingDto();
        r.setNrPontos(12);
        when(repository.ranking(EU, DaviJobServiceImpl.TOP)).thenReturn(Collections.singletonList(r));
        when(repository.recorde(EU)).thenReturn(Optional.of(r));
        DaviJobPlacarDto p = service.registrar(form(12));
        verify(repository).inserir(EU, 12);
        assertEquals(1, p.getRanking().size());
        assertEquals(12, p.getMeuRecorde().getNrPontos());
    }

    @Test
    void recusaPontosForaDoLimite() {
        assertThrows(EntradaInvalidaException.class, () -> service.registrar(form(-1)));
        assertThrows(EntradaInvalidaException.class, () -> service.registrar(form(10001)));
        assertThrows(EntradaInvalidaException.class, () -> service.registrar(form(null)));
        verify(repository, never()).inserir(anyLong(), anyInt());
    }

    @Test
    void quemNuncaJogouNaoTemRecorde() {
        when(repository.ranking(EU, DaviJobServiceImpl.TOP)).thenReturn(Collections.emptyList());
        when(repository.recorde(EU)).thenReturn(Optional.empty());
        assertNull(service.placar().getMeuRecorde());
    }

    @Test
    void exigeLogin() {
        when(autorizacao.exigirAutenticado()).thenThrow(new NaoAutenticadoException("x"));
        assertThrows(NaoAutenticadoException.class, () -> service.placar());
        assertThrows(NaoAutenticadoException.class, () -> service.registrar(form(3)));
    }
}
