package br.com.nord_tool_backend.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** Filtros opcionais da listagem/resumo da Caixinha. {@code situacao}: TODOS | A_PAGAR | PAGO | NAO_LANCADO. */
@Getter @NoArgsConstructor @AllArgsConstructor
public class CaixinhaFiltro {
    private String responsavel;
    private String situacao;
    private LocalDate de;
    private LocalDate ate;
}
