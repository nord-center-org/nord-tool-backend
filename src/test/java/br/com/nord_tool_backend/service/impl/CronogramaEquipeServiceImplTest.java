package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.domain.CronogramaEquipe;
import br.com.nord_tool_backend.dto.CronogramaEquipeDto;
import br.com.nord_tool_backend.exception.AcessoNegadoException;
import br.com.nord_tool_backend.exception.ConflitoException;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.exception.NaoEncontradoException;
import br.com.nord_tool_backend.form.CronogramaEquipeForm;
import br.com.nord_tool_backend.repository.CronogramaEquipeRepository;
import br.com.nord_tool_backend.repository.CronogramaSemanalRepository;
import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.security.UsuarioAutenticado;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.service.ContextoFaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CronogramaEquipeServiceImplTest {

    private static final long OBRA = 4L;
    private CronogramaEquipeRepository repository;
    private CronogramaSemanalRepository cronograma;
    private AutorizacaoService autorizacao;
    private CronogramaEquipeServiceImpl service;

    private static UsuarioAutenticado usuario(Acao noCronograma) {
        Map<Modulo, Acao> p = new EnumMap<>(Modulo.class);
        p.put(Modulo.CRONOGRAMA, noCronograma);
        return new UsuarioAutenticado(9L, "u@o.com", "USER", p, 0, Instant.now());
    }

    @BeforeEach
    void setUp() {
        repository = mock(CronogramaEquipeRepository.class);
        cronograma = mock(CronogramaSemanalRepository.class);
        autorizacao = mock(AutorizacaoService.class);
        ContextoFaseService contexto = mock(ContextoFaseService.class);
        when(contexto.obraEscolhida()).thenReturn(OBRA);
        UsuarioAutenticado admin = usuario(Acao.ADMIN);
        when(autorizacao.exigir(any(), any())).thenReturn(admin);
        when(autorizacao.exigirAutenticado()).thenReturn(admin);
        service = new CronogramaEquipeServiceImpl(repository, cronograma, autorizacao, contexto);
    }

    private static CronogramaEquipe linha(long id, String membros) {
        CronogramaEquipe e = new CronogramaEquipe();
        e.setId(id);
        e.setIdObra(OBRA);
        e.setNmObra("Mérito");
        e.setNmEquipe("Qualidade");
        e.setIdLider(3L);
        e.setTxMembros(membros);
        return e;
    }

    private static CronogramaEquipeForm form(String nome, Long lider, Long... membros) {
        CronogramaEquipeForm f = new CronogramaEquipeForm();
        f.setNmEquipe(nome);
        f.setIdLider(lider);
        f.setIdsMembros(Arrays.asList(membros));
        return f;
    }

    @Test
    void minhasEquipesVemDeTodasAsObrasEOAdminVeTodas() {
        when(repository.listar(null, 9L, true)).thenReturn(Collections.singletonList(linha(1, "3,7,9")));
        List<CronogramaEquipeDto> l = service.minhasEquipes();
        assertEquals(Arrays.asList(3L, 7L, 9L), l.get(0).getIdsMembros());

        // Quem não é admin só vê as equipes de que participa.
        when(autorizacao.exigir(Modulo.CRONOGRAMA, Acao.LEITURA)).thenReturn(usuario(Acao.ESCRITA));
        service.minhasEquipes();
        verify(repository).listar(null, 9L, false);
    }

    @Test
    void criarIncluiOLiderSemRepetirEValidaPessoas() {
        when(cronograma.contarUsuariosAtivos(any())).thenReturn(3);
        when(repository.criar(OBRA, "Qualidade", 3L)).thenReturn(1L);
        when(repository.listar(OBRA, 9L, true)).thenReturn(Collections.singletonList(linha(1, "3,7,8")));

        CronogramaEquipeDto d = service.criar(form("  Qualidade ", 3L, 7L, 8L, 7L, null));

        verify(repository).substituirMembros(1L, new LinkedHashSet<>(Arrays.asList(3L, 7L, 8L)));
        assertEquals("Qualidade", d.getNmEquipe());
    }

    @Test
    void criarRecusaNomeRepetidoPessoaInativaEEquipeVazia() {
        when(repository.nomeEmUso(OBRA, "Qualidade", null)).thenReturn(true);
        assertThrows(ConflitoException.class, () -> service.criar(form("Qualidade", 3L)));
        when(repository.nomeEmUso(OBRA, "Obra", null)).thenReturn(false);
        when(cronograma.contarUsuariosAtivos(any())).thenReturn(1);
        assertThrows(EntradaInvalidaException.class, () -> service.criar(form("Obra", 3L, 7L)));
        assertThrows(EntradaInvalidaException.class, () -> service.criar(form("Obra", null)));
        assertThrows(EntradaInvalidaException.class, () -> service.criar(form("  ", 3L)));
        verify(repository, never()).criar(anyLong(), anyString(), any());
    }

    @Test
    void alterarEExcluirSoNaObraEscolhida() {
        when(cronograma.contarUsuariosAtivos(any())).thenReturn(1);
        when(repository.alterar(5L, OBRA, "Nova", 3L)).thenReturn(0);
        assertThrows(NaoEncontradoException.class, () -> service.alterar(5L, form("Nova", 3L)));
        verify(repository, never()).substituirMembros(anyLong(), any());
        when(repository.excluir(5L, OBRA)).thenReturn(0);
        assertThrows(NaoEncontradoException.class, () -> service.excluir(5L));
        when(repository.excluir(6L, OBRA)).thenReturn(1);
        service.excluir(6L);
    }

    @Test
    void montarEquipeEhSoDoAdminDoCronograma() {
        doThrow(new AcessoNegadoException("negado")).when(autorizacao).exigir(Modulo.CRONOGRAMA, Acao.ADMIN);
        assertThrows(AcessoNegadoException.class, () -> service.criar(form("Qualidade", 3L)));
        assertThrows(AcessoNegadoException.class, () -> service.equipesDaObra());
        assertThrows(AcessoNegadoException.class, () -> service.pelaHierarquia(3L));
        verify(repository, never()).criar(anyLong(), anyString(), any());
        verify(repository, never()).hierarquia(anyLong());
    }

    @Test
    void hierarquiaDevolveOLiderEQuemEstaAbaixo() {
        when(repository.hierarquia(3L)).thenReturn(Arrays.asList(3L, 7L, 8L));
        assertEquals(Arrays.asList(3L, 7L, 8L), service.pelaHierarquia(3L));
        assertThrows(EntradaInvalidaException.class, () -> service.pelaHierarquia(null));
        verify(repository, never()).listar(eq(OBRA), anyLong(), eq(false));
        verify(repository, never()).listar(isNull(), anyLong(), eq(false));
    }
}
