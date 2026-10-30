package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.dto.LugiaConversaDto;
import br.com.nord_tool_backend.dto.LugiaMensagemDto;
import br.com.nord_tool_backend.dto.LugiaModelosDto;
import br.com.nord_tool_backend.dto.LugiaParteDto;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.exception.NaoEncontradoException;
import br.com.nord_tool_backend.exception.NegocioException;
import br.com.nord_tool_backend.form.LugiaAnexoForm;
import br.com.nord_tool_backend.form.LugiaChatForm;
import br.com.nord_tool_backend.form.LugiaTituloForm;
import br.com.nord_tool_backend.repository.LugiaRepository;
import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.security.UsuarioAutenticado;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.service.lugia.LugiaAgenteClient;
import br.com.nord_tool_backend.service.lugia.LugiaFalhaException;
import br.com.nord_tool_backend.service.lugia.LugiaIaProvider;
import br.com.nord_tool_backend.service.lugia.LugiaProperties;
import br.com.nord_tool_backend.service.lugia.LugiaResposta;
import br.com.nord_tool_backend.service.lugia.LugiaSaida;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LugiaServiceImplTest {

    private LugiaRepository repository;
    private AutorizacaoService autorizacao;
    private LugiaIaProvider claude;
    private LugiaIaProvider gemini;
    private LugiaAgenteClient agente;
    private LugiaServiceImpl service;
    private SaidaGravada saida;
    private MutableClock relogio;

    @BeforeEach
    void setUp() {
        repository = mock(LugiaRepository.class);
        autorizacao = mock(AutorizacaoService.class);
        claude = provedor("claude", "Claude", true, "claude-opus-5-5");
        gemini = provedor("gemini", "Gemini", false, "gemini-flash-latest");
        agente = mock(LugiaAgenteClient.class);
        relogio = new MutableClock(Instant.parse("2026-10-29T12:00:00Z"));
        service = new LugiaServiceImpl(repository, autorizacao, Arrays.asList(claude, gemini), agente, propriedades(""), relogio);
        saida = new SaidaGravada();
        logado(Acao.LEITURA);
        when(repository.criarConversa(eq(7L), anyString())).thenReturn(30L);
    }

    private static LugiaIaProvider provedor(String id, String nome, boolean disponivel, String modelo) {
        LugiaIaProvider p = mock(LugiaIaProvider.class);
        when(p.id()).thenReturn(id);
        when(p.nome()).thenReturn(nome);
        when(p.disponivel()).thenReturn(disponivel);
        when(p.modelos()).thenReturn(Collections.singletonList(modelo));
        return p;
    }

    private static LugiaProperties propriedades(String cadeiaAutomatico) {
        return new LugiaProperties("sk-ant", "", "", "claude-opus-5-5", "", "gemini-flash-latest", 1,
                "ghp", "", "claude-opus-5-5", "", "", "", "", "", "", "", "", "", "", "", cadeiaAutomatico);
    }

    private static LugiaIaProvider gratuito(String id, String nome, boolean leAnexos, String... modelos) {
        LugiaIaProvider p = mock(LugiaIaProvider.class);
        when(p.id()).thenReturn(id);
        when(p.nome()).thenReturn(nome);
        when(p.disponivel()).thenReturn(true);
        when(p.gratuito()).thenReturn(true);
        when(p.leAnexos()).thenReturn(leAnexos);
        when(p.modelos()).thenReturn(Arrays.asList(modelos));
        return p;
    }

    private LugiaServiceImpl comGratuitos(String cadeia, LugiaIaProvider... gratuitos) {
        List<LugiaIaProvider> todos = new ArrayList<>(Collections.singletonList(claude));
        todos.addAll(Arrays.asList(gratuitos));
        return new LugiaServiceImpl(repository, autorizacao, todos, agente, propriedades(cadeia), relogio);
    }

    private static void responde(LugiaIaProvider p, String texto) {
        when(p.conversar(anyString(), anyString(), anyList(), any())).thenAnswer(inv -> {
            Consumer<String> aoTexto = inv.getArgument(3);
            aoTexto.accept(texto);
            return new LugiaResposta(texto, null, "stop", 4, 2);
        });
    }

    private static void noLimite(LugiaIaProvider p, long pausaSegundos) {
        LugiaFalhaException limite = new LugiaFalhaException("Limite de uso do " + p.nome() + " atingido; tente em instantes.", null, pausaSegundos);
        when(p.conversar(anyString(), anyString(), anyList(), any())).thenThrow(limite);
    }

    private static LugiaChatForm automatico(String texto) {
        LugiaChatForm f = chat(texto);
        f.setModelo("automatico");
        return f;
    }

    private UsuarioAutenticado logado(Acao nivel) {
        Map<Modulo, Acao> permissoes = new EnumMap<>(Modulo.class);
        permissoes.put(Modulo.LUGIA, nivel);
        UsuarioAutenticado u = new UsuarioAutenticado(7L, "nick@nord.com", "ADMIN", permissoes, 0, Instant.now());
        when(autorizacao.exigir(eq(Modulo.LUGIA), any())).thenReturn(u);
        return u;
    }

    private static LugiaChatForm chat(String texto) {
        LugiaChatForm f = new LugiaChatForm();
        f.setModelo("claude-opus-5-5");
        f.setTexto(texto);
        return f;
    }

    private static LugiaAnexoForm anexo(String mime, String base64) {
        LugiaAnexoForm a = new LugiaAnexoForm();
        a.setNome("print.png");
        a.setMime(mime);
        a.setBase64(base64);
        return a;
    }

    @SuppressWarnings("unchecked")
    private void claudeResponde(String... pedacos) {
        when(claude.conversar(anyString(), anyString(), anyList(), any())).thenAnswer(inv -> {
            Consumer<String> aoTexto = inv.getArgument(3);
            StringBuilder total = new StringBuilder();
            for (String p : pedacos) {
                aoTexto.accept(p);
                total.append(p);
            }
            return new LugiaResposta(total.toString(), "{\"content\":[]}", "end_turn", 10, 3);
        });
    }

    @Test
    void modelosListaSoProvedoresDisponiveisESemAgenteParaLeitura() {
        when(agente.disponivel()).thenReturn(true);

        LugiaModelosDto dto = service.modelos();

        assertEquals(1, dto.getModelos().size());
        assertEquals("claude-opus-5-5", dto.getModelos().get(0).getId());
        assertEquals("Claude · claude-opus-5-5", dto.getModelos().get(0).getNome());
        assertEquals("LEITURA", dto.getNivel());
        assertFalse(dto.isAgenteDisponivel());
    }

    @Test
    void modelosLiberaAgenteParaAdminComAgenteConfigurado() {
        logado(Acao.ADMIN);
        when(agente.disponivel()).thenReturn(true);

        assertTrue(service.modelos().isAgenteDisponivel());
    }

    @Test
    void conversarCriaConversaTransmiteEGravaAsDuasMensagens() {
        claudeResponde("Olá, ", "Nick!");

        service.conversar(chat("Oi Lugia"), saida);

        assertEquals(Arrays.asList("abrir", "conversa:30", "texto:Olá, ", "texto:Nick!", "fim:end_turn"), saida.eventos);
        verify(repository).criarConversa(7L, "Oi Lugia");
        ArgumentCaptor<LugiaMensagemDto> msgs = ArgumentCaptor.forClass(LugiaMensagemDto.class);
        verify(repository, times(2)).adicionarMensagem(eq(30L), msgs.capture(), any(), any(), any());
        assertEquals(LugiaMensagemDto.USUARIO, msgs.getAllValues().get(0).getPapel());
        LugiaMensagemDto resposta = msgs.getAllValues().get(1);
        assertEquals("claude", resposta.getProvedor());
        assertEquals("Olá, Nick!", resposta.getPartes().get(0).getTexto());
        assertEquals("{\"content\":[]}", resposta.getNativo());
        verify(repository).adicionarMensagem(eq(30L), eq(resposta), eq("claude-opus-5-5"), eq(10), eq(3));
    }

    @Test
    @SuppressWarnings("unchecked")
    void conversarContinuaConversaComHistoricoCompleto() {
        LugiaConversaDto conversa = new LugiaConversaDto();
        conversa.setId(5L);
        conversa.setTitulo("Anterior");
        when(repository.buscarConversa(7L, 5L)).thenReturn(Optional.of(conversa));
        when(repository.mensagens(5L)).thenReturn(new ArrayList<>(Arrays.asList(
                new LugiaMensagemDto("usuario", null, Collections.singletonList(LugiaParteDto.texto("Oi")), null),
                new LugiaMensagemDto("assistente", "gemini", Collections.singletonList(LugiaParteDto.texto("Olá")), null))));
        claudeResponde("Tudo certo");
        LugiaChatForm form = chat("E aí?");
        form.setIdConversa(5L);

        service.conversar(form, saida);

        ArgumentCaptor<List<LugiaMensagemDto>> historico = ArgumentCaptor.forClass(List.class);
        verify(claude).conversar(eq("claude-opus-5-5"), anyString(), historico.capture(), any());
        assertEquals(3, historico.getValue().size());
        assertEquals("E aí?", historico.getValue().get(2).getPartes().get(0).getTexto());
        verify(repository, never()).criarConversa(anyLong(), anyString());
    }

    @Test
    void conversarRecusaAntesDeAbrirTransmissao() {
        LugiaChatForm modeloInexistente = chat("oi");
        modeloInexistente.setModelo("gpt-x");
        assertThrows(EntradaInvalidaException.class, () -> service.conversar(modeloInexistente, saida));

        LugiaChatForm provedorSemChave = chat("oi");
        provedorSemChave.setModelo("gemini-flash-latest");
        assertThrows(EntradaInvalidaException.class, () -> service.conversar(provedorSemChave, saida));

        assertThrows(EntradaInvalidaException.class, () -> service.conversar(chat("   "), saida));

        LugiaChatForm outraPessoa = chat("oi");
        outraPessoa.setIdConversa(99L);
        when(repository.buscarConversa(7L, 99L)).thenReturn(Optional.empty());
        assertThrows(NaoEncontradoException.class, () -> service.conversar(outraPessoa, saida));

        assertTrue(saida.eventos.isEmpty());
        verify(repository, never()).adicionarMensagem(anyLong(), any(), any(), any(), any());
    }

    @Test
    void anexosValidamTipoQuantidadeETamanho() {
        LugiaChatForm tipo = chat("veja");
        tipo.getAnexos().add(anexo("text/html", "AAAA"));
        assertThrows(EntradaInvalidaException.class, () -> service.conversar(tipo, saida));

        LugiaChatForm invalido = chat("veja");
        invalido.getAnexos().add(anexo("image/png", "não é base64"));
        assertThrows(EntradaInvalidaException.class, () -> service.conversar(invalido, saida));

        LugiaChatForm muitos = chat("veja");
        for (int i = 0; i < 6; i++) muitos.getAnexos().add(anexo("image/png", "AAAA"));
        assertThrows(EntradaInvalidaException.class, () -> service.conversar(muitos, saida));

        LugiaChatForm grande = chat("veja");
        char[] dados = new char[1_500_000];
        Arrays.fill(dados, 'A');
        grande.getAnexos().add(anexo("image/png", new String(dados)));
        assertThrows(EntradaInvalidaException.class, () -> service.conversar(grande, saida));
    }

    @Test
    void anexosValidosViramPartesAntesDoTextoSemPrefixoDataUrl() {
        LugiaChatForm form = chat("Quero mudar isto");
        form.getAnexos().add(anexo("image/png", "data:image/png;base64,AAAA"));

        List<LugiaParteDto> partes = service.partes(form);

        assertEquals(2, partes.size());
        assertEquals(LugiaParteDto.IMAGEM, partes.get(0).getTipo());
        assertEquals("AAAA", partes.get(0).getBase64());
        assertEquals("Quero mudar isto", partes.get(1).getTexto());
    }

    @Test
    void falhaDoProvedorGravaRespostaParcialEAvisa() {
        when(claude.conversar(anyString(), anyString(), anyList(), any())).thenAnswer(inv -> {
            Consumer<String> aoTexto = inv.getArgument(3);
            aoTexto.accept("Começando");
            throw new LugiaFalhaException("Limite de uso do Claude atingido; tente em instantes.");
        });

        service.conversar(chat("oi"), saida);

        assertEquals("erro:Limite de uso do Claude atingido; tente em instantes.", saida.eventos.get(saida.eventos.size() - 1));
        ArgumentCaptor<LugiaMensagemDto> msgs = ArgumentCaptor.forClass(LugiaMensagemDto.class);
        verify(repository, times(2)).adicionarMensagem(eq(30L), msgs.capture(), any(), any(), any());
        assertEquals("Começando", msgs.getAllValues().get(1).getPartes().get(0).getTexto());
    }

    @Test
    void modoAgenteExigeAdmin() {
        LugiaChatForm form = chat("Mude o título da tela de medições");
        form.setModo("agente");
        when(agente.disponivel()).thenReturn(false);

        assertThrows(NegocioException.class, () -> service.conversar(form, saida));

        verify(autorizacao).exigir(Modulo.LUGIA, Acao.ADMIN);
        assertTrue(saida.eventos.isEmpty());
    }

    @Test
    void modoAgenteCriaSessaoNaPrimeiraMensagemETransmiteProgresso() {
        logado(Acao.ADMIN);
        when(agente.disponivel()).thenReturn(true);
        when(agente.criarSessao("Mude o título")).thenReturn("sesn_1");
        when(agente.executar(eq("sesn_1"), anyList(), any())).thenAnswer(inv -> {
            LugiaSaida s = inv.getArgument(2);
            s.ferramenta("bash: git status");
            s.texto("PR aberto");
            return new LugiaResposta("PR aberto", null, "end_turn", null, null);
        });
        LugiaChatForm form = chat("Mude o título");
        form.setModo("agente");
        form.setModelo(null);

        service.conversar(form, saida);

        verify(repository).definirSessaoAgente(30L, "sesn_1");
        assertEquals(Arrays.asList("abrir", "conversa:30", "ferramenta:bash: git status", "texto:PR aberto", "fim:end_turn"), saida.eventos);
        ArgumentCaptor<LugiaMensagemDto> msgs = ArgumentCaptor.forClass(LugiaMensagemDto.class);
        verify(repository, times(2)).adicionarMensagem(eq(30L), msgs.capture(), any(), isNull(), isNull());
        assertEquals("agente", msgs.getAllValues().get(1).getProvedor());
        verify(claude, never()).conversar(anyString(), anyString(), anyList(), any());
    }

    @Test
    void modoAgenteReaproveitaSessaoDaConversa() {
        logado(Acao.ADMIN);
        when(agente.disponivel()).thenReturn(true);
        LugiaConversaDto conversa = new LugiaConversaDto();
        conversa.setId(5L);
        conversa.setTitulo("Ajustes");
        conversa.setSessaoAgente("sesn_9");
        when(repository.buscarConversa(7L, 5L)).thenReturn(Optional.of(conversa));
        when(repository.mensagens(5L)).thenReturn(new ArrayList<>());
        when(agente.executar(eq("sesn_9"), anyList(), any())).thenReturn(new LugiaResposta("Feito", null, "end_turn", null, null));
        LugiaChatForm form = chat("Agora o botão");
        form.setModo("agente");
        form.setIdConversa(5L);

        service.conversar(form, saida);

        verify(agente, never()).criarSessao(anyString());
        verify(repository, never()).definirSessaoAgente(anyLong(), anyString());
    }

    @Test
    void mensagensTiramBase64EExigemConversaDoUsuario() {
        LugiaConversaDto conversa = new LugiaConversaDto();
        conversa.setId(5L);
        when(repository.buscarConversa(7L, 5L)).thenReturn(Optional.of(conversa));
        when(repository.mensagens(5L)).thenReturn(Collections.singletonList(new LugiaMensagemDto("usuario", null, Arrays.asList(
                new LugiaParteDto(LugiaParteDto.IMAGEM, null, "image/png", "AAAA", "print.png"), LugiaParteDto.texto("Veja")), "{}")));

        List<LugiaMensagemDto> mensagens = service.mensagens(5L);

        assertNull(mensagens.get(0).getPartes().get(0).getBase64());
        assertEquals("print.png", mensagens.get(0).getPartes().get(0).getNome());
        assertEquals("Veja", mensagens.get(0).getPartes().get(1).getTexto());
        assertNull(mensagens.get(0).getNativo());

        when(repository.buscarConversa(7L, 6L)).thenReturn(Optional.empty());
        assertThrows(NaoEncontradoException.class, () -> service.mensagens(6L));
    }

    @Test
    void renomearEExcluirSoDoProprioUsuario() {
        LugiaTituloForm form = new LugiaTituloForm();
        form.setTitulo("  Novo título ");
        when(repository.renomear(7L, 5L, "Novo título")).thenReturn(true);
        service.renomear(5L, form);

        when(repository.renomear(7L, 6L, "Novo título")).thenReturn(false);
        assertThrows(NaoEncontradoException.class, () -> service.renomear(6L, form));

        when(repository.excluir(7L, 5L)).thenReturn(true);
        service.excluir(5L);
        assertThrows(NaoEncontradoException.class, () -> service.excluir(8L));
    }

    @Test
    void tituloInicialUsaTextoOuNomeDoAnexo() {
        assertEquals("oi lugia", LugiaServiceImpl.tituloInicial("  oi   lugia ", Collections.emptyList()));
        assertEquals(60, LugiaServiceImpl.tituloInicial(new String(new char[80]).replace('\0', 'x'), Collections.emptyList()).length());
        assertEquals("termo.pdf", LugiaServiceImpl.tituloInicial("", Collections.singletonList(
                new LugiaParteDto(LugiaParteDto.PDF, null, null, "AAAA", "termo.pdf"))));
    }

    @Test
    void modelosOfereceAutomaticoPrimeiroSoComProvedorGratuito() {
        assertFalse(service.modelos().getModelos().stream().anyMatch(m -> "automatico".equals(m.getId())));

        LugiaModelosDto dto = comGratuitos("", gratuito("groq", "Groq", false, "gpt-oss-120b")).modelos();

        assertEquals("automatico", dto.getModelos().get(0).getId());
        assertEquals("automatico", dto.getModelos().get(0).getProvedor());
        assertEquals(Arrays.asList("automatico", "claude-opus-5-5", "gpt-oss-120b"),
                dto.getModelos().stream().map(m -> m.getId()).collect(java.util.stream.Collectors.toList()));
    }

    @Test
    void automaticoSemGratuitoDisponivelRecusa() {
        assertThrows(EntradaInvalidaException.class, () -> service.conversar(automatico("oi"), saida));
        assertTrue(saida.eventos.isEmpty());
    }

    @Test
    void automaticoPassaParaOProximoQuandoUmAtingeOLimiteAntesDeResponder() {
        LugiaIaProvider groq = gratuito("groq", "Groq", false, "gpt-oss-120b");
        LugiaIaProvider mistral = gratuito("mistral", "Mistral", false, "mistral-small-latest");
        noLimite(groq, 3600);
        responde(mistral, "Oi!");

        comGratuitos("", groq, mistral).conversar(automatico("oi"), saida);

        assertEquals(Arrays.asList("abrir", "conversa:30", "provedor:groq/gpt-oss-120b",
                "ferramenta:Groq indisponível agora; passando para Mistral", "provedor:mistral/mistral-small-latest",
                "texto:Oi!", "fim:stop"), saida.eventos);
        verify(claude, never()).conversar(anyString(), anyString(), anyList(), any());
        ArgumentCaptor<LugiaMensagemDto> msgs = ArgumentCaptor.forClass(LugiaMensagemDto.class);
        verify(repository).adicionarMensagem(eq(30L), msgs.capture(), eq("mistral-small-latest"), eq(4), eq(2));
        assertEquals("mistral", msgs.getValue().getProvedor());
    }

    @Test
    void responderUmaVezPassaParaOProximoESemGravarConversa() {
        LugiaIaProvider groq = gratuito("groq", "Groq", false, "gpt-oss-120b");
        LugiaIaProvider mistral = gratuito("mistral", "Mistral", false, "mistral-small-latest");
        noLimite(groq, 3600);
        responde(mistral, "{\"demandas\":[]}");

        assertEquals("{\"demandas\":[]}", comGratuitos("", groq, mistral).responderUmaVez("sistema", "pedido"));

        verify(mistral).conversar(eq("mistral-small-latest"), eq("sistema"), anyList(), any());
        verify(claude, never()).conversar(anyString(), anyString(), anyList(), any());
        verify(repository, never()).criarConversa(anyLong(), anyString());
    }

    @Test
    void responderUmaVezComTodasNoLimiteDaErroDeNegocio() {
        LugiaIaProvider groq = gratuito("groq", "Groq", false, "gpt-oss-120b");
        noLimite(groq, 3600);
        assertThrows(NegocioException.class, () -> comGratuitos("", groq).responderUmaVez("s", "p"));
        assertThrows(NegocioException.class, () -> service.responderUmaVez("s", "p"));
    }

    @Test
    void automaticoDeixaQuemAtingiuOLimiteNoFimAteAPausaAcabar() {
        LugiaIaProvider groq = gratuito("groq", "Groq", false, "gpt-oss-120b");
        LugiaIaProvider mistral = gratuito("mistral", "Mistral", false, "mistral-small-latest");
        noLimite(groq, 120);
        responde(mistral, "Oi!");
        LugiaServiceImpl auto = comGratuitos("", groq, mistral);
        auto.conversar(automatico("oi"), saida);

        relogio.avancar(60);
        auto.conversar(automatico("de novo"), new SaidaGravada());
        verify(groq, times(1)).conversar(anyString(), anyString(), anyList(), any());
        verify(mistral, times(2)).conversar(anyString(), anyString(), anyList(), any());

        relogio.avancar(61);
        auto.conversar(automatico("e agora"), new SaidaGravada());
        verify(groq, times(2)).conversar(anyString(), anyString(), anyList(), any());
    }

    @Test
    void automaticoNaoTrocaDepoisQueARespostaComecou() {
        LugiaIaProvider groq = gratuito("groq", "Groq", false, "gpt-oss-120b");
        LugiaIaProvider mistral = gratuito("mistral", "Mistral", false, "mistral-small-latest");
        when(groq.conversar(anyString(), anyString(), anyList(), any())).thenAnswer(inv -> {
            Consumer<String> aoTexto = inv.getArgument(3);
            aoTexto.accept("Come");
            throw new LugiaFalhaException("Falha no Groq (500).");
        });

        comGratuitos("", groq, mistral).conversar(automatico("oi"), saida);

        assertEquals("erro:Falha no Groq (500).", saida.eventos.get(saida.eventos.size() - 1));
        verify(mistral, never()).conversar(anyString(), anyString(), anyList(), any());
        verify(repository).adicionarMensagem(eq(30L), any(), eq("gpt-oss-120b"), isNull(), isNull());
    }

    @Test
    void automaticoAvisaQuandoTodosEstaoNoLimite() {
        LugiaIaProvider groq = gratuito("groq", "Groq", false, "gpt-oss-120b");
        LugiaIaProvider mistral = gratuito("mistral", "Mistral", false, "mistral-small-latest");
        noLimite(groq, 0);
        noLimite(mistral, 0);

        comGratuitos("", groq, mistral).conversar(automatico("oi"), saida);

        assertEquals("erro:Todas as IAs gratuitas estão no limite agora. Tente de novo em alguns minutos ou escolha outro modelo.",
                saida.eventos.get(saida.eventos.size() - 1));
        verify(repository, times(1)).adicionarMensagem(anyLong(), any(), any(), any(), any());
    }

    @Test
    void automaticoComAnexoComecaPorQuemLeImagemEPdf() {
        LugiaIaProvider groq = gratuito("groq", "Groq", false, "gpt-oss-120b");
        LugiaIaProvider geminiGratis = gratuito("gemini", "Gemini", true, "gemini-flash-latest");
        responde(geminiGratis, "Vi o print");
        LugiaChatForm form = automatico("O que tem aqui?");
        form.getAnexos().add(anexo("image/png", "AAAA"));

        comGratuitos("", groq, geminiGratis).conversar(form, saida);

        assertEquals("provedor:gemini/gemini-flash-latest", saida.eventos.get(2));
        verify(groq, never()).conversar(anyString(), anyString(), anyList(), any());
    }

    @Test
    void cadeiaConfiguradaDefineOrdemEIgnoraModelosIndisponiveis() {
        LugiaIaProvider groq = gratuito("groq", "Groq", false, "gpt-oss-120b", "llama-3.1-8b-instant");
        LugiaIaProvider mistral = gratuito("mistral", "Mistral", false, "mistral-small-latest");

        List<LugiaServiceImpl.Opcao> cadeia = comGratuitos("mistral-small-latest, claude-opus-5-5, inexistente, llama-3.1-8b-instant",
                groq, mistral).cadeiaAutomatico();

        assertEquals(Arrays.asList("mistral:mistral-small-latest", "groq:llama-3.1-8b-instant"),
                cadeia.stream().map(LugiaServiceImpl.Opcao::chave).collect(java.util.stream.Collectors.toList()));
    }

    @Test
    void modeloEscolhidoSemAutomaticoNaoTrocaDeProvedor() {
        LugiaIaProvider groq = gratuito("groq", "Groq", false, "gpt-oss-120b");
        LugiaIaProvider mistral = gratuito("mistral", "Mistral", false, "mistral-small-latest");
        noLimite(groq, 60);
        LugiaChatForm form = chat("oi");
        form.setModelo("gpt-oss-120b");

        comGratuitos("", groq, mistral).conversar(form, saida);

        assertEquals("erro:Limite de uso do Groq atingido; tente em instantes.", saida.eventos.get(saida.eventos.size() - 1));
        verify(mistral, never()).conversar(anyString(), anyString(), anyList(), any());
    }

    /** Relógio que só anda quando o teste manda. */
    private static final class MutableClock extends Clock {
        private Instant agora;

        MutableClock(Instant agora) { this.agora = agora; }

        void avancar(long segundos) { agora = agora.plusSeconds(segundos); }

        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return agora; }
    }

    /** Saída que só registra os eventos em ordem. */
    private static final class SaidaGravada implements LugiaSaida {
        final List<String> eventos = new ArrayList<>();

        @Override public void abrir() { eventos.add("abrir"); }
        @Override public void conversa(Long idConversa) { eventos.add("conversa:" + idConversa); }
        @Override public void texto(String delta) { eventos.add("texto:" + delta); }
        @Override public void ferramenta(String descricao) { eventos.add("ferramenta:" + descricao); }
        @Override public void provedor(String id, String modelo) { eventos.add("provedor:" + id + "/" + modelo); }
        @Override public void fim(String motivo) { eventos.add("fim:" + motivo); }
        @Override public void erro(String mensagem) { eventos.add("erro:" + mensagem); }
    }
}
