package br.com.nord_tool_backend.service.marketing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Cofre do Marketing: cifra os tokens das redes (AES-256-GCM) antes de irem ao banco e assina o {@code state} do
 * login das redes (HMAC-SHA256), que amarra a volta do login à pessoa e à rede e vence em poucos minutos.
 * <p>
 * A chave vem de NORD_MARKETING_CHAVE (base64 de 32 bytes). Sem ela, é derivada das chaves JWT do ambiente; trocar
 * essas chaves só obriga a conectar as contas de novo.
 */
@Component
public class MarketingCofre {

    private static final int IV = 12;
    private static final int TAG_BITS = 128;
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder D64 = Base64.getUrlDecoder();

    private static final SecureRandom ALEATORIO = new SecureRandom();
    private final SecureRandom aleatorio = ALEATORIO;
    private final byte[] chaveCifra;
    private final byte[] chaveAssinatura;

    @Autowired
    public MarketingCofre(@Value("${nord-tool.marketing.chave:}") String chave,
                          @Value("${nord-tool.security.jwt-keys:}") String jwtKeys,
                          @Value("${nord-tool.security.jwt-secret:}") String jwtSecret) {
        this(base(chave, jwtKeys, jwtSecret));
    }

    public MarketingCofre(byte[] base) {
        this.chaveCifra = derivar(base, "cifra");
        this.chaveAssinatura = derivar(base, "assinatura");
    }

    private static byte[] base(String chave, String jwtKeys, String jwtSecret) {
        if (chave != null && !chave.isBlank()) {
            byte[] b = Base64.getDecoder().decode(chave.trim());
            if (b.length < 32) throw new IllegalStateException("NORD_MARKETING_CHAVE precisa de 32 bytes (base64)");
            return b;
        }
        String jwt = (jwtKeys == null ? "" : jwtKeys) + "|" + (jwtSecret == null ? "" : jwtSecret);
        if (jwt.length() > 1) return jwt.getBytes(StandardCharsets.UTF_8);
        // Sem nenhuma chave (só no perfil local): vale até reiniciar o servidor.
        byte[] b = new byte[32];
        ALEATORIO.nextBytes(b);
        return b;
    }

    private static byte[] derivar(byte[] base, String uso) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(base, "HmacSHA256"));
            return mac.doFinal(("nordtool-marketing-" + uso).getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Sem HMAC-SHA256 na JVM", e);
        }
    }

    public String cifrar(String texto) {
        if (texto == null) return null;
        try {
            byte[] iv = new byte[IV];
            aleatorio.nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(chaveCifra, "AES"), new GCMParameterSpec(TAG_BITS, iv));
            byte[] cifrado = c.doFinal(texto.getBytes(StandardCharsets.UTF_8));
            return B64.encodeToString(ByteBuffer.allocate(IV + cifrado.length).put(iv).put(cifrado).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao cifrar o token", e);
        }
    }

    /** Nulo se o texto não foi cifrado com esta chave (ex.: a chave mudou). */
    public String decifrar(String cifrado) {
        if (cifrado == null) return null;
        try {
            byte[] tudo = D64.decode(cifrado);
            if (tudo.length <= IV) return null;
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(chaveCifra, "AES"), new GCMParameterSpec(TAG_BITS, tudo, 0, IV));
            return new String(c.doFinal(tudo, IV, tudo.length - IV), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            return null;
        }
    }

    /** {@code state} do login: usuário, rede, validade e um número aleatório, assinados. */
    public String assinarEstado(long idUsuario, RedeSocial rede, long validoAteMs) {
        byte[] nonce = new byte[12];
        aleatorio.nextBytes(nonce);
        String corpo = idUsuario + "." + rede.name() + "." + validoAteMs + "." + B64.encodeToString(nonce);
        return corpo + "." + B64.encodeToString(hmac(corpo));
    }

    /** Confere a assinatura, a pessoa, a rede e a validade do {@code state}. */
    public boolean estadoValido(String estado, long idUsuario, RedeSocial rede, long agoraMs) {
        if (estado == null) return false;
        int ponto = estado.lastIndexOf('.');
        if (ponto <= 0) return false;
        String corpo = estado.substring(0, ponto);
        byte[] assinatura;
        try {
            assinatura = D64.decode(estado.substring(ponto + 1));
        } catch (IllegalArgumentException e) {
            return false;
        }
        if (!MessageDigest.isEqual(hmac(corpo), assinatura)) return false;
        String[] partes = corpo.split("\\.");
        if (partes.length != 4) return false;
        try {
            return Long.parseLong(partes[0]) == idUsuario && rede.name().equals(partes[1]) && Long.parseLong(partes[2]) >= agoraMs;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** Código aleatório para links temporários (ex.: o vídeo que o Instagram busca). */
    public String codigoAleatorio() {
        byte[] b = new byte[32];
        aleatorio.nextBytes(b);
        return B64.encodeToString(b);
    }

    private byte[] hmac(String corpo) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(chaveAssinatura, "HmacSHA256"));
            return mac.doFinal(corpo.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Sem HMAC-SHA256 na JVM", e);
        }
    }
}
