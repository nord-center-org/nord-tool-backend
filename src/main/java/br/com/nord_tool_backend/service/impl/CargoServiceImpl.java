package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.dto.CargoDto;
import br.com.nord_tool_backend.form.CargoForm;
import br.com.nord_tool_backend.repository.CargoRepository;
import br.com.nord_tool_backend.service.CargoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class CargoServiceImpl implements CargoService {
    private final CargoRepository cargoRepository;


    private final AutorizacaoService autorizacao;

    private final br.com.nord_tool_backend.repository.AcessoRepository acessoRepository;

    /** Cargo de estagiário já nasce sem poder excluir demandas do cronograma (dá para liberar na tela de Acessos). */
    private static final java.util.regex.Pattern ESTAGIARIO = java.util.regex.Pattern.compile("estagi|trainee", java.util.regex.Pattern.CASE_INSENSITIVE);

    public List<CargoDto> listarCargos() {
        autorizacao.exigirAutenticado();
        return cargoRepository.listarCargos().stream()
                .map(CargoDto::converterToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CargoDto salvarCargo(CargoForm cargoForm) {
        autorizacao.exigir(Modulo.CADASTROS, Acao.ESCRITA);
        br.com.nord_tool_backend.domain.Cargo cargo = cargoRepository.salvarCargo(cargoForm.converterToDomain(null));
        if (cargo.getId() != null && cargo.getNmCargo() != null && ESTAGIARIO.matcher(cargo.getNmCargo()).find()) {
            acessoRepository.substituirAcessosCargo(cargo.getId().intValue(), java.util.Collections.singletonList(
                    new br.com.nord_tool_backend.domain.PerfilPermissao(Modulo.CRONOGRAMA_EXCLUIR.name(), Acao.NENHUM.name())));
        }
        return CargoDto.converterToDto(cargo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CargoDto alterarCargo(Long id, CargoForm cargoForm) {
        autorizacao.exigir(Modulo.CADASTROS, Acao.ESCRITA);
        return CargoDto.converterToDto(cargoRepository.alterarCargo(cargoForm.converterToDomain(id)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletarCargo(Long id) {
        autorizacao.exigir(Modulo.CADASTROS, Acao.ESCRITA);
        cargoRepository.deletarCargo(id);
    }
}
