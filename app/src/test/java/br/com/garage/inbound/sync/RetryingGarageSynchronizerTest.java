package br.com.garage.inbound.sync;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.garage.application.port.inbound.SyncGarage;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class RetryingGarageSynchronizerTest {

    private final GarageSyncState state = new GarageSyncState();
    private final List<Duration> pauses = new ArrayList<>();

    private RetryingGarageSynchronizer synchronizer(SyncGarage sync, RetryingGarageSynchronizer.Sleeper sleeper) {
        return new RetryingGarageSynchronizer(
                sync, state, sleeper, Duration.ofMillis(100), Duration.ofMillis(500));
    }

    private static SyncGarage failingTimes(int failures, AtomicInteger calls) {
        return () -> {
            if (calls.incrementAndGet() <= failures) {
                throw new IllegalStateException("simulador fora do ar");
            }
        };
    }

    @Test
    void sincronizaNaPrimeiraTentativaSemEsperar() {
        AtomicInteger calls = new AtomicInteger();

        synchronizer(failingTimes(0, calls), pauses::add).run();

        assertThat(state.isReady()).isTrue();
        assertThat(calls).hasValue(1);
        assertThat(pauses).isEmpty();
    }

    @Test
    void tentaDeNovoComEsperaExponencialLimitadaAteConseguir() {
        AtomicInteger calls = new AtomicInteger();

        synchronizer(failingTimes(5, calls), pauses::add).run();

        assertThat(state.isReady()).isTrue();
        assertThat(calls).hasValue(6);
        assertThat(pauses)
                .containsExactly(
                        Duration.ofMillis(100),
                        Duration.ofMillis(200),
                        Duration.ofMillis(400),
                        Duration.ofMillis(500),
                        Duration.ofMillis(500));
    }

    @Test
    void naoFicaProntoEnquantoASincronizacaoFalha() {
        AtomicInteger calls = new AtomicInteger();
        List<Boolean> readyDuringRetries = new ArrayList<>();

        synchronizer(failingTimes(2, calls), duration -> readyDuringRetries.add(state.isReady())).run();

        assertThat(readyDuringRetries).containsExactly(false, false);
        assertThat(state.isReady()).isTrue();
    }

    @Test
    void interrupcaoEncerraAsTentativasSemFicarPronto() {
        AtomicInteger calls = new AtomicInteger();

        synchronizer(failingTimes(100, calls), duration -> {
                    throw new InterruptedException();
                })
                .run();

        assertThat(state.isReady()).isFalse();
        assertThat(calls).hasValue(1);
        assertThat(Thread.interrupted()).isTrue();
    }
}
