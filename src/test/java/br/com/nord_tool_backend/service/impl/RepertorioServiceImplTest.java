package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.dto.PessoaCronogramaDto;
import br.com.nord_tool_backend.dto.RepertorioCifraDto;
import br.com.nord_tool_backend.dto.RepertorioMusicaDto;
import br.com.nord_tool_backend.dto.RepertorioProjetoDto;
import br.com.nord_tool_backend.dto.RepertorioUsoDto;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.exception.NaoEncontradoException;
import br.com.nord_tool_backend.exception.NegocioException;
import br.com.nord_tool_backend.form.RepertorioCifraEnvioForm;
import br.com.nord_tool_backend.form.RepertorioMusicaForm;
import br.com.nord_tool_backend.form.RepertorioProjetoForm;
import br.com.nord_tool_backend.repository.CronogramaSemanalRepository;
import br.com.nord_tool_backend.repository.RepertorioRepository;
import br.com.nord_tool_backend.security.UsuarioAutenticado;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.storage.ArmazenamentoService;
import br.com.nord_tool_backend.storage.ArquivoConteudo;
import br.com.nord_tool_backend.storage.ArquivoDownload;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RepertorioServiceImplTest {

    private static final Long EU = 7L;
    private RepertorioRepository repository;
    private ArmazenamentoService armazenamento;
    private RepertorioServiceImpl service;
    private AutorizacaoService autorizacao;
    private CronogramaSemanalRepository pessoas;
    private static final Instant AGORA = Instant.parse("2026-11-14T12:00:00Z");

    @BeforeEach
    void setUp() {
        repository = mock(RepertorioRepository.class);
        armazenamento = mock(ArmazenamentoService.class);
        autorizacao = mock(AutorizacaoService.class);
        when(autorizacao.exigir(org.mockito.ArgumentMatchers.any(br.com.nord_tool_backend.security.Modulo.class), org.mockito.ArgumentMatchers.any(br.com.nord_tool_backend.security.Acao.class))).thenReturn(new UsuarioAutenticado(EU, "eu@obra.com", "USUARIO", Collections.emptyMap(), 0, Instant.now()));
        pessoas = mock(CronogramaSemanalRepository.class);
        service = new RepertorioServiceImpl(repository, autorizacao, armazenamento, new ObjectMapper(), pessoas, Clock.fixed(AGORA, ZoneOffset.UTC), 1);
        when(repository.buscarMusica(EU, "m1")).thenReturn(Optional.of(musica("m1", null)));
        when(repository.buscarMusica(EU, "m2")).thenReturn(Optional.of(musica("m2", 50L)));
        when(repository.buscarProjeto(EU, "p1")).thenReturn(Optional.of(projeto("p1")));
    }

    private static RepertorioMusicaDto musica(String chave, Long arquivo) {
        RepertorioMusicaDto m = new RepertorioMusicaDto();
        m.setCdChave(chave);
        m.setNmTitulo("Canção");
        m.setIdArquivo(arquivo);
        m.setTxDados("{\"cifra\":\"\"}");
        m.setInMinha(true);
        return m;
    }

    private static RepertorioProjetoDto projeto(String chave) {
        RepertorioProjetoDto p = new RepertorioProjetoDto();
        p.setCdChave(chave);
        p.setNmTitulo("Música 1");
        p.setTxDados("{}");
        p.setInMinha(true);
        return p;
    }

    private static byte[] mp3() {
        byte[] b = new byte[100];
        b[0] = 'I'; b[1] = 'D'; b[2] = '3';
        return b;
    }

    @Test
    void listaEAbreSoAsDoDono() {
        when(repository.listarMusicas(EU)).thenReturn(List.of(musica("m1", null)));
        when(repository.listarProjetos(EU)).thenReturn(List.of(projeto("p1")));
        assertEquals(1, service.listarMusicas().size());
        assertEquals(1, service.listarProjetos().size());
        assertEquals("{\"cifra\":\"\"}", service.abrirMusica("m1").getTxDados());
        assertEquals("p1", service.abrirProjeto("p1").getCdChave());
        assertThrows(NaoEncontradoException.class, () -> service.abrirMusica("outra"));
        assertThrows(NaoEncontradoException.class, () -> service.abrirProjeto("outra"));
        assertThrows(EntradaInvalidaException.class, () -> service.abrirMusica("../x"));
        assertThrows(EntradaInvalidaException.class, () -> service.abrirMusica(null));
    }

    @Test
    void salvaMusicaComLimitesEValoresPadrao() {
        RepertorioMusicaForm f = new RepertorioMusicaForm();
        f.setNmTitulo("  " + "a".repeat(250) + "  ");
        f.setNmArtista(null);
        f.setNmTom("  ");
        f.setNrBpm(new BigDecimal("120.5"));
        f.setNrDuracao(new BigDecimal("180"));
        f.setCdProjeto("p1");
        f.setTxDados("{\"ajustes\":{}}");
        RepertorioMusicaDto r = service.salvarMusica("m1", f);
        assertNull(r.getTxDados(), "volta só o resumo");
        ArgumentCaptor<RepertorioMusicaDto> cap = ArgumentCaptor.forClass(RepertorioMusicaDto.class);
        verify(repository).salvarMusica(eq(EU), eq("m1"), cap.capture());
        assertEquals(200, cap.getValue().getNmTitulo().length());
        assertEquals("", cap.getValue().getNmArtista());
        assertNull(cap.getValue().getNmTom());
        assertEquals("p1", cap.getValue().getCdProjeto());

        RepertorioMusicaForm vazio = new RepertorioMusicaForm();
        vazio.setNmTom("Em");
        vazio.setCdProjeto("");
        service.salvarMusica("m1", vazio);
        verify(repository, times(2)).salvarMusica(eq(EU), eq("m1"), cap.capture());
        RepertorioMusicaDto ultimo = cap.getAllValues().get(cap.getAllValues().size() - 1);
        assertEquals("Música", ultimo.getNmTitulo());
        assertEquals("{}", ultimo.getTxDados());
        assertEquals(BigDecimal.ZERO, ultimo.getNrDuracao());
        assertEquals("Em", ultimo.getNmTom());
        assertNull(ultimo.getCdProjeto());
        assertNull(ultimo.getNrBpm());
    }

    @Test
    void recusaDadosInvalidos() {
        RepertorioMusicaForm f = new RepertorioMusicaForm();
        f.setNrBpm(new BigDecimal("999"));
        assertThrows(EntradaInvalidaException.class, () -> service.salvarMusica("m1", f));
        f.setNrBpm(null);
        f.setNrDuracao(new BigDecimal("-1"));
        assertThrows(EntradaInvalidaException.class, () -> service.salvarMusica("m1", f));
        f.setNrDuracao(null);
        f.setTxDados("[1,2]");
        assertThrows(EntradaInvalidaException.class, () -> service.salvarMusica("m1", f));
        f.setTxDados("{quebrado");
        assertThrows(EntradaInvalidaException.class, () -> service.salvarMusica("m1", f));
        f.setTxDados("{\"x\":\"" + "a".repeat(3_000_001) + "\"}");
        assertThrows(EntradaInvalidaException.class, () -> service.salvarMusica("m1", f));
        f.setTxDados(null);
        f.setCdProjeto("com espaço");
        assertThrows(EntradaInvalidaException.class, () -> service.salvarMusica("m1", f));
    }

    @Test
    void limiteDeMusicasSoParaNovas() {
        when(repository.contarMusicas(EU)).thenReturn(RepertorioServiceImpl.MAX_MUSICAS);
        RepertorioMusicaForm f = new RepertorioMusicaForm();
        assertThrows(NegocioException.class, () -> service.salvarMusica("nova", f));
        service.salvarMusica("m1", f);
        verify(repository).salvarMusica(eq(EU), eq("m1"), any());
    }

    @Test
    void guardaOAudioETrocaOAnterior() {
        when(armazenamento.salvar(anyString(), anyString(), any())).thenReturn(99L);
        when(repository.bytesUsados(EU, "m2")).thenReturn(0L);
        service.enviarAudio("m2", "a.mp3", mp3());
        verify(armazenamento).salvar("a.mp3", "audio/mpeg", mp3());
        verify(repository).ligarAudio(EU, "m2", 99L, "a.mp3", 100);
        verify(armazenamento).apagar(50L);
        when(repository.bytesUsados(EU, "m1")).thenReturn(0L);
        service.enviarAudio("m1", "b.mp3", mp3());
        verify(armazenamento, never()).apagar(99L);
    }

    @Test
    void recusaAudioInvalidoGrandeOuSemEspaco() {
        assertThrows(EntradaInvalidaException.class, () -> service.enviarAudio("m1", "a.mp3", new byte[0]));
        assertThrows(EntradaInvalidaException.class, () -> service.enviarAudio("m1", "a.mp3", null));
        assertThrows(EntradaInvalidaException.class, () -> service.enviarAudio("m1", "a.txt", "texto qualquer".getBytes(StandardCharsets.UTF_8)));
        byte[] grande = new byte[RepertorioServiceImpl.MAX_AUDIO + 1];
        grande[0] = 'I'; grande[1] = 'D'; grande[2] = '3';
        assertThrows(EntradaInvalidaException.class, () -> service.enviarAudio("m1", "a.mp3", grande));
        // cota de 1 MB no teste
        when(repository.bytesUsados(EU, "m1")).thenReturn(1024L * 1024L - 50);
        assertThrows(NegocioException.class, () -> service.enviarAudio("m1", "a.mp3", mp3()));
        assertThrows(NaoEncontradoException.class, () -> service.enviarAudio("nao-existe", "a.mp3", mp3()));
        verify(armazenamento, never()).salvar(anyString(), anyString(), any());
    }

    @Test
    void reconheceOsFormatosDeAudio() {
        assertEquals("audio/mpeg", RepertorioServiceImpl.tipoDoAudio(mp3()));
        assertEquals("audio/mpeg", RepertorioServiceImpl.tipoDoAudio(new byte[]{(byte) 0xFF, (byte) 0xFB, 0, 0}));
        assertEquals("audio/aac", RepertorioServiceImpl.tipoDoAudio(new byte[]{(byte) 0xFF, (byte) 0xF1, 0, 0}));
        assertEquals("audio/wav", RepertorioServiceImpl.tipoDoAudio("RIFF0000WAVEfmt ".getBytes(StandardCharsets.ISO_8859_1)));
        assertEquals("audio/ogg", RepertorioServiceImpl.tipoDoAudio("OggS0000".getBytes(StandardCharsets.ISO_8859_1)));
        assertEquals("audio/flac", RepertorioServiceImpl.tipoDoAudio("fLaC0000".getBytes(StandardCharsets.ISO_8859_1)));
        assertEquals("audio/mp4", RepertorioServiceImpl.tipoDoAudio("0000ftypM4A ".getBytes(StandardCharsets.ISO_8859_1)));
        assertEquals("audio/webm", RepertorioServiceImpl.tipoDoAudio(new byte[]{0x1A, 0x45, (byte) 0xDF, (byte) 0xA3, 0}));
        assertThrows(EntradaInvalidaException.class, () -> RepertorioServiceImpl.tipoDoAudio("RIFF0000AVI ".getBytes(StandardCharsets.ISO_8859_1)));
        assertThrows(EntradaInvalidaException.class, () -> RepertorioServiceImpl.tipoDoAudio(new byte[]{1}));
    }

    @Test
    void baixaOAudioDaConta() {
        ArquivoConteudo c = new ArquivoConteudo("a.mp3", "audio/mpeg", 3, new byte[]{1, 2, 3});
        when(armazenamento.abrir(50L)).thenReturn(c);
        ArquivoDownload d = service.baixarAudio("m2");
        assertSame(c, d.getConteudo());
        assertEquals(50L, d.getVersao());
        assertArrayEquals(new byte[]{1, 2, 3}, d.getConteudo().getBytes());
        assertThrows(NaoEncontradoException.class, () -> service.baixarAudio("m1"));
    }

    @Test
    void excluiMusicaEOAudio() {
        service.excluirMusica("m2");
        verify(repository).excluirMusica(EU, "m2");
        verify(armazenamento).apagar(50L);
        service.excluirMusica("m1");
        verify(repository).excluirMusica(EU, "m1");
        service.excluirMusica("nao-existe");
        verify(repository, never()).excluirMusica(EU, "nao-existe");
    }

    @Test
    void v16AbertaATodosMasSoQuemCriouExclui() {
        RepertorioMusicaDto deOutro = musica("m9", 70L);
        deOutro.setInMinha(false);
        deOutro.setIdUsuario(42L);
        deOutro.setNmCriador("Outra Pessoa");
        when(repository.buscarMusica(EU, "m9")).thenReturn(Optional.of(deOutro));
        assertEquals("Outra Pessoa", service.abrirMusica("m9").getNmCriador());
        // editar a música de outra pessoa grava como quem mexeu
        service.salvarMusica("m9", new RepertorioMusicaForm());
        verify(repository).salvarMusica(eq(EU), eq("m9"), any());
        // o áudio conta no espaço de quem criou
        when(armazenamento.salvar(anyString(), anyString(), any())).thenReturn(98L);
        when(repository.bytesUsados(42L, "m9")).thenReturn(0L);
        service.enviarAudio("m9", "a.mp3", mp3());
        verify(repository).bytesUsados(42L, "m9");
        verify(repository).ligarAudio(EU, "m9", 98L, "a.mp3", 100);
        when(repository.bytesUsados(42L, "m9")).thenReturn(1024L * 1024L);
        NegocioException cheio = assertThrows(NegocioException.class, () -> service.enviarAudio("m9", "a.mp3", mp3()));
        assertEquals(true, cheio.getMessage().contains("quem criou"));
        // excluir, só quem criou
        assertThrows(NegocioException.class, () -> service.excluirMusica("m9"));
        verify(repository, never()).excluirMusica(EU, "m9");
        verify(armazenamento, times(1)).apagar(70L);
        RepertorioProjetoDto criacao = projeto("p9");
        criacao.setInMinha(false);
        when(repository.buscarProjeto(EU, "p9")).thenReturn(Optional.of(criacao));
        assertThrows(NegocioException.class, () -> service.excluirProjeto("p9"));
        verify(repository, never()).excluirProjeto(EU, "p9");
        service.excluirProjeto("nao-existe");
        verify(repository, never()).excluirProjeto(EU, "nao-existe");
    }

    @Test
    void usoDoEspaco() {
        when(repository.bytesUsados(EU, null)).thenReturn(1234L);
        when(repository.contarMusicas(EU)).thenReturn(3);
        RepertorioUsoDto u = service.uso();
        assertEquals(1234L, u.getBytesUsados());
        assertEquals(1024L * 1024L, u.getBytesCota());
        assertEquals(3, u.getMusicas());
        assertEquals(RepertorioServiceImpl.MAX_MUSICAS, u.getMusicasMax());
        assertEquals(RepertorioServiceImpl.MAX_AUDIO, u.getBytesPorArquivo());
    }

    @Test
    void salvaEExcluiCriacoes() {
        RepertorioProjetoForm f = new RepertorioProjetoForm();
        f.setNmTitulo(" ");
        f.setTxDados("{\"bpm\":100}");
        assertNull(service.salvarProjeto("p1", f).getTxDados());
        verify(repository).salvarProjeto(EU, "p1", "Música nova", "{\"bpm\":100}");
        when(repository.contarProjetos(EU)).thenReturn(RepertorioServiceImpl.MAX_PROJETOS);
        assertThrows(NegocioException.class, () -> service.salvarProjeto("p2", f));
        f.setTxDados("{\"x\":\"" + "a".repeat(2_000_001) + "\"}");
        assertThrows(EntradaInvalidaException.class, () -> service.salvarProjeto("p1", f));
        service.excluirProjeto("p1");
        verify(repository).excluirProjeto(EU, "p1");
    }

    @Test
    void cotaNegativaViraZeroETextoSemControle() {
        AutorizacaoService autorizacao = mock(AutorizacaoService.class);
        when(autorizacao.exigir(org.mockito.ArgumentMatchers.any(br.com.nord_tool_backend.security.Modulo.class), org.mockito.ArgumentMatchers.any(br.com.nord_tool_backend.security.Acao.class))).thenReturn(new UsuarioAutenticado(EU, "eu@obra.com", "USUARIO", Collections.emptyMap(), 0, Instant.now()));
        RepertorioServiceImpl s = new RepertorioServiceImpl(repository, autorizacao, armazenamento, new ObjectMapper(), pessoas, -5);
        assertEquals(0L, s.uso().getBytesCota());
        assertEquals("x", RepertorioServiceImpl.texto("\u0001x\u0002", 10, "p"));
    }

    // ---------------------------------------------------------------- v12: cifras compartilhadas

    private static RepertorioCifraEnvioForm envio(Long... ids) {
        RepertorioCifraEnvioForm f = new RepertorioCifraEnvioForm();
        f.setIdsUsuarios(ids == null ? null : Arrays.asList(ids));
        f.setNmTitulo("  Canção da Obra ");
        f.setNmArtista("Banda");
        f.setNmTom("G");
        f.setTxCifra("G   D\r\nLevanta a parede\rEm");
        f.setTxMensagem("Olha essa\u0007!");
        return f;
    }

    @Test
    void pessoasParaCompartilhar() {
        List<PessoaCronogramaDto> lista = Collections.singletonList(new PessoaCronogramaDto());
        when(pessoas.listarPessoas()).thenReturn(lista);
        assertSame(lista, service.pessoas());
    }

    @Test
    void compartilhaUmaCopiaParaCadaPessoaSemRepetirNemMandarParaSiMesmo() {
        when(pessoas.contarUsuariosAtivos(any())).thenReturn(2);
        assertEquals(2, service.enviarCifra(envio(3L, EU, 3L, null, 4L)).getNrEnviadas());
        verify(repository).enviarCifra(EU, 3L, "Canção da Obra", "Banda", "G", "G   D\nLevanta a parede\nEm", "Olha essa !");
        verify(repository).enviarCifra(eq(EU), eq(4L), anyString(), anyString(), anyString(), anyString(), anyString());
        verify(repository).contarCifrasEnviadasDesde(EU, LocalDateTime.of(2026, 11, 13, 12, 0));
    }

    @Test
    void compartilharSemTituloNemTomUsaOPadrao() {
        when(pessoas.contarUsuariosAtivos(any())).thenReturn(1);
        RepertorioCifraEnvioForm f = envio(3L);
        f.setNmTitulo(null);
        f.setNmArtista(null);
        f.setNmTom(" ");
        f.setTxMensagem(null);
        service.enviarCifra(f);
        verify(repository).enviarCifra(EU, 3L, "Música", "", null, "G   D\nLevanta a parede\nEm", "");
    }

    @Test
    void compartilharRecusaListaVaziaGrandeOuComInativos() {
        assertThrows(EntradaInvalidaException.class, () -> service.enviarCifra(envio((Long[]) null)));
        assertThrows(EntradaInvalidaException.class, () -> service.enviarCifra(envio(EU)));
        Long[] muitos = new Long[RepertorioServiceImpl.MAX_DESTINOS + 1];
        for (int i = 0; i < muitos.length; i++) muitos[i] = 100L + i;
        assertThrows(EntradaInvalidaException.class, () -> service.enviarCifra(envio(muitos)));
        when(pessoas.contarUsuariosAtivos(any())).thenReturn(0);
        assertThrows(EntradaInvalidaException.class, () -> service.enviarCifra(envio(3L)));
        verify(repository, never()).enviarCifra(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void compartilharRecusaCifraVaziaOuGrande() {
        RepertorioCifraEnvioForm vazia = envio(3L);
        vazia.setTxCifra("  \n ");
        assertThrows(EntradaInvalidaException.class, () -> service.enviarCifra(vazia));
        vazia.setTxCifra(null);
        assertThrows(EntradaInvalidaException.class, () -> service.enviarCifra(vazia));
        RepertorioCifraEnvioForm grande = envio(3L);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= RepertorioServiceImpl.MAX_CIFRA; i++) sb.append('a');
        grande.setTxCifra(sb.toString());
        assertThrows(EntradaInvalidaException.class, () -> service.enviarCifra(grande));
    }

    @Test
    void compartilharRespeitaOLimiteDoDia() {
        when(pessoas.contarUsuariosAtivos(any())).thenReturn(2);
        when(repository.contarCifrasEnviadasDesde(eq(EU), any())).thenReturn(RepertorioServiceImpl.MAX_ENVIOS_DIA - 1);
        assertThrows(NegocioException.class, () -> service.enviarCifra(envio(3L, 4L)));
        verify(repository, never()).enviarCifra(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void listaAsRecebidas() {
        List<RepertorioCifraDto> lista = Collections.singletonList(new RepertorioCifraDto());
        when(repository.listarCifrasRecebidas(EU)).thenReturn(lista);
        assertSame(lista, service.cifrasRecebidas());
    }

    @Test
    void abrirMarcaComoLidaSoNaPrimeiraVez() {
        RepertorioCifraDto nova = new RepertorioCifraDto();
        nova.setTxCifra("G");
        when(repository.buscarCifraRecebida(EU, 9L)).thenReturn(Optional.of(nova));
        assertEquals(LocalDateTime.of(2026, 11, 14, 12, 0), service.abrirCifraRecebida(9L).getDhLeitura());
        verify(repository).marcarCifraLida(EU, 9L);
        RepertorioCifraDto lida = new RepertorioCifraDto();
        lida.setDhLeitura(LocalDateTime.of(2026, 1, 1, 0, 0));
        when(repository.buscarCifraRecebida(EU, 10L)).thenReturn(Optional.of(lida));
        service.abrirCifraRecebida(10L);
        verify(repository, never()).marcarCifraLida(EU, 10L);
        assertThrows(NaoEncontradoException.class, () -> service.abrirCifraRecebida(11L));
    }

    @Test
    void excluiSoADaPropriaPessoa() {
        service.excluirCifraRecebida(9L);
        verify(repository).excluirCifraRecebida(EU, 9L);
    }

    @Test
    void verExigeOModuloRepertorioComLeitura() {
        try {
            service.listarMusicas();
        } catch (RuntimeException ignorado) {
            // só interessa o acesso pedido
        }
        org.mockito.Mockito.verify(autorizacao, org.mockito.Mockito.atLeastOnce())
                .exigir(br.com.nord_tool_backend.security.Modulo.REPERTORIO, br.com.nord_tool_backend.security.Acao.LEITURA);
    }

    @Test
    void semEditarNaoExcluiMusica() {
        org.mockito.Mockito.when(autorizacao.exigir(br.com.nord_tool_backend.security.Modulo.REPERTORIO, br.com.nord_tool_backend.security.Acao.ESCRITA))
                .thenThrow(new br.com.nord_tool_backend.exception.AcessoNegadoException("sem acesso"));
        org.junit.jupiter.api.Assertions.assertThrows(br.com.nord_tool_backend.exception.AcessoNegadoException.class, () -> service.excluirMusica("abc"));
    }
}
