package br.com.nord_tool_backend.repository;

import br.com.nord_tool_backend.domain.Perfil;
import br.com.nord_tool_backend.domain.PerfilPermissao;
import br.com.nord_tool_backend.domain.Usuario;

import java.util.List;
import java.util.Map;

/** Leitura e gravação da administração de acessos (usuários, perfis e exceções por usuário). */
public interface AcessoRepository {
    List<Usuario> listarUsuarios();
    List<Perfil> listarPerfis();
    /** Permissões de todos os perfis, por id do perfil. */
    Map<Long, List<PerfilPermissao>> permissoesPorPerfil();
    /** Exceções de todos os usuários, por id do usuário. */
    Map<Long, List<PerfilPermissao>> excecoesPorUsuario();
    /** Troca perfil e situação e incrementa a versão da sessão (o usuário entra de novo com os acessos novos). */
    void alterarPerfilESituacao(Long idUsuario, Long idPerfil, boolean ativo);
    void substituirExcecoes(Long idUsuario, List<PerfilPermissao> excecoes);
    void alterarDados(Long idUsuario, String nome, String celular, Integer idEmpresa, Integer idCargo);
    List<br.com.nord_tool_backend.dto.ColaboradorLegadoDto> listarColaboradoresLegados();
    /** Tira o vínculo do colaborador antigo de quem o tiver. */
    void desvincularColaborador(Long idUserLegado);
    /** Vincula, completa celular/empresa/cargo vazios do usuário e preenche o usuário nas retiradas antigas. */
    void vincularColaborador(Long idUserLegado, Long idUsuario);
}
