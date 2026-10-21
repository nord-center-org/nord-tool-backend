package br.com.nord_tool_backend.repository;

import br.com.nord_tool_backend.domain.Obra;

import java.time.LocalDateTime;
import java.util.Optional;

/** Cadastro pelo próprio usuário e tokens de redefinição de senha. */
public interface CadastroRepository {
    Optional<Obra> buscarObraAtivaPorCodigo(String cdAcesso);
    boolean emailExiste(String email);
    /** Cria o usuário PENDENTE no perfil USER, sem nenhum acesso. */
    Long inserirPendente(String nome, String email, String hash, String celular, Long idObra);
    void inserirToken(Long idUsuario, String hashToken, LocalDateTime expira);
    Optional<TokenRedefinicao> buscarToken(String hashToken);
    /** Marca como usado; devolve false se outro pedido já usou (corrida). */
    boolean usarToken(Long idToken);
    /** Inutiliza os outros links pendentes do usuário. */
    void invalidarTokens(Long idUsuario);

    final class TokenRedefinicao {
        public final Long id;
        public final Long idUsuario;
        public final LocalDateTime expira;
        public final LocalDateTime uso;

        public TokenRedefinicao(Long id, Long idUsuario, LocalDateTime expira, LocalDateTime uso) {
            this.id = id;
            this.idUsuario = idUsuario;
            this.expira = expira;
            this.uso = uso;
        }
    }
}
