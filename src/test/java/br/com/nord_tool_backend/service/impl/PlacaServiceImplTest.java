package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.dto.PlacaDto;
import br.com.nord_tool_backend.dto.PlacaModeloDto;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.exception.NaoEncontradoException;
import br.com.nord_tool_backend.form.PlacaForm;
import br.com.nord_tool_backend.form.PlacaModeloForm;
import br.com.nord_tool_backend.repository.PlacaRepository;
import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.security.UsuarioAutenticado;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.service.ContextoFaseService;
import br.com.nord_tool_backend.storage.ArmazenamentoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlacaServiceImplTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private PlacaRepository repository;
    private AutorizacaoService autorizacao;
    private ContextoFaseService contexto;
    private ArmazenamentoService armazenamento;
    private PlacaServiceImpl service;
    private final UsuarioAutenticado adm = new UsuarioAutenticado(1L, "a@obra.com", "ADMIN", Collections.singletonMap(Modulo.PLACAS, Acao.ADMIN), 0, Instant.now());

    @BeforeEach
    void preparar() {
        repository = mock(PlacaRepository.class);
        autorizacao = mock(AutorizacaoService.class);
        contexto = mock(ContextoFaseService.class);
        armazenamento = mock(ArmazenamentoService.class);
        service = new PlacaServiceImpl(repository, autorizacao, contexto, armazenamento, mapper);
        when(autorizacao.exigir(any(), any())).thenReturn(adm);
        when(contexto.obraEscolhida()).thenReturn(7L);
    }

    private PlacaModeloDto modelo(long id) {
        PlacaModeloDto m = new PlacaModeloDto();
        m.setId(id);
        m.setNmModelo("Aviso");
        m.setTxConfig("{\"orientacao\":\"paisagem\"}");
        m.setInAtivo(true);
        return m;
    }

    private PlacaForm form() throws Exception {
        PlacaForm f = new PlacaForm();
        f.setIdModelo(3L);
        f.setNmTitulo(" Escoramento ");
        f.setValores(mapper.readTree("{\"cabecalho\":\"NEXT GUARULHOS\",\"linhas\":[\"Viga barrote sem madeira\"]}"));
        return f;
    }

    @Test
    void placaNovaEntraNaObraEscolhidaComOsTextos() throws Exception {
        when(repository.modelo(3L)).thenReturn(Optional.of(modelo(3)));
        when(repository.inserirPlaca(eq(7L), eq(3L), eq("Escoramento"), anyString(), eq(1L))).thenReturn(20L);
        PlacaDto salva = new PlacaDto();
        salva.setId(20L);
        salva.setIdObra(7L);
        salva.setTxValores("{\"cabecalho\":\"NEXT GUARULHOS\"}");
        when(repository.placa(20L)).thenReturn(Optional.of(salva));
        PlacaDto p = service.criar(form());
        assertEquals("NEXT GUARULHOS", p.getValores().get("cabecalho").asText());
        verify(autorizacao).exigir(Modulo.PLACAS, Acao.ESCRITA);
    }

    @Test
    void placaDeOutraObraNaoEAlterada() throws Exception {
        PlacaDto outra = new PlacaDto();
        outra.setId(30L);
        outra.setIdObra(99L);
        when(repository.placa(30L)).thenReturn(Optional.of(outra));
        assertThrows(NaoEncontradoException.class, () -> service.alterar(30L, form()));
        assertThrows(NaoEncontradoException.class, () -> service.excluir(30L));
        verify(repository, never()).excluirPlaca(anyLong());
    }

    @Test
    void modeloPrecisaDeFormatoEmObjetoEExigeAdmin() throws Exception {
        PlacaModeloForm f = new PlacaModeloForm();
        f.setNmModelo("Aviso");
        f.setConfig(mapper.readTree("[1,2]"));
        assertThrows(EntradaInvalidaException.class, () -> service.criarModelo(f));
        verify(autorizacao).exigir(Modulo.PLACAS, Acao.ADMIN);
        verify(repository, never()).inserirModelo(anyString(), anyString(), anyBoolean(), anyLong());
    }

    @Test
    void modeloComPlacasSoDesativa() {
        when(repository.modelo(3L)).thenReturn(Optional.of(modelo(3)));
        when(repository.placasDoModelo(3L)).thenReturn(2);
        service.excluirModelo(3L);
        verify(repository).alterarModelo(eq(3L), eq("Aviso"), anyString(), eq(false), eq(1L));
        verify(repository, never()).excluirModelo(anyLong());
    }

    @Test
    void imagemSoFundoOuLogo() {
        when(repository.modelo(3L)).thenReturn(Optional.of(modelo(3)));
        assertThrows(EntradaInvalidaException.class, () -> service.removerImagem(3L, "outra"));
        assertThrows(NaoEncontradoException.class, () -> service.baixarImagem(3L, "logo"));
    }
}
