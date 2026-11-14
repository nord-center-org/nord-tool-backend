package br.com.nord_tool_backend.form;

import lombok.Data;

import java.util.List;

/** Repertório v12: compartilhar uma cifra com outras pessoas. Os limites são conferidos no serviço. */
@Data
public class RepertorioCifraEnvioForm {
    private List<Long> idsUsuarios;
    private String nmTitulo;
    private String nmArtista;
    private String nmTom;
    private String txCifra;
    private String txMensagem;
}
