package br.com.nord_tool_backend.domain.enums;

import br.com.nord_tool_backend.dto.StatusCronogramaSemanalDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public enum StatusCronogramaSemanalEnum {
    EM_EXECUCAO(1, "Em execução"),
    VERIFICAR(2, "Verificar"),
    PLANEJAMENTO(3, "Planejamento"),
    FINALIZADO(4, "Finalizado");

    private final Integer id;
    private final String nmStatusCronogramaSemanal;

    public StatusCronogramaSemanalDto converterToDto() {
        return new StatusCronogramaSemanalDto(id, nmStatusCronogramaSemanal);
    }

    public static List<StatusCronogramaSemanalDto> listaStatusCronogramaSemanal() {
        return Arrays.stream(values())
                .map(StatusCronogramaSemanalEnum::converterToDto)
                .collect(Collectors.toList());
    }

}
