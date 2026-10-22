package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.dto.AvatarDto;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.exception.NaoEncontradoException;
import br.com.nord_tool_backend.form.AvatarForm;
import br.com.nord_tool_backend.repository.AvatarRepository;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.service.AvatarService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Avatar de cada usuário: basta estar logado; cada um só altera o próprio. A configuração é um JSON plano
 * (chave curta → texto curto, número ou booleano), validado aqui para não virar depósito de dados arbitrários.
 */
@Service
public class AvatarServiceImpl implements AvatarService {

    static final int LIMITE_EQUIPE = 40;
    static final int MAX_CHAVES = 30;
    static final int MAX_TEXTO = 40;
    private static final ObjectMapper JSON = new ObjectMapper();

    private final AvatarRepository repository;
    private final AutorizacaoService autorizacao;

    public AvatarServiceImpl(AvatarRepository repository, AutorizacaoService autorizacao) {
        this.repository = repository;
        this.autorizacao = autorizacao;
    }

    @Override
    @Transactional(readOnly = true)
    public AvatarDto meu() {
        Long eu = autorizacao.exigirAutenticado().getId();
        return repository.buscar(eu).orElseThrow(() -> new NaoEncontradoException("Usuário não encontrado."));
    }

    @Override
    @Transactional
    public AvatarDto salvar(AvatarForm form) {
        Long eu = autorizacao.exigirAutenticado().getId();
        String config = normalizar(form.getDsConfig());
        repository.salvar(eu, config);
        return repository.buscar(eu).orElseThrow(() -> new NaoEncontradoException("Usuário não encontrado."));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvatarDto> equipe() {
        return repository.equipe(autorizacao.exigirAutenticado().getId(), LIMITE_EQUIPE);
    }

    /** Aceita só um objeto plano com chaves e valores curtos; devolve o JSON compacto. */
    static String normalizar(String bruto) {
        JsonNode raiz;
        try {
            raiz = JSON.readTree(bruto == null ? "" : bruto);
        } catch (JsonProcessingException e) {
            throw new EntradaInvalidaException("A configuração do avatar não é um JSON válido.");
        }
        if (raiz == null || !raiz.isObject()) throw new EntradaInvalidaException("A configuração do avatar deve ser um objeto.");
        if (raiz.size() > MAX_CHAVES) throw new EntradaInvalidaException("A configuração do avatar tem itens demais.");
        for (Iterator<Map.Entry<String, JsonNode>> it = raiz.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> campo = it.next();
            JsonNode valor = campo.getValue();
            boolean simples = valor.isBoolean() || valor.isNumber() || (valor.isTextual() && valor.asText().length() <= MAX_TEXTO);
            if (!campo.getKey().matches("^[a-zA-Z][a-zA-Z0-9]{0,29}$") || !simples) {
                throw new EntradaInvalidaException("Item inválido na configuração do avatar: " + campo.getKey());
            }
        }
        return raiz.toString();
    }
}
