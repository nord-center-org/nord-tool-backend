package br.com.nord_tool_backend.service.lugia;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;

/**
 * Protege o hardware da máquina local (secretária local, Ollama): poucos pedidos gerando ao mesmo tempo, fila curta
 * com tempo-limite de espera e um disjuntor que, depois de falhas seguidas (máquina desligada, túnel parado), responde
 * na hora "fora do ar" por um tempo em vez de deixar todo mundo esperando o tempo-limite. Depois desse tempo, deixa um
 * pedido de teste passar.
 */
public class LugiaProtetor {

    private final Semaphore vagas;
    private final int maxSimultaneos;
    private final int filaMax;
    private final long esperaMaxMs;
    private final int falhasParaAbrir;
    private final long abertoPorMs;
    private final LongSupplier agora;

    private final AtomicInteger aguardando = new AtomicInteger();
    private final AtomicBoolean emTeste = new AtomicBoolean();
    private int falhasSeguidas;
    private long abertoAte;

    public LugiaProtetor(int maxSimultaneos, int filaMax, long esperaMaxMs, int falhasParaAbrir, long abertoPorMs, LongSupplier agora) {
        this.maxSimultaneos = Math.max(1, maxSimultaneos);
        this.vagas = new Semaphore(this.maxSimultaneos, true);
        this.filaMax = Math.max(0, filaMax);
        this.esperaMaxMs = esperaMaxMs;
        this.falhasParaAbrir = Math.max(1, falhasParaAbrir);
        this.abertoPorMs = abertoPorMs;
        this.agora = agora;
    }

    /** fechado (normal), aberto (fora do ar, recusa na hora) ou teste (deixa um pedido passar para ver se voltou). */
    public synchronized String circuito() {
        if (abertoAte == 0) return "fechado";
        return agora.getAsLong() < abertoAte ? "aberto" : "teste";
    }

    public int ocupados() { return maxSimultaneos - vagas.availablePermits(); }

    public int naFila() { return aguardando.get(); }

    /**
     * Espera a vez e devolve quem libera a vaga (chame sempre, com sucesso ou falha). Recusa com
     * {@link LugiaFalhaException} quando o disjuntor está aberto, a fila está cheia ou a espera passou do limite.
     */
    public Runnable reservar() {
        String c = circuito();
        if ("aberto".equals(c)) {
            long resta = Math.max(1, (abertoAteAgora() - agora.getAsLong()) / 1000);
            throw new LugiaFalhaException("A secretária local está fora do ar agora (máquina desligada ou túnel parado). Tente em instantes ou escolha outro modelo.", null, resta);
        }
        boolean teste = "teste".equals(c);
        if (teste && !emTeste.compareAndSet(false, true)) {
            throw new LugiaFalhaException("A secretária local está voltando; tente de novo em instantes.", null, 10);
        }
        try {
            if (vagas.tryAcquire()) return liberador();
            if (aguardando.incrementAndGet() > filaMax) {
                aguardando.decrementAndGet();
                throw new LugiaFalhaException("A secretária local está ocupada com outros pedidos. Tente de novo em instantes.", null, 15);
            }
            try {
                if (vagas.tryAcquire(esperaMaxMs, TimeUnit.MILLISECONDS)) return liberador();
            } finally {
                aguardando.decrementAndGet();
            }
            throw new LugiaFalhaException("A fila da secretária local demorou demais. Tente de novo.", null, 15);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LugiaFalhaException("Resposta cancelada.", e);
        } catch (LugiaFalhaException e) {
            if (teste) emTeste.set(false);
            throw e;
        }
    }

    /** A resposta veio: fecha o disjuntor. */
    public synchronized void sucesso() {
        falhasSeguidas = 0;
        abertoAte = 0;
        emTeste.set(false);
    }

    /** A máquina falhou; depois de N falhas seguidas (ou falhando no teste), abre o disjuntor. */
    public synchronized void falha() {
        falhasSeguidas++;
        emTeste.set(false);
        if (falhasSeguidas >= falhasParaAbrir || abertoAte != 0) abertoAte = agora.getAsLong() + abertoPorMs;
    }

    private synchronized long abertoAteAgora() { return abertoAte; }

    private Runnable liberador() {
        AtomicBoolean livre = new AtomicBoolean();
        return () -> {
            if (livre.compareAndSet(false, true)) vagas.release();
        };
    }
}
