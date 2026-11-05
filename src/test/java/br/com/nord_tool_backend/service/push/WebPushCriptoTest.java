package br.com.nord_tool_backend.service.push;

import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebPushCriptoTest {

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
    void cifraComOsVetoresDaRfcEONavegadorDecifra() throws Exception {
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
    void cadaEnvioUsaParESaltNovos() throws Exception {
        KeyPair navegador = WebPushCripto.par(WebPushCripto.deB64(UA_PRIVADA), WebPushCripto.deB64(UA_PUBLICA));
        byte[] a = WebPushCripto.cifrar("oi".getBytes(StandardCharsets.UTF_8), UA_PUBLICA, SEGREDO);
        byte[] b = WebPushCripto.cifrar("oi".getBytes(StandardCharsets.UTF_8), UA_PUBLICA, SEGREDO);
        assertTrue(!Arrays.equals(a, b));
        assertEquals("oi", new String(decifrar(a, navegador, WebPushCripto.deB64(SEGREDO)), StandardCharsets.UTF_8));
    }

    @Test
    void vapidAssinaOJwtQueAChavePublicaConfere() throws Exception {
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
    void origemDoEndpoint() {
        assertEquals("https://updates.push.services.mozilla.com", WebPushCripto.origem("https://updates.push.services.mozilla.com/wpush/v2/gAAA"));
        assertEquals("https://web.push.apple.com", WebPushCripto.origem("https://web.push.apple.com/QGuQ"));
    }
}
