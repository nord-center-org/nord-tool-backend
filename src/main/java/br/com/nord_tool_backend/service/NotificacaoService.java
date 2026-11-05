package br.com.nord_tool_backend.service;

import br.com.nord_tool_backend.dto.NotificacaoConfigDto;
import br.com.nord_tool_backend.dto.NotificacaoPreferenciaDto;
import br.com.nord_tool_backend.dto.PushInscricaoDto;
import br.com.nord_tool_backend.form.NotificacaoPreferenciaForm;
import br.com.nord_tool_backend.form.PushInscricaoForm;
import br.com.nord_tool_backend.security.UsuarioAutenticado;

import java.util.Collection;
import java.util.List;

public interface NotificacaoService {
    NotificacaoConfigDto config();

    void inscrever(PushInscricaoForm form);

    void cancelar(String endpoint);

    List<PushInscricaoDto> meusAparelhos();

    NotificacaoPreferenciaDto preferencias();

    NotificacaoPreferenciaDto salvarPreferencias(NotificacaoPreferenciaForm form);

    /** Manda um aviso de teste para os aparelhos da pessoa; devolve quantos. */
    int testar();

    // eventos (nunca lançam: aviso não pode quebrar a ação)
    void demandaCriada(Long idDemanda, UsuarioAutenticado ator);

    void demandaAlterada(Long idDemanda, UsuarioAutenticado ator, String oQue);

    void demandaCompartilhada(Long idDemanda, UsuarioAutenticado ator, Collection<Long> novos);

    void sacRespondido(Long idDono, Long idChamado, String titulo, String status, UsuarioAutenticado ator);

    /** Lembretes do horário agendado e do prazo do dia (chamado a cada minuto). */
    void lembretes();
}
