package br.com.nord_tool_backend.service.push;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.util.Arrays;
import java.util.Base64;

/**
 * Web Push sem biblioteca externa: criptografia do conteúdo (RFC 8291, "aes128gcm" da RFC 8188) e a assinatura VAPID
 * (RFC 8292, JWT ES256) com o que o próprio Java traz (curva P-256, ECDH, HMAC-SHA256, AES-GCM).
 */
public final class WebPushCripto {

    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder D64 = Base64.getUrlDecoder();
    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final int TAMANHO_REGISTRO = 4096;

    private WebPushCripto() {
    }

    /** Par de chaves VAPID em base64url: pública (65 bytes, ponto não comprimido) e privada (32 bytes). */
    public static final class Chaves {
        public final String publica;
        public final String privada;

        public Chaves(String publica, String privada) {
            this.publica = publica;
            this.privada = privada;
        }
    }

    public static String b64(byte[] bytes) {
        return B64.encodeToString(bytes);
    }

    public static byte[] deB64(String texto) {
        return D64.decode(texto.trim().replace('+', '-').replace('/', '_').replace("=", ""));
    }

    // ---------------------------------------------------------------- chaves P-256

    private static ECParameterSpec curva() throws GeneralSecurityException {
        AlgorithmParameters p = AlgorithmParameters.getInstance("EC");
        p.init(new ECGenParameterSpec("secp256r1"));
        return p.getParameterSpec(ECParameterSpec.class);
    }

    private static KeyPair novoPar() throws GeneralSecurityException {
        KeyPairGenerator g = KeyPairGenerator.getInstance("EC");
        g.initialize(new ECGenParameterSpec("secp256r1"), ALEATORIO);
        return g.generateKeyPair();
    }

    /** Ponto não comprimido: 0x04 || X (32) || Y (32). */
    static byte[] bytesDaPublica(ECPublicKey k) {
        byte[] saida = new byte[65];
        saida[0] = 4;
        copiar32(k.getW().getAffineX(), saida, 1);
        copiar32(k.getW().getAffineY(), saida, 33);
        return saida;
    }

    private static void copiar32(BigInteger n, byte[] destino, int pos) {
        byte[] b = n.toByteArray();
        int inicio = Math.max(0, b.length - 32);
        int tam = b.length - inicio;
        System.arraycopy(b, inicio, destino, pos + 32 - tam, tam);
    }

    static ECPublicKey publicaDosBytes(byte[] ponto) throws GeneralSecurityException {
        if (ponto.length != 65 || ponto[0] != 4) throw new GeneralSecurityException("Chave pública P-256 inválida");
        BigInteger x = new BigInteger(1, Arrays.copyOfRange(ponto, 1, 33));
        BigInteger y = new BigInteger(1, Arrays.copyOfRange(ponto, 33, 65));
        return (ECPublicKey) KeyFactory.getInstance("EC").generatePublic(new ECPublicKeySpec(new ECPoint(x, y), curva()));
    }

    static ECPrivateKey privadaDosBytes(byte[] d) throws GeneralSecurityException {
        return (ECPrivateKey) KeyFactory.getInstance("EC").generatePrivate(new ECPrivateKeySpec(new BigInteger(1, d), curva()));
    }

    static byte[] bytesDaPrivada(ECPrivateKey k) {
        byte[] saida = new byte[32];
        copiar32(k.getS(), saida, 0);
        return saida;
    }

    /** Gera um par VAPID novo. */
    public static Chaves gerarChaves() {
        try {
            KeyPair par = novoPar();
            return new Chaves(b64(bytesDaPublica((ECPublicKey) par.getPublic())), b64(bytesDaPrivada((ECPrivateKey) par.getPrivate())));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Não foi possível gerar as chaves VAPID", e);
        }
    }

    // ---------------------------------------------------------------- conteúdo (RFC 8291)

