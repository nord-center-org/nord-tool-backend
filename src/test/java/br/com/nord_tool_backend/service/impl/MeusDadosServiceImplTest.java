package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.domain.Empresa;
import br.com.nord_tool_backend.domain.Usuario;
import br.com.nord_tool_backend.dto.MeusDadosDto;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.exception.NaoEncontradoException;
import br.com.nord_tool_backend.form.MeusDadosForm;
import br.com.nord_tool_backend.repository.AcessoRepository;
import br.com.nord_tool_backend.repository.EmpresaRepository;
import br.com.nord_tool_backend.security.UsuarioAutenticado;
import br.com.nord_tool_backend.service.AutorizacaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MeusDadosServiceImplTest {

    private static final Long EU = 20L;
    private AcessoRepository repository;
    private EmpresaRepository empresas;
    private AutorizacaoService autorizacao;
    private MeusDadosServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(AcessoRepository.class);
        empresas = mock(EmpresaRepository.class);
        autorizacao = mock(AutorizacaoService.class);
        service = new MeusDadosServiceImpl(repository, empresas, autorizacao);
        when(autorizacao.exigirAutenticado()).thenReturn(new UsuarioAutenticado(EU, "ana@obra.com", "USER", Collections.emptyMap(), 0, Instant.now()));
        when(repository.listarUsuarios()).thenReturn(List.of(usuario(10L, "Admin", 1), usuario(EU, "Ana", 7)));
        when(empresas.listarEmpresas()).thenReturn(List.of(Empresa.builder().id(3L).nmEmpresa("Cury").build()));
    }

    private static Usuario usuario(Long id, String nome, Integer cargo) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNmNome(nome);
        u.setNmEmail(nome.toLowerCase() + "@obra.com");
        u.setIdCargo(cargo);
        u.setNmCargo("Estagiária");
        u.setNrCelular("11 99999-0000");
        return u;
    }

    private static MeusDadosForm form(String nome, String celular, Integer empresa) {
        MeusDadosForm f = new MeusDadosForm();
        f.setNmNome(nome);
        f.setNrCelular(celular);
        f.setIdEmpresa(empresa);
        return f;
    }

    @Test
    void leOsMeusDados() {
        MeusDadosDto d = service.ler();
        assertEquals("Ana", d.getNmNome());
        assertEquals("ana@obra.com", d.getNmEmail());
        assertEquals("Estagiária", d.getNmCargo());
    }

    @Test
    void salvaNomeCelularEEmpresaMantendoOCargo() {
        service.salvar(form("  Ana Paula ", " 11 98888-7777 ", 3));
        verify(repository).alterarDados(EU, "Ana Paula", "11 98888-7777", 3, 7);
    }

    @Test
    void celularVazioViraNuloESemEmpresa() {
        service.salvar(form("Ana", "  ", null));
        verify(repository).alterarDados(EU, "Ana", null, null, 7);
        service.salvar(form("Ana", null, null));
        verify(repository, org.mockito.Mockito.times(2)).alterarDados(EU, "Ana", null, null, 7);
    }

    @Test
    void empresaQueNaoExisteERecusada() {
        MeusDadosForm f = form("Ana", "", 99);
        assertThrows(EntradaInvalidaException.class, () -> service.salvar(f));
        verify(repository, never()).alterarDados(anyLong(), anyString(), any(), any(), any());
    }

    @Test
    void usuarioQueSumiuNaoEncontra() {
        when(repository.listarUsuarios()).thenReturn(Collections.emptyList());
        assertThrows(NaoEncontradoException.class, () -> service.ler());
    }
}
