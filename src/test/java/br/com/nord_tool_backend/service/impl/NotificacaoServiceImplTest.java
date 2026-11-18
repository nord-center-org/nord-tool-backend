package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.dto.DemandaAvisoDto;
import br.com.nord_tool_backend.dto.NotificacaoPreferenciaDto;
import br.com.nord_tool_backend.dto.PushInscricaoDto;
import br.com.nord_tool_backend.repository.NotificacaoRepository;
import br.com.nord_tool_backend.security.UsuarioAutenticado;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.service.push.PushEnviador;
import com.fasterxml.jackson.databind.ObjectMapper;
import br.com.nord_tool_backend.service.push.WebPushCripto;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificacaoServiceImplTest {

    private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");
    private NotificacaoRepository repository;
    private PushEnviador enviador;
    private br.com.nord_tool_backend.service.AcessoService acessos;
    private final UsuarioAutenticado nick = new UsuarioAutenticado(1L, "n@obra.com", "ADMIN", Collections.emptyMap(), 0, java.time.Instant.now());

    @BeforeEach
    void preparar() {
        repository = mock(NotificacaoRepository.class);
        enviador = mock(PushEnviador.class);
        acessos = mock(br.com.nord_tool_backend.service.AcessoService.class);
        doAnswer(inv -> { ((Runnable) inv.getArgument(0)).run(); return null; }).when(enviador).emSegundoPlano(any());
        when(repository.nomeDoUsuario(anyLong())).thenReturn("Nick Silva");
        // Cada pessoa tem um aparelho com o id dela.
        when(repository.inscricoes(any())).thenAnswer(i -> ((Collection<Long>) i.getArgument(0)).stream().map(id -> {
            PushInscricaoDto a = new PushInscricaoDto();
            a.setId(id);
            a.setIdUsuario(id);
            return a;
        }).collect(Collectors.toList()));
    }

    private NotificacaoServiceImpl servico(LocalDateTime agora) {
        Clock c = Clock.fixed(agora.atZone(SP).toInstant(), SP);
        return new NotificacaoServiceImpl(repository, mock(AutorizacaoService.class), enviador, new ObjectMapper(), acessos, c);
    }

    private static NotificacaoPreferenciaDto pref(long id) {
        NotificacaoPreferenciaDto p = new NotificacaoPreferenciaDto();
        p.setIdUsuario(id);
        return p;
    }

    private static DemandaAvisoDto demanda(LocalDateTime agendada, String envolvidos) {
        DemandaAvisoDto d = new DemandaAvisoDto();
        d.setId(5L);
        d.setNmCronogramaSemanal("Reunião de obra");
        d.setDtAgendamento(agendada);
        d.setTxEnvolvidos(envolvidos);
        return d;
    }

    @SuppressWarnings("unchecked")
    private List<Long> destinatarios(int vezes) {
        ArgumentCaptor<List<PushInscricaoDto>> cap = ArgumentCaptor.forClass(List.class);
        verify(enviador, times(vezes)).enviar(cap.capture(), anyString());
        return cap.getAllValues().stream().flatMap(List::stream).map(PushInscricaoDto::getIdUsuario).collect(Collectors.toList());
    }

    @Test
    void demandaNovaVaiParaDonoECompartilhadosConformeAPreferencia() {
        NotificacaoPreferenciaDto semNovas = pref(3);
        semNovas.setInDemandaNova(false);
        when(repository.demanda(5L)).thenReturn(Optional.of(demanda(null, "1,2,3")));
        when(repository.preferencias(any())).thenReturn(Arrays.asList(pref(1), pref(2), semNovas));
        servico(LocalDateTime.of(2026, 11, 5, 9, 0)).demandaCriada(5L, nick);
        assertEquals(Arrays.asList(1L, 2L), destinatarios(1));
    }

    @Test
    void quemFezSoRecebeSePediuOQueEleMesmoFez() {
        NotificacaoPreferenciaDto eu = pref(1);
        eu.setInMinhasAcoes(false);
        when(repository.demanda(5L)).thenReturn(Optional.of(demanda(null, "1,2")));
        when(repository.preferencias(any())).thenReturn(Arrays.asList(eu, pref(2)));
        servico(LocalDateTime.of(2026, 11, 5, 9, 0)).demandaAlterada(5L, nick, "etapa:executar");
        assertEquals(Collections.singletonList(2L), destinatarios(1));
        ArgumentCaptor<String> conteudo = ArgumentCaptor.forClass(String.class);
        verify(enviador).enviar(any(), conteudo.capture());
        assertTrue(conteudo.getValue().contains("Nick moveu “Reunião de obra” para Executar"), conteudo.getValue());
    }

    @Test
    void horarioAvisaComAAntecedenciaDeCadaUmESoUmaVez() {
        LocalDateTime agora = LocalDateTime.of(2026, 11, 5, 9, 40);
        DemandaAvisoDto d = demanda(LocalDateTime.of(2026, 11, 5, 10, 0), "1,2");
        NotificacaoPreferenciaDto trinta = pref(1); // 30 min antes: faltam 20 → avisa
        NotificacaoPreferenciaDto dez = pref(2);
        dez.setNrAntecedenciaMin(10); // faltam 20 → ainda não
        when(repository.demandasAgendadas(any(), any())).thenReturn(Collections.singletonList(d));
        when(repository.demandasComPrazo(any())).thenReturn(Collections.emptyList());
        when(repository.preferencias(any())).thenReturn(Arrays.asList(trinta, dez));
        when(repository.registrarEnvio(eq(1L), eq("horario:5:2026-11-05T10:00"))).thenReturn(true, false);
        NotificacaoServiceImpl s = servico(agora);
        s.lembretes();
        s.lembretes(); // segunda volta do agendador: não repete
        assertEquals(Collections.singletonList(1L), destinatarios(1));
        verify(repository, never()).registrarEnvio(eq(2L), anyString());
        ArgumentCaptor<String> conteudo = ArgumentCaptor.forClass(String.class);
        verify(enviador).enviar(any(), conteudo.capture());
        assertTrue(conteudo.getValue().contains("Daqui a 20 min: Reunião de obra"), conteudo.getValue());
    }

    @Test
    void prazoDoDiaSoDepoisDasOito() {
        DemandaAvisoDto d = demanda(null, "1");
        when(repository.demandasAgendadas(any(), any())).thenReturn(Collections.emptyList());
        when(repository.demandasComPrazo(any())).thenReturn(Collections.singletonList(d));
        when(repository.preferencias(any())).thenReturn(Collections.singletonList(pref(1)));
        when(repository.registrarEnvio(eq(1L), eq("prazo:5:2026-11-05"))).thenReturn(true);
        servico(LocalDateTime.of(2026, 11, 5, 7, 30)).lembretes();
        verify(repository, never()).demandasComPrazo(any());
        servico(LocalDateTime.of(2026, 11, 5, 8, 0)).lembretes();
        assertEquals(Collections.singletonList(1L), destinatarios(1));
    }

    @Test
    void falhaNoAvisoNaoQuebraAAcao() {
        when(repository.demanda(5L)).thenThrow(new RuntimeException("banco fora"));
        servico(LocalDateTime.of(2026, 11, 5, 9, 0)).demandaCriada(5L, nick);
        verify(enviador, never()).enviar(any(), anyString());
    }

    @Test
    void envolvidosIgnoraLixo() {
        assertEquals(Arrays.asList(1L, 2L), new java.util.ArrayList<>(NotificacaoServiceImpl.envolvidos(demanda(null, "1, 2,,x,1"))));
    }

    @Test
    void repositorioGravaEmTransacaoPropria() {
        // O pool roda sem auto-commit: sem transação, chaves VAPID, aparelhos e lembretes já enviados nunca seriam gravados.
        assertTrue(br.com.nord_tool_backend.repository.impl.NotificacaoRepositoryImpl.class
                .isAnnotationPresent(org.springframework.transaction.annotation.Transactional.class));
    }

    // ---------------------------------------------------------------- tela de Configurações

    private NotificacaoServiceImpl servicoLogado() {
        AutorizacaoService aut = mock(AutorizacaoService.class);
        when(aut.exigirAutenticado()).thenReturn(nick);
        return new NotificacaoServiceImpl(repository, aut, enviador, new ObjectMapper(), acessos, Clock.fixed(java.time.Instant.now(), SP));
    }

    @Test
    void configEntregaAChavePublicaDoServidor() {
        when(enviador.chaves()).thenReturn(new WebPushCripto.Chaves("PUB", "PRIV"));
        br.com.nord_tool_backend.dto.NotificacaoConfigDto c = servicoLogado().config();
        assertTrue(c.isHabilitado());
        assertEquals("PUB", c.getChavePublica());
    }

    @Test
    void inscreveECancelaOAparelhoDeQuemEstaLogado() {
        br.com.nord_tool_backend.form.PushInscricaoForm f = new br.com.nord_tool_backend.form.PushInscricaoForm();
        f.setEndpoint(" https://push.exemplo/x ");
        f.setP256dh("chave");
        f.setAuth("segredo");
        f.setAparelho("  ");
        NotificacaoServiceImpl s = servicoLogado();
        s.inscrever(f);
        verify(repository).inscrever(1L, "https://push.exemplo/x", "chave", "segredo", null);
        f.setAparelho(" Android · Chrome ");
        s.inscrever(f);
        verify(repository).inscrever(1L, "https://push.exemplo/x", "chave", "segredo", "Android · Chrome");
        s.cancelar(" https://push.exemplo/x ");
        verify(repository).cancelar(1L, "https://push.exemplo/x");
        assertEquals(1, s.meusAparelhos().size());
    }

    @Test
    void preferenciasPadraoESalvas() {
        when(repository.preferencia(1L)).thenReturn(Optional.empty());
        NotificacaoServiceImpl s = servicoLogado();
        NotificacaoPreferenciaDto padrao = s.preferencias();
        assertEquals(1L, padrao.getIdUsuario());
        assertTrue(padrao.isInPrazo() && padrao.isInMinhasAcoes());
        assertEquals(30, padrao.getNrAntecedenciaMin());

        br.com.nord_tool_backend.form.NotificacaoPreferenciaForm f = new br.com.nord_tool_backend.form.NotificacaoPreferenciaForm();
        f.setInDemandaNova(true);
        f.setNrAntecedenciaMin(60);
        NotificacaoPreferenciaDto salva = s.salvarPreferencias(f);
        verify(repository).salvarPreferencia(salva);
        assertEquals(60, salva.getNrAntecedenciaMin());
        assertTrue(salva.isInDemandaNova() && !salva.isInPrazo() && !salva.isInSac());
    }

    @Test
    void testeVaiParaOsAparelhosDeQuemPediu() {
        ArgumentCaptor<String> conteudo = ArgumentCaptor.forClass(String.class);
        assertEquals(1, servicoLogado().testar());
        verify(enviador).enviar(any(), conteudo.capture());
        assertTrue(conteudo.getValue().contains("\"tag\":\"teste\""));
    }

    @Test
    void respostaNoSacAvisaQuemAbriu() {
        when(repository.preferencias(any())).thenAnswer(i -> ((Collection<Long>) i.getArgument(0)).stream().map(id -> {
            NotificacaoPreferenciaDto p = new NotificacaoPreferenciaDto();
            p.setIdUsuario(id);
            return p;
        }).collect(Collectors.toList()));
        ArgumentCaptor<String> conteudo = ArgumentCaptor.forClass(String.class);
        servicoLogado().sacRespondido(7L, 3L, "Filtro por obra", "RESOLVIDO", nick);
        verify(enviador).enviar(any(), conteudo.capture());
        assertTrue(conteudo.getValue().contains("/sac?chamado=3") && conteudo.getValue().contains("Filtro por obra"));
    }

    @Test
    void falhaAoAvisarNaoChegaEmQuemChamou() {
        when(repository.demanda(anyLong())).thenThrow(new IllegalStateException("banco fora"));
        NotificacaoServiceImpl s = servicoLogado();
        s.demandaCriada(1L, nick);
        s.demandaAlterada(1L, nick, "etapa:executar");
        s.demandaCompartilhada(1L, nick, List.of(2L));
        verify(enviador, never()).enviar(any(), anyString());
    }

    // ---------------------------------------------------------------- criptografia do Web Push (WebPushCripto)


    // Vetores do Apêndice A da RFC 8291.
    private static final String CONTEUDO = "When I grow up, I want to be a watermelon";
    private static final String AS_PRIVADA = "yfWPiYE-n46HLnH0KqZOF1fJJU3MYrct3AELtAQ-oRw";
    private static final String AS_PUBLICA = "BP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27mlmlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A8";
    private static final String UA_PRIVADA = "q1dXpw3UpT5VOmu_cf_v6ih07Aems3njxI-JWgLcM94";
    private static final String UA_PUBLICA = "BCVxsr7N_eNgVRqvHtD0zTZsEc6-VV-JvLexhqUzORcxaOzi6-AYWXvTBHm4bjyPjs7Vd8pZGH6SRpkNtoIAiw4";
    private static final String SALT = "DGv6ra1nlYgDCS1FRnbzlw";
    private static final String SEGREDO = "BTBZMqHH6r4Tts7J_aSIgg";

    private static byte[] hmac(byte[] k, byte[]... partes) throws Exception {
        Mac m = Mac.getInstance("HmacSHA256");
        m.init(new SecretKeySpec(k, "HmacSHA256"));
        for (byte[] p : partes) m.update(p);
        return m.doFinal();
    }

    /** O lado do navegador (decifra), escrito à parte para conferir o envio de ponta a ponta. */
    private static byte[] decifrar(byte[] corpo, KeyPair ua, byte[] segredo) throws Exception {
        byte[] salt = Arrays.copyOfRange(corpo, 0, 16);
        int idlen = corpo[20] & 0xff;
        byte[] asPublica = Arrays.copyOfRange(corpo, 21, 21 + idlen);
        byte[] cifrado = Arrays.copyOfRange(corpo, 21 + idlen, corpo.length);
        KeyAgreement ka = KeyAgreement.getInstance("ECDH");
        ka.init(ua.getPrivate());
        ka.doPhase(WebPushCripto.publicaDosBytes(asPublica), true);
        byte[] uaPublica = WebPushCripto.bytesDaPublica((ECPublicKey) ua.getPublic());
        byte[] ikm = hmac(hmac(segredo, ka.generateSecret()), "WebPush: info\0".getBytes(StandardCharsets.US_ASCII), uaPublica, asPublica, new byte[]{1});
        byte[] prk = hmac(salt, ikm);
        byte[] cek = Arrays.copyOf(hmac(prk, "Content-Encoding: aes128gcm\0".getBytes(StandardCharsets.US_ASCII), new byte[]{1}), 16);
        byte[] nonce = Arrays.copyOf(hmac(prk, "Content-Encoding: nonce\0".getBytes(StandardCharsets.US_ASCII), new byte[]{1}), 12);
        Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
        aes.init(Cipher.DECRYPT_MODE, new SecretKeySpec(cek, "AES"), new GCMParameterSpec(128, nonce));
        byte[] claro = aes.doFinal(cifrado);
        assertEquals(2, claro[claro.length - 1], "delimitador do último registro");
        return Arrays.copyOf(claro, claro.length - 1);
    }

    @Test
    void webPushCifraComOsVetoresDaRfcEONavegadorDecifra() throws Exception {
        KeyPair servidor = WebPushCripto.par(WebPushCripto.deB64(AS_PRIVADA), WebPushCripto.deB64(AS_PUBLICA));
        KeyPair navegador = WebPushCripto.par(WebPushCripto.deB64(UA_PRIVADA), WebPushCripto.deB64(UA_PUBLICA));
        byte[] corpo = WebPushCripto.cifrar(CONTEUDO.getBytes(StandardCharsets.UTF_8), WebPushCripto.deB64(UA_PUBLICA),
                WebPushCripto.deB64(SEGREDO), servidor, WebPushCripto.deB64(SALT));
        // cabeçalho: salt, tamanho do registro 4096, chave do servidor
        assertArrayEquals(WebPushCripto.deB64(SALT), Arrays.copyOf(corpo, 16));
        assertEquals(4096, ((corpo[16] & 0xff) << 24) | ((corpo[17] & 0xff) << 16) | ((corpo[18] & 0xff) << 8) | (corpo[19] & 0xff));
        assertEquals(65, corpo[20]);
        assertArrayEquals(WebPushCripto.deB64(AS_PUBLICA), Arrays.copyOfRange(corpo, 21, 86));
        assertEquals(CONTEUDO, new String(decifrar(corpo, navegador, WebPushCripto.deB64(SEGREDO)), StandardCharsets.UTF_8));
        assertEquals("8pfeW0KbunFT06SuDKoJH9Ql87S1QUrdirN6GcG7sFz1y1sqLgVi1VhjVkHsUoEsbI_0LpXMuGvnzQ",
                WebPushCripto.b64(Arrays.copyOfRange(corpo, 86, corpo.length)), "texto cifrado do Apêndice A da RFC 8291");
    }

    @Test
    void webPushUsaParESaltNovosACadaEnvio() throws Exception {
        KeyPair navegador = WebPushCripto.par(WebPushCripto.deB64(UA_PRIVADA), WebPushCripto.deB64(UA_PUBLICA));
        byte[] a = WebPushCripto.cifrar("oi".getBytes(StandardCharsets.UTF_8), UA_PUBLICA, SEGREDO);
        byte[] b = WebPushCripto.cifrar("oi".getBytes(StandardCharsets.UTF_8), UA_PUBLICA, SEGREDO);
        assertTrue(!Arrays.equals(a, b));
        assertEquals("oi", new String(decifrar(a, navegador, WebPushCripto.deB64(SEGREDO)), StandardCharsets.UTF_8));
    }

    @Test
    void webPushVapidAssinaOJwtQueAChavePublicaConfere() throws Exception {
        WebPushCripto.Chaves c = WebPushCripto.gerarChaves();
        assertTrue(WebPushCripto.parValido(c));
        String auth = WebPushCripto.autorizacao("https://fcm.googleapis.com/fcm/send/abc", c, "mailto:obra@exemplo.com", 1_900_000_000L);
        assertTrue(auth.startsWith("vapid t=") && auth.endsWith(", k=" + c.publica));
        String jwt = auth.substring(8, auth.indexOf(", k="));
        String[] partes = jwt.split("\\.");
        String corpo = new String(WebPushCripto.deB64(partes[1]), StandardCharsets.UTF_8);
        assertTrue(corpo.contains("\"aud\":\"https://fcm.googleapis.com\"") && corpo.contains("\"exp\":1900000000"));
        // assinatura bruta r||s (64 bytes) volta para DER e confere com a pública
        byte[] bruto = WebPushCripto.deB64(partes[2]);
        assertEquals(64, bruto.length);
        Signature s = Signature.getInstance("SHA256withECDSA");
        s.initVerify(WebPushCripto.publicaDosBytes(WebPushCripto.deB64(c.publica)));
        s.update((partes[0] + "." + partes[1]).getBytes(StandardCharsets.US_ASCII));
        assertTrue(s.verify(paraDer(bruto)));
    }

    private static byte[] paraDer(byte[] rs) {
        byte[] r = inteiro(Arrays.copyOf(rs, 32)), s = inteiro(Arrays.copyOfRange(rs, 32, 64));
        byte[] der = new byte[6 + r.length + s.length];
        der[0] = 0x30; der[1] = (byte) (4 + r.length + s.length); der[2] = 2; der[3] = (byte) r.length;
        System.arraycopy(r, 0, der, 4, r.length);
        der[4 + r.length] = 2; der[5 + r.length] = (byte) s.length;
        System.arraycopy(s, 0, der, 6 + r.length, s.length);
        return der;
    }

    private static byte[] inteiro(byte[] b) {
        return new java.math.BigInteger(1, b).toByteArray();
    }

    @Test
    void webPushOrigemDoEndpoint() {
        assertEquals("https://updates.push.services.mozilla.com", WebPushCripto.origem("https://updates.push.services.mozilla.com/wpush/v2/gAAA"));
        assertEquals("https://web.push.apple.com", WebPushCripto.origem("https://web.push.apple.com/QGuQ"));
    }

    // ---------------------------------------------------------------- caixa (app Android) e alterações

    @Test
    void caixaSemPontoDePartidaSoDizDeOndeComecar() {
        when(repository.ultimoDaCaixa(1L)).thenReturn(42L);
        br.com.nord_tool_backend.dto.NotificacaoCaixaDto c = servicoLogado().caixa(null);
        assertEquals(42L, c.getNrUltimo());
        assertTrue(c.getItens().isEmpty());
        assertTrue(servicoLogado().caixa(-1L).getItens().isEmpty());
        // caixa recriada (o app tinha um número maior): recomeça do fim
        assertTrue(servicoLogado().caixa(99L).getItens().isEmpty());
        verify(repository, never()).caixa(anyLong(), anyLong(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void caixaDevolveOsAvisosDepoisDoNumero() {
        br.com.nord_tool_backend.dto.NotificacaoCaixaItemDto item = new br.com.nord_tool_backend.dto.NotificacaoCaixaItemDto();
        item.setNrSeq(41L);
        when(repository.ultimoDaCaixa(1L)).thenReturn(41L);
        when(repository.caixa(1L, 40L, NotificacaoServiceImpl.LIMITE_CAIXA)).thenReturn(List.of(item));
        br.com.nord_tool_backend.dto.NotificacaoCaixaDto c = servicoLogado().caixa(40L);
        assertEquals(1, c.getItens().size());
        assertEquals(41L, c.getNrUltimo());
    }

    @Test
    void avisosDoCronogramaVaoTambemParaACaixa() {
        DemandaAvisoDto d = new DemandaAvisoDto();
        d.setId(5L);
        d.setNmCronogramaSemanal("Concretagem");
        d.setTxEnvolvidos("1,2");
        when(repository.demanda(5L)).thenReturn(Optional.of(d));
        when(repository.preferencias(any())).thenReturn(List.of(pref(2)));
        servico(LocalDateTime.of(2026, 11, 18, 10, 0)).demandaCriada(5L, nick);
        verify(repository).inserirCaixa(eq(2L), eq("demanda"), eq(null), eq("Nova demanda"), org.mockito.ArgumentMatchers.contains("Concretagem"), eq(NotificacaoServiceImpl.TELA_CRONOGRAMA));
    }

    @Test
    void testeTambemChegaNaCaixa() {
        servicoLogado().testar();
        verify(repository).inserirCaixa(eq(1L), eq("teste"), eq(null), eq("NordTool"), anyString(), eq(NotificacaoServiceImpl.TELA_NOTIFICACOES));
    }

    @Test
    void areaDoCaminhoIgnoraOQueNaoEAlteracaoDeDados() {
        assertEquals(Optional.of("Bloco de notas"), NotificacaoServiceImpl.areaDoCaminho("/api/v1/nord-tool/blocos-notas/3/documento"));
        assertEquals(Optional.of("Termo de reprova"), NotificacaoServiceImpl.areaDoCaminho("/api/v1/nord-tool/apartamentoVistoria/termos-reprova/1/situacao"));
        assertEquals(Optional.of("Vistorias"), NotificacaoServiceImpl.areaDoCaminho("/api/v1/nord-tool/apartamentoVistoria/importar"));
        assertEquals(Optional.of("Equipes do cronograma"), NotificacaoServiceImpl.areaDoCaminho("/api/v1/nord-tool/cronogramaSemanal/equipes/2"));
        assertEquals(Optional.of("Cronograma"), NotificacaoServiceImpl.areaDoCaminho("/api/v1/nord-tool/cronogramaSemanal"));
        assertEquals(Optional.of("Acessos e usuários"), NotificacaoServiceImpl.areaDoCaminho("/api/v1/nord-tool/administracao/acessos/4"));
        assertEquals(Optional.of("novidade"), NotificacaoServiceImpl.areaDoCaminho("/api/v1/nord-tool/novidade/1"));
        for (String ignorado : new String[]{"/api/v1/nord-tool/auth/login", "/api/v1/nord-tool/notificacoes/aparelhos", "/api/v1/nord-tool/lugia/conversas/1",
                "/api/v1/nord-tool/cronogramaSemanal/interpretar", "/api/v1/nord-tool/sac/chamados/vistos", "/api/v1/nord-tool/menu-favoritos",
                "/api/v1/nord-tool/", "/outra/coisa", null}) {
            assertEquals(Optional.empty(), NotificacaoServiceImpl.areaDoCaminho(ignorado), String.valueOf(ignorado));
        }
        assertEquals("Criou", NotificacaoServiceImpl.verbo("post"));
        assertEquals("Alterou", NotificacaoServiceImpl.verbo("PUT"));
        assertEquals("Alterou", NotificacaoServiceImpl.verbo("PATCH"));
        assertEquals("Excluiu", NotificacaoServiceImpl.verbo("DELETE"));
        assertEquals(null, NotificacaoServiceImpl.verbo("GET"));
        assertEquals(null, NotificacaoServiceImpl.verbo(null));
    }

    private final UsuarioAutenticado ana = new UsuarioAutenticado(2L, "a@obra.com", "USER", Collections.emptyMap(), 0, java.time.Instant.now());

    @Test
    void alteracaoAvisaOsAdministradoresMenosQuemFezEQuemDesligou() {
        when(acessos.administradoresAtivos()).thenReturn(new java.util.LinkedHashSet<>(List.of(1L, 2L, 3L)));
        NotificacaoPreferenciaDto desligou = pref(3);
        desligou.setInAtividade(false);
        when(repository.preferencias(any())).thenAnswer(i -> ((Collection<Long>) i.getArgument(0)).stream()
                .map(id -> id == 3L ? desligou : pref(id)).collect(Collectors.toList()));
        when(repository.nomeDoUsuario(2L)).thenReturn("Ana Souza");
        when(repository.agruparCaixa(anyLong(), anyString(), any(), anyString(), anyString(), any())).thenReturn(Optional.empty());

        servico(LocalDateTime.of(2026, 11, 18, 10, 0)).atividade(ana, "PUT", "/api/v1/nord-tool/blocos-notas/3");

        verify(repository).registrarAtividade(2L, "PUT", "/api/v1/nord-tool/blocos-notas/3", "Bloco de notas");
        ArgumentCaptor<Collection<Long>> pedidos = ArgumentCaptor.forClass(Collection.class);
        verify(repository).preferencias(pedidos.capture());
        assertEquals(List.of(1L, 3L), List.copyOf(pedidos.getValue()), "quem fez não recebe");
        verify(repository).inserirCaixa(1L, "atividade", "atividade:2:Bloco de notas", "Ana Souza · Bloco de notas", "Alterou um registro", null);
        verify(repository, never()).inserirCaixa(eq(3L), anyString(), any(), anyString(), anyString(), any());
        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(enviador).enviar(any(), json.capture());
        assertTrue(json.getValue().contains("Ana Souza · Bloco de notas"));
    }

    @Test
    void alteracoesSeguidasViramUmAvisoSo() {
        when(acessos.administradoresAtivos()).thenReturn(Set.of(1L));
        when(repository.preferencias(any())).thenReturn(List.of(pref(1)));
        when(repository.agruparCaixa(eq(1L), eq("atividade:2:Financeiro"), eq(LocalDateTime.of(2026, 11, 18, 9, 50)), anyString(),
                eq("{n} alterações seguidas · a última: excluiu"), eq(null))).thenReturn(Optional.of(4));

        servico(LocalDateTime.of(2026, 11, 18, 10, 0)).atividade(ana, "DELETE", "/api/v1/nord-tool/financeiro/lancamentos/9");

        verify(repository, never()).inserirCaixa(anyLong(), anyString(), any(), anyString(), anyString(), any());
        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(enviador).enviar(any(), json.capture());
        assertTrue(json.getValue().contains("4 alterações seguidas"), json.getValue());
    }

    @Test
    void alteracaoSemInteresseOuSemAdminNaoAvisa() {
        NotificacaoServiceImpl s = servico(LocalDateTime.of(2026, 11, 18, 10, 0));
        s.atividade(ana, "POST", "/api/v1/nord-tool/auth/refresh");
        s.atividade(ana, "GET", "/api/v1/nord-tool/blocos-notas");
        s.atividade(null, "POST", "/api/v1/nord-tool/blocos-notas");
        s.atividade(ana, null, "/api/v1/nord-tool/blocos-notas");
        s.atividade(ana, "POST", null);
        s.atividade(new UsuarioAutenticado(null, "x", "USER", Collections.emptyMap(), 0, java.time.Instant.now()), "POST", "/api/v1/nord-tool/blocos-notas");
        verify(repository, never()).registrarAtividade(any(), any(), any(), any());

        // só o próprio admin alterou: ninguém para avisar (e a lista de admins fica guardada por 1 min)
        when(acessos.administradoresAtivos()).thenReturn(Set.of(2L));
        s.atividade(ana, "POST", "/api/v1/nord-tool/treinos/sessoes");
        s.atividade(ana, "POST", "/api/v1/nord-tool/treinos/sessoes");
        verify(repository, times(2)).registrarAtividade(2L, "POST", "/api/v1/nord-tool/treinos/sessoes", "Treinos");
        verify(acessos, times(1)).administradoresAtivos();
        verify(enviador, never()).enviar(any(), anyString());

        // admin com o aviso desligado
        when(acessos.administradoresAtivos()).thenReturn(Set.of(1L));
        NotificacaoPreferenciaDto desligou = pref(1);
        desligou.setInAtividade(false);
        when(repository.preferencias(any())).thenReturn(List.of(desligou));
        NotificacaoServiceImpl outro = servico(LocalDateTime.of(2026, 11, 18, 10, 0));
        outro.atividade(ana, "POST", "/api/v1/nord-tool/treinos/sessoes");
        verify(enviador, never()).enviar(any(), anyString());
    }

    @Test
    void falhaAoAvisarAlteracaoNaoSobe() {
        when(acessos.administradoresAtivos()).thenThrow(new IllegalStateException("banco fora"));
        servico(LocalDateTime.of(2026, 11, 18, 10, 0)).atividade(ana, "POST", "/api/v1/nord-tool/treinos/sessoes");
        verify(enviador, never()).enviar(any(), anyString());
    }

    @Test
    void preferenciaDeAlteracoesFicaLigadaQuandoOAppAntigoNaoManda() {
        br.com.nord_tool_backend.form.NotificacaoPreferenciaForm f = new br.com.nord_tool_backend.form.NotificacaoPreferenciaForm();
        NotificacaoServiceImpl s = servicoLogado();
        assertTrue(s.salvarPreferencias(f).isInAtividade());
        f.setInAtividade(false);
        assertTrue(!s.salvarPreferencias(f).isInAtividade());
    }

    @Test
    void naHoraCheiaLimpaCaixaEAlteracoesAntigas() {
        when(repository.demandasAgendadas(any(), any())).thenReturn(List.of());
        when(repository.demandasComPrazo(any())).thenReturn(List.of());
        servico(LocalDateTime.of(2026, 11, 18, 10, 0)).lembretes();
        verify(repository).limparCaixa(LocalDateTime.of(2026, 10, 19, 10, 0));
        verify(repository).limparAtividades(LocalDateTime.of(2026, 8, 20, 10, 0));
    }
}