    private static byte[] hmac(byte[] chave, byte[]... partes) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(chave, "HmacSHA256"));
        for (byte[] p : partes) mac.update(p);
        return mac.doFinal();
    }

    private static byte[] juntar(byte[]... partes) {
        ByteArrayOutputStream s = new ByteArrayOutputStream();
        for (byte[] p : partes) s.write(p, 0, p.length);
        return s.toByteArray();
    }

    /**
     * Cifra o conteúdo para um aparelho. `uaPublica` (p256dh) e `segredo` (auth) vêm da inscrição do navegador.
     * `parDoServidor` e `salt` são aleatórios a cada envio (só os testes passam valores fixos).
     */
    static byte[] cifrar(byte[] conteudo, byte[] uaPublica, byte[] segredo, KeyPair parDoServidor, byte[] salt) throws GeneralSecurityException {
        ECPublicKey ua = publicaDosBytes(uaPublica);
        byte[] asPublica = bytesDaPublica((ECPublicKey) parDoServidor.getPublic());
        KeyAgreement ka = KeyAgreement.getInstance("ECDH");
        ka.init(parDoServidor.getPrivate());
        ka.doPhase(ua, true);
        byte[] ecdh = ka.generateSecret();

        byte[] prkChave = hmac(segredo, ecdh);
        byte[] infoChave = juntar("WebPush: info\0".getBytes(StandardCharsets.US_ASCII), uaPublica, asPublica);
        byte[] ikm = hmac(prkChave, infoChave, new byte[]{1});
        byte[] prk = hmac(salt, ikm);
        byte[] cek = Arrays.copyOf(hmac(prk, "Content-Encoding: aes128gcm\0".getBytes(StandardCharsets.US_ASCII), new byte[]{1}), 16);
        byte[] nonce = Arrays.copyOf(hmac(prk, "Content-Encoding: nonce\0".getBytes(StandardCharsets.US_ASCII), new byte[]{1}), 12);

        Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
        aes.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(cek, "AES"), new GCMParameterSpec(128, nonce));
        byte[] cifrado = aes.doFinal(juntar(conteudo, new byte[]{2}));

        ByteBuffer cab = ByteBuffer.allocate(16 + 4 + 1 + asPublica.length);
        cab.put(salt).putInt(TAMANHO_REGISTRO).put((byte) asPublica.length).put(asPublica);
        return juntar(cab.array(), cifrado);
    }

    /** Cifra com par e salt novos (o uso normal). */
    public static byte[] cifrar(byte[] conteudo, String p256dh, String auth) {
        try {
            byte[] salt = new byte[16];
            ALEATORIO.nextBytes(salt);
            return cifrar(conteudo, deB64(p256dh), deB64(auth), novoPar(), salt);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Inscrição de push inválida", e);
        }
    }

    // ---------------------------------------------------------------- VAPID (RFC 8292)

    /** Origem do endpoint (o "aud" do JWT): https://fcm.googleapis.com. */
    static String origem(String endpoint) {
        URI u = URI.create(endpoint);
        return u.getScheme() + "://" + u.getHost() + (u.getPort() > 0 ? ":" + u.getPort() : "");
    }

    /** Cabeçalho Authorization: "vapid t=<jwt>, k=<chave pública>". */
    public static String autorizacao(String endpoint, Chaves chaves, String assunto, long expiraEmSegundos) {
        try {
            String cabecalho = b64("{\"typ\":\"JWT\",\"alg\":\"ES256\"}".getBytes(StandardCharsets.UTF_8));
            String corpo = b64(("{\"aud\":\"" + origem(endpoint) + "\",\"exp\":" + expiraEmSegundos + ",\"sub\":\"" + assunto.replace("\"", "") + "\"}")
                    .getBytes(StandardCharsets.UTF_8));
            Signature s = Signature.getInstance("SHA256withECDSA");
            s.initSign(privadaDosBytes(deB64(chaves.privada)));
            s.update((cabecalho + "." + corpo).getBytes(StandardCharsets.US_ASCII));
            String jwt = cabecalho + "." + corpo + "." + b64(derParaBruto(s.sign()));
            return "vapid t=" + jwt + ", k=" + chaves.publica;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Não foi possível assinar o VAPID", e);
        }
    }

    /** Assinatura ECDSA em DER (SEQUENCE { r, s }) para o formato do JWT: r || s com 32 bytes cada. */
    static byte[] derParaBruto(byte[] der) {
        int pos = 2;
        if ((der[1] & 0x80) != 0) pos = 2 + (der[1] & 0x7f);
        int tamR = der[pos + 1];
        byte[] r = Arrays.copyOfRange(der, pos + 2, pos + 2 + tamR);
        pos = pos + 2 + tamR;
        int tamS = der[pos + 1];
        byte[] s = Arrays.copyOfRange(der, pos + 2, pos + 2 + tamS);
        byte[] saida = new byte[64];
        copiar32(new BigInteger(1, r), saida, 0);
        copiar32(new BigInteger(1, s), saida, 32);
        return saida;
    }

    /** Confere se o par salvo é válido (a pública bate com a privada). */
    public static boolean parValido(Chaves c) {
        try {
            ECPrivateKey priv = privadaDosBytes(deB64(c.privada));
            ECPublicKey pub = publicaDosBytes(deB64(c.publica));
            Signature s = Signature.getInstance("SHA256withECDSA");
            s.initSign(priv);
            s.update(new byte[]{1, 2, 3});
            byte[] ass = s.sign();
            s.initVerify(pub);
            s.update(new byte[]{1, 2, 3});
            return s.verify(ass);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            return false;
        }
    }

    /** Monta um par a partir dos bytes (testes com os vetores da RFC). */
    static KeyPair par(byte[] privada, byte[] publica) throws GeneralSecurityException {
        return new KeyPair(publicaDosBytes(publica), privadaDosBytes(privada));
    }
}
