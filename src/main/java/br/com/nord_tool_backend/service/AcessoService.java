package br.com.nord_tool_backend.service;

import br.com.nord_tool_backend.dto.AcessoUsuarioDto;
import br.com.nord_tool_backend.dto.AcessosDto;
import br.com.nord_tool_backend.form.AcessoUsuarioForm;

public interface AcessoService {
    AcessosDto listar();
    AcessoUsuarioDto salvar(Long idUsuario, AcessoUsuarioForm form);
    AcessoUsuarioDto salvarDados(Long idUsuario, br.com.nord_tool_backend.form.DadosUsuarioForm form);
    java.util.List<br.com.nord_tool_backend.dto.ColaboradorLegadoDto> listarColaboradoresLegados();
    /** Vincula o colaborador antigo ao usuário (null desfaz) e completa o histórico de chaves. */
    br.com.nord_tool_backend.dto.ColaboradorLegadoDto vincularColaborador(Long idUserLegado, Long idUsuario);
}
