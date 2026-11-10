package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.repository.MenuFavoritoRepository;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.service.MenuFavoritoService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Favoritos do menu lateral (o tijolo ao lado de cada tela). Cada pessoa guarda os seus; só caminhos de tela
 * ("/mapeamentos/bloco-notas"), sem repetir, na ordem em que foram marcados.
 */
@Service
@RequiredArgsConstructor
public class MenuFavoritoServiceImpl implements MenuFavoritoService {

    static final int MAX_FAVORITOS = 40;
    static final Pattern CAMINHO = Pattern.compile("^/[a-z0-9/-]{0,100}$");
    private static final TypeReference<List<String>> LISTA = new TypeReference<>() { };

    private final MenuFavoritoRepository repository;
    private final AutorizacaoService autorizacao;
    private final ObjectMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<String> listar() {
        Long eu = autorizacao.exigirAutenticado().getId();
        return repository.buscar(eu).map(this::ler).orElseGet(Collections::emptyList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<String> salvar(List<String> caminhos) {
        Long eu = autorizacao.exigirAutenticado().getId();
        Set<String> unicos = new LinkedHashSet<>();
        for (String c : caminhos == null ? Collections.<String>emptyList() : caminhos) {
            if (c == null || !CAMINHO.matcher(c).matches()) throw new EntradaInvalidaException("Tela inválida nos favoritos");
            unicos.add(c);
        }
        if (unicos.size() > MAX_FAVORITOS) throw new EntradaInvalidaException("No máximo " + MAX_FAVORITOS + " favoritos");
        List<String> lista = new ArrayList<>(unicos);
        try {
            repository.salvar(eu, mapper.writeValueAsString(lista));
        } catch (JsonProcessingException e) {
            throw new EntradaInvalidaException("Favoritos inválidos", e);
        }
        return lista;
    }

    private List<String> ler(String json) {
        try {
            List<String> l = mapper.readValue(json, LISTA);
            return l == null ? Collections.emptyList() : l;
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }
}
