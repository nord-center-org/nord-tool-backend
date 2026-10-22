package br.com.nord_tool_backend.form;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

@Data
public class RelatorioFotograficoForm {
    @NotBlank(message = "Informe o título do relatório")
    @Size(max = 150, message = "O título deve ter no máximo 150 caracteres")
    private String nmTitulo;

    @Size(max = 150, message = "O local deve ter no máximo 150 caracteres")
    private String nmLocal;

    /** yyyy-MM-dd */
    @NotNull(message = "Informe a data do relatório")
    private LocalDate dtRelatorio;

    /** Vazio = o do modelo (ou 2). */
    private Integer nrFotosPagina;

    /** Só na criação: copia os itens do modelo quando a lista de itens vier vazia. */
    private Long idModelo;

    /** Obrigatória na edição. */
    private Integer nrVersao;

    /** Na edição, a lista substitui os itens (ausentes são excluídos com as fotos). Nula = não mexe nos itens. */
    @Valid
    @Size(max = 100, message = "O relatório pode ter no máximo 100 itens")
    private List<RelatorioItemForm> itens;
}
