package br.com.nord_tool_backend.repository;

import br.com.nord_tool_backend.dto.DemandaAvisoDto;
import br.com.nord_tool_backend.dto.NotificacaoPreferenciaDto;
import br.com.nord_tool_backend.dto.PushInscricaoDto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NotificacaoRepository {
    /** Par VAPID guardado: [pública, privada]. */
    Optional<String[]> chaves();

    /** Guarda o par se ainda não houver (corrida entre instâncias: fica o primeiro). */
    void salvarChaves(String publica, String privada);

    void inscrever(Long idUsuario, String endpoint, String p256dh, String auth, String aparelho);

    int cancelar(Long idUsuario, String endpoint);

    int removerEndpoint(String endpoint);

    List<PushInscricaoDto> inscricoes(Collection<Long> idsUsuarios);

    void marcarEnvio(Long idInscricao);

    Optional<NotificacaoPreferenciaDto> preferencia(Long idUsuario);

    List<NotificacaoPreferenciaDto> preferencias(Collection<Long> idsUsuarios);

    void salvarPreferencia(NotificacaoPreferenciaDto p);

    /** true só na primeira vez que a chave é registrada para a pessoa (o aviso agendado não repete). */
    boolean registrarEnvio(Long idUsuario, String chave);

    int limparEnviadas(LocalDateTime antesDe);

    Optional<DemandaAvisoDto> demanda(Long id);

    List<DemandaAvisoDto> demandasAgendadas(LocalDateTime de, LocalDateTime ate);

    List<DemandaAvisoDto> demandasComPrazo(LocalDate dia);

    String nomeDoUsuario(Long id);
}
