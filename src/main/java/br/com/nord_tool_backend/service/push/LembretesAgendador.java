package br.com.nord_tool_backend.service.push;

import br.com.nord_tool_backend.service.NotificacaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Lembretes do cronograma (horário chegando e prazo do dia), a cada minuto. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "nord-tool.push.lembretes", havingValue = "true", matchIfMissing = true)
public class LembretesAgendador {

    private final NotificacaoService notificacoes;

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void rodar() {
        notificacoes.lembretes();
    }
}
