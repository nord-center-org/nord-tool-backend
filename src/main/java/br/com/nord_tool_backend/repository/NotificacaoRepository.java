package br.com.nord_tool_backend.repository;

import br.com.nord_tool_backend.dto.DemandaAvisoDto;
import br.com.nord_tool_backend.dto.NotificacaoCaixaItemDto;
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

    // ---- caixa de notificações (app Android)

    void inserirCaixa(Long idUsuario, String tipo, String agrupar, String titulo, String corpo, String url);

    /** Junta ao aviso com o mesmo {@code agrupar} desde {@code desde} ({n} no corpo vira a quantidade); devolve a nova quantidade (vazio se não havia). */
    Optional<Integer> agruparCaixa(Long idUsuario, String agrupar, LocalDateTime desde, String titulo, String corpo, String url);

    List<NotificacaoCaixaItemDto> caixa(Long idUsuario, long depois, int limite);

    long ultimoDaCaixa(Long idUsuario);

    int limparCaixa(LocalDateTime antesDe);

    // ---- alterações feitas no NordTool

    void registrarAtividade(Long idUsuario, String metodo, String caminho, String area);

    int limparAtividades(LocalDateTime antesDe);
}
