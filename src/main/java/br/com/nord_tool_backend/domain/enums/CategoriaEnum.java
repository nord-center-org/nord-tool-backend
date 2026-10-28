package br.com.nord_tool_backend.domain.enums;

import br.com.nord_tool_backend.dto.CategoriaDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public enum CategoriaEnum {
    ATLETICA(1, "Atlética"),
    PESSOAL(2, "Pessoal"),
    PROFISSIONAL(3, "Profissional"),
    MUSICAL(4, "Musical"),
    DEVOCIONAL(5, "Devocional"),
    ACADEMICA(6, "Acadêmica");

    private final Integer id;
    private final String nmCategoria;

    public CategoriaDto converterToDto() {
        return new CategoriaDto(id, nmCategoria);
    }

    public static List<CategoriaDto> listarCategoria() {
        return Arrays.stream(values())
                .map(CategoriaEnum::converterToDto)
                .collect(Collectors.toList());
    }
}
