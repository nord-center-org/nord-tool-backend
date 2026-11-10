package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.domain.Usuario;
import br.com.nord_tool_backend.dto.MeusDadosDto;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.exception.NaoEncontradoException;
import br.com.nord_tool_backend.form.MeusDadosForm;
import br.com.nord_tool_backend.repository.AcessoRepository;
import br.com.nord_tool_backend.repository.EmpresaRepository;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.service.MeusDadosService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Meus dados: cada pessoa logada vê e muda o próprio nome, celular e empresa. O cargo continua o que o administrador
 * definiu (ele dá acessos) e o e-mail de login muda pela tela "Alterar e-mail", que pede a senha.
 */
@Service
@RequiredArgsConstructor
public class MeusDadosServiceImpl implements MeusDadosService {

    private final AcessoRepository repository;
    private final EmpresaRepository empresas;
    private final AutorizacaoService autorizacao;

    @Override
    @Transactional(readOnly = true)
    public MeusDadosDto ler() {
        return dto(eu());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MeusDadosDto salvar(MeusDadosForm form) {
        Usuario u = eu();
        Integer idEmpresa = form.getIdEmpresa();
        if (idEmpresa != null && empresas.listarEmpresas().stream().noneMatch(e -> e.getId() != null && e.getId().intValue() == idEmpresa)) {
            throw new EntradaInvalidaException("Empresa não encontrada");
        }
        String celular = form.getNrCelular() == null ? "" : form.getNrCelular().trim();
        // O cargo fica como está: só o administrador muda (ele traz os acessos).
        repository.alterarDados(u.getId(), form.getNmNome().trim(), celular.isEmpty() ? null : celular, idEmpresa, u.getIdCargo());
        return dto(eu());
    }

    private Usuario eu() {
        Long id = autorizacao.exigirAutenticado().getId();
        return repository.listarUsuarios().stream().filter(x -> id.equals(x.getId())).findFirst()
                .orElseThrow(() -> new NaoEncontradoException("Usuário não encontrado"));
    }

    private static MeusDadosDto dto(Usuario u) {
        return new MeusDadosDto(u.getNmNome(), u.getNmEmail(), u.getNrCelular(), u.getIdEmpresa(), u.getNmEmpresa(), u.getNmCargo());
    }
}
