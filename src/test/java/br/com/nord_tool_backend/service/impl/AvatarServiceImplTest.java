package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.dto.AvatarDto;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.exception.NaoAutenticadoException;
import br.com.nord_tool_backend.form.AvatarForm;
import br.com.nord_tool_backend.repository.AvatarRepository;
import br.com.nord_tool_backend.security.UsuarioAutenticado;
import br.com.nord_tool_backend.service.AutorizacaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AvatarServiceImplTest {

    private static final Long EU = 7L;

    private AvatarRepository repository;
    private AutorizacaoService autorizacao;
    private AvatarServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(AvatarRepository.class);
        autorizacao = mock(AutorizacaoService.class);
        UsuarioAutenticado u = mock(UsuarioAutenticado.class);
        when(u.getId()).thenReturn(EU);
        when(autorizacao.exigirAutenticado()).thenReturn(u);
        service = new AvatarServiceImpl(repository, autorizacao);
    }

    private static AvatarForm form(String json) {
        AvatarForm f = new AvatarForm();
        f.setDsConfig(json);
        return f;
    }

    @Test
    void salvaJsonCompactoDoProprioUsuario() {
        when(repository.buscar(EU)).thenReturn(Optional.of(new AvatarDto()));
        service.salvar(form("{ \"pele\": \"p3\", \"funcao\": \"engenheiro\", \"datas\": true, \"tom\": 2 }"));
        verify(repository).salvar(EU, "{\"pele\":\"p3\",\"funcao\":\"engenheiro\",\"datas\":true,\"tom\":2}");
    }

    @Test
    void recusaJsonInvalidoOuQueNaoSejaObjeto() {
        assertThrows(EntradaInvalidaException.class, () -> service.salvar(form("{pele")));
        assertThrows(EntradaInvalidaException.class, () -> service.salvar(form("[1,2]")));
        verify(repository, never()).salvar(anyLong(), any());
    }

    @Test
    void recusaValoresAninhadosLongosOuChavesEstranhas() {
        assertThrows(EntradaInvalidaException.class, () -> service.salvar(form("{\"a\":{\"b\":1}}")));
        assertThrows(EntradaInvalidaException.class, () -> service.salvar(form("{\"a\":\"" + new String(new char[41]).replace('\0', 'x') + "\"}")));
        assertThrows(EntradaInvalidaException.class, () -> service.salvar(form("{\"a-b\":\"x\"}")));
        StringBuilder muitas = new StringBuilder("{");
        for (int i = 0; i < 31; i++) muitas.append(i == 0 ? "" : ",").append("\"k").append(i).append("\":1");
        assertThrows(EntradaInvalidaException.class, () -> service.salvar(form(muitas.append("}").toString())));
    }

    @Test
    void equipeUsaOUsuarioLogadoComLimite() {
        when(repository.equipe(EU, AvatarServiceImpl.LIMITE_EQUIPE)).thenReturn(Collections.emptyList());
        assertEquals(0, service.equipe().size());
    }

    @Test
    void exigeLogin() {
        when(autorizacao.exigirAutenticado()).thenThrow(new NaoAutenticadoException("x"));
        assertThrows(NaoAutenticadoException.class, () -> service.meu());
        assertThrows(NaoAutenticadoException.class, () -> service.salvar(form("{}")));
        verify(repository, never()).salvar(anyLong(), any());
    }
}
