package br.com.nord_tool_backend.service.lugia;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LugiaProtetorTest {

    @Test
    void umPorVezFilaCheiaEEsperaComLimite() {
        LugiaProtetor p = new LugiaProtetor(1, 0, 50, 3, 1000, System::currentTimeMillis);
        Runnable liberar = p.reservar();
        assertThat(p.ocupados()).isEqualTo(1);
        assertThatThrownBy(p::reservar).isInstanceOf(LugiaFalhaException.class).hasMessageContaining("ocupada");
        liberar.run();
        liberar.run(); // liberar duas vezes não abre vaga a mais
        assertThat(p.ocupados()).isZero();

        LugiaProtetor comFila = new LugiaProtetor(1, 2, 50, 3, 1000, System::currentTimeMillis);
        Runnable ocupando = comFila.reservar();
        assertThatThrownBy(comFila::reservar).hasMessageContaining("demorou demais");
        assertThat(comFila.naFila()).isZero();
        ocupando.run();
    }

    @Test
    void disjuntorAbreDeixaUmTestePassarEFecha() {
        AtomicLong agora = new AtomicLong(0);
        LugiaProtetor p = new LugiaProtetor(2, 2, 50, 2, 100, agora::get);
        p.falha();
        assertThat(p.circuito()).isEqualTo("fechado");
        p.falha();
        assertThat(p.circuito()).isEqualTo("aberto");
        assertThatThrownBy(p::reservar).isInstanceOfSatisfying(LugiaFalhaException.class, e -> {
            assertThat(e.getMessage()).contains("fora do ar");
            assertThat(e.getPausaSegundos()).isPositive();
        });
        agora.set(150);
        assertThat(p.circuito()).isEqualTo("teste");
        Runnable teste = p.reservar();
        assertThatThrownBy(p::reservar).hasMessageContaining("voltando");
        p.falha();
        teste.run();
        assertThat(p.circuito()).isEqualTo("aberto");
        agora.set(400);
        Runnable denovo = p.reservar();
        p.sucesso();
        denovo.run();
        assertThat(p.circuito()).isEqualTo("fechado");
    }
}
