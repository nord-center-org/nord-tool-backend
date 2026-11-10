package br.com.nord_tool_backend.service;

import java.util.List;

public interface MenuFavoritoService {

    List<String> listar();

    List<String> salvar(List<String> caminhos);
}
