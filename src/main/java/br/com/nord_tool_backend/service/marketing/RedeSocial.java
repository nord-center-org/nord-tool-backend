package br.com.nord_tool_backend.service.marketing;

import java.util.Locale;
import java.util.Optional;

/** Redes que o Marketing conecta. O nome é o {@code cd_rede} gravado em {@code marketing_conta}. */
public enum RedeSocial {
    YOUTUBE("YouTube"),
    INSTAGRAM("Instagram"),
    TIKTOK("TikTok");

    private final String nome;

    RedeSocial(String nome) {
        this.nome = nome;
    }

    public String getNome() {
        return nome;
    }

    public static Optional<RedeSocial> de(String valor) {
        if (valor == null) return Optional.empty();
        try {
            return Optional.of(valueOf(valor.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
