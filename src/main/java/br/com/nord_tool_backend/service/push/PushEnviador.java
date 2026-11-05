package br.com.nord_tool_backend.service.push;

import br.com.nord_tool_backend.dto.PushInscricaoDto;
import br.com.nord_tool_backend.repository.NotificacaoRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.PreDestroy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Entrega as notificações nos serviços de push dos navegadores (Google, Apple, Mozilla) fora da requisição: depois
 * do commit, numa fila com duas linhas. Aparelho que o serviço diz que sumiu (404/410) é apagado.
 * As chaves VAPID vêm do ambiente (NORD_PUSH_VAPID_PUBLIC / _PRIVATE) ou são geradas uma vez e guardadas no banco.
 */
@Log4j2
@Component
public class PushEnviador {

    private final NotificacaoRepository repository;
    private final String assunto;
    private final String publicaAmbiente;
    private final String privadaAmbiente;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ExecutorService fila = new ThreadPoolExecutor(1, 2, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(2000), r -> {
        Thread t = new Thread(r, "nord-push");
        t.setDaemon(true);
        return t;
    }, new ThreadPoolExecutor.DiscardOldestPolicy());
    private volatile WebPushCripto.Chaves chaves;

    public PushEnviador(NotificacaoRepository repository,
                        @Value("${nord-tool.push.subject:}") String assunto,
                        @Value("${nord-tool.push.vapid-public:}") String publica,
                        @Value("${nord-tool.push.vapid-private:}") String privada) {
        this.repository = repository;
        this.assunto = assunto == null || assunto.isBlank() ? "mailto:nordtool@localhost" : assunto.trim();
        this.publicaAmbiente = publica == null ? "" : publica.trim();
        this.privadaAmbiente = privada == null ? "" : privada.trim();
    }

    /** O par VAPID em uso: o do ambiente ou o guardado no banco (gerado na primeira vez). */
    public WebPushCripto.Chaves chaves() {
        WebPushCripto.Chaves atual = chaves;
        if (atual != null) return atual;
        synchronized (this) {
            if (chaves != null) return chaves;
            if (!publicaAmbiente.isEmpty() && !privadaAmbiente.isEmpty()) {
                WebPushCripto.Chaves c = new WebPushCripto.Chaves(publicaAmbiente, privadaAmbiente);
                if (!WebPushCripto.parValido(c)) throw new IllegalStateException("NORD_PUSH_VAPID_PUBLIC/PRIVATE não formam um par válido");
                chaves = c;
                return c;
            }
            String[] salvas = repository.chaves().orElse(null);
            if (salvas == null) {
                WebPushCripto.Chaves nova = WebPushCripto.gerarChaves();
                repository.salvarChaves(nova.publica, nova.privada);
                salvas = repository.chaves().orElse(new String[]{nova.publica, nova.privada});
                log.info("Notificações: par VAPID gerado e guardado no banco.");
            }
            chaves = new WebPushCripto.Chaves(salvas[0], salvas[1]);
            return chaves;
        }
    }

    /**
     * Roda a tarefa na fila de envio depois do commit da transação atual (ou já, fora de transação). Assim o aviso lê a
     * demanda já gravada, numa transação própria, e uma falha dele nunca derruba a gravação de quem chamou.
     */
    public void emSegundoPlano(Runnable tarefa) {
        Runnable agendar = () -> fila.execute(tarefa);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    agendar.run();
                }
            });
        } else {
            agendar.run();
        }
    }

    /** Agenda o envio para depois do commit (ou já, fora de transação). Nunca lança: aviso não pode quebrar a tela. */
    public void enviar(List<PushInscricaoDto> aparelhos, String conteudoJson) {
        if (aparelhos.isEmpty()) return;
        Runnable tarefa = () -> aparelhos.forEach(a -> fila.execute(() -> entregar(a, conteudoJson)));
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    tarefa.run();
                }
            });
        } else {
            tarefa.run();
        }
    }

    void entregar(PushInscricaoDto a, String conteudoJson) {
        try {
            WebPushCripto.Chaves c = chaves();
            byte[] corpo = WebPushCripto.cifrar(conteudoJson.getBytes(StandardCharsets.UTF_8), a.getTxP256dh(), a.getTxAuth());
            long expira = Instant.now().plusSeconds(12 * 3600).getEpochSecond();
            HttpRequest req = HttpRequest.newBuilder(URI.create(a.getTxEndpoint()))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", WebPushCripto.autorizacao(a.getTxEndpoint(), c, assunto, expira))
                    .header("Content-Encoding", "aes128gcm")
                    .header("Content-Type", "application/octet-stream")
                    .header("TTL", "86400")
                    .header("Urgency", "high")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(corpo))
                    .build();
            int status = http.send(req, HttpResponse.BodyHandlers.discarding()).statusCode();
            if (status == 404 || status == 410) {
                repository.removerEndpoint(a.getTxEndpoint());
                log.info("Notificações: aparelho {} saiu (HTTP {}), inscrição removida", a.getId(), status);
            } else if (status >= 200 && status < 300) {
                repository.marcarEnvio(a.getId());
            } else {
                log.warn("Notificações: envio para o aparelho {} recusado (HTTP {})", a.getId(), status);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.warn("Notificações: falha ao enviar para o aparelho {}: {}", a.getId(), e.getMessage());
        }
    }

    @PreDestroy
    void parar() {
        fila.shutdown();
    }
}
