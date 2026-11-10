package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.dto.CubicagemDto;
import br.com.nord_tool_backend.exception.ConflitoException;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.exception.NaoEncontradoException;
import br.com.nord_tool_backend.exception.NegocioException;
import br.com.nord_tool_backend.form.CubicagemForm;
import br.com.nord_tool_backend.repository.CubicagemRepository;
import br.com.nord_tool_backend.security.UsuarioAutenticado;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.service.CubicagemService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Cubicagem: cada pessoa só vê e altera as próprias (qualquer usuário logado). As peças são o JSON da tela, que faz
 * as contas; o servidor confere que é uma lista de objetos, o tamanho e a versão.
 */
@Service
@RequiredArgsConstructor
public class CubicagemServiceImpl implements CubicagemService {

    static final int MAX_CUBICAGENS = 300;
    static final int MAX_PECAS = 2000;
    static final int MAX_JSON = 1_000_000;
    static final String VAZIA = "[]";

    private final CubicagemRepository repository;
    private final AutorizacaoService autorizacao;
    private final ObjectMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<CubicagemDto> listar() {
        return repository.listar(eu().getId());
    }

    @Override
    @Transactional(readOnly = true)
    public CubicagemDto abrir(Long id) {
        return comPecas(buscar(id, eu()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CubicagemDto criar(CubicagemForm form) {
        UsuarioAutenticado u = eu();
        if (repository.contar(u.getId()) >= MAX_CUBICAGENS) {
            throw new NegocioException("Você chegou ao limite de " + MAX_CUBICAGENS + " cubicagens. Exclua alguma para criar outra.");
        }
        Long id = repository.inserir(u.getId(), form.getNmTitulo().trim(), perda(form.getNrPerda()), pecas(form.getPecas()));
        return comPecas(buscar(id, u));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CubicagemDto salvar(Long id, CubicagemForm form) {
        UsuarioAutenticado u = eu();
        if (form.getNrVersao() == null) throw new EntradaInvalidaException("Informe a versão (nrVersao)");
        buscar(id, u);
        String pecas = pecas(form.getPecas());
        if (repository.alterar(id, u.getId(), form.getNmTitulo().trim(), perda(form.getNrPerda()), pecas, form.getNrVersao()) == 0) {
            throw new ConflitoException("A cubicagem mudou em outro aparelho. Recarregue para ver a versão mais nova.");
        }
        return comPecas(buscar(id, u));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void excluir(Long id) {
        UsuarioAutenticado u = eu();
        buscar(id, u);
        repository.excluir(id, u.getId());
    }

    private UsuarioAutenticado eu() {
        return autorizacao.exigirAutenticado();
    }

    private CubicagemDto buscar(Long id, UsuarioAutenticado u) {
        return repository.buscar(id, u.getId()).orElseThrow(() -> new NaoEncontradoException("Cubicagem não encontrada"));
    }

    private static BigDecimal perda(BigDecimal p) {
        return p == null ? BigDecimal.ZERO : p;
    }

    /** Lista de objetos, até {@link #MAX_PECAS} peças e {@link #MAX_JSON} bytes. Nulo vira lista vazia. */
    String pecas(JsonNode pecas) {
        if (pecas == null || pecas.isNull()) return VAZIA;
        if (!pecas.isArray()) throw new EntradaInvalidaException("As peças devem vir numa lista");
        if (pecas.size() > MAX_PECAS) throw new EntradaInvalidaException("No máximo " + MAX_PECAS + " peças por cubicagem");
        for (JsonNode p : pecas) {
            if (!p.isObject()) throw new EntradaInvalidaException("Peça inválida");
        }
        String texto = pecas.toString();
        if (texto.getBytes(StandardCharsets.UTF_8).length > MAX_JSON) throw new EntradaInvalidaException("A cubicagem passou do tamanho máximo (1 MB)");
        return texto;
    }

    private CubicagemDto comPecas(CubicagemDto c) {
        try {
            c.setPecas(mapper.readTree(c.getTxPecas() == null ? VAZIA : c.getTxPecas()));
        } catch (JsonProcessingException e) {
            c.setPecas(mapper.createArrayNode());
        }
        c.setTxPecas(null);
        return c;
    }
}
