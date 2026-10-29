package br.com.garage.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.garage.fixture.TestGarage;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ConcurrencyE2eTest extends E2eSupport {

    private <T> List<T> inParallel(List<Callable<T>> tasks) throws Exception {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            return executor.invokeAll(tasks).stream().map(ConcurrencyE2eTest::join).toList();
        }
    }

    private static <T> T join(Future<T> future) {
        try {
            return future.get();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static long count(List<Reply> replies, int status) {
        return replies.stream().filter(r -> r.status() == status).count();
    }

    private int insideCount() {
        return jdbc.sql("SELECT COUNT(*) FROM parking_session WHERE status <> 'CLOSED'")
                .query(Integer.class)
                .single();
    }

    @Test
    void sessentaEntradasSimultaneasParaTrintaVagasAceitamExatamenteTrinta() throws Exception {
        List<Callable<Reply>> tasks = IntStream.rangeClosed(1, 60)
                .<Callable<Reply>>mapToObj(n -> () -> enter("RUSH%04d".formatted(n), "2026-10-29T02:00:00"))
                .toList();

        List<Reply> replies = inParallel(tasks);

        assertThat(count(replies, 200)).isEqualTo(30);
        assertThat(count(replies, 409)).isEqualTo(30);
        assertThat(reserved()).isEqualTo(30);
        assertThat(insideCount()).isEqualTo(30);
    }

    @Test
    void entradasSimultaneasDaMesmaPlacaCriamUmaSoPermanencia() throws Exception {
        List<Callable<Reply>> tasks = IntStream.range(0, 20)
                .<Callable<Reply>>mapToObj(n -> () -> enter("SAME001", "2026-10-29T02:00:00"))
                .toList();

        List<Reply> replies = inParallel(tasks);

        assertThat(replies).allMatch(r -> r.status() == 200);
        assertThat(replies.stream().filter(r -> "ACCEPTED".equals(r.body().get("status").asString())))
                .hasSize(1);
        assertThat(reserved()).isEqualTo(1);
    }

    @Test
    void dezVeiculosDisputandoAMesmaVagaApenasUmEstaciona() throws Exception {
        IntStream.rangeClosed(1, 10).forEach(n -> enter("SPOT%04d".formatted(n), "2026-10-29T02:00:00"));
        List<Callable<Reply>> tasks = IntStream.rangeClosed(1, 10)
                .<Callable<Reply>>mapToObj(n -> () -> park("SPOT%04d".formatted(n), 1))
                .toList();

        List<Reply> replies = inParallel(tasks);

        assertThat(count(replies, 200)).isEqualTo(1);
        assertThat(count(replies, 409)).isEqualTo(9);
        assertThat(jdbc.sql("SELECT COUNT(*) FROM parking_session WHERE status = 'PARKED'")
                        .query(Integer.class)
                        .single())
                .isEqualTo(1);
    }

    @Test
    void saidasSimultaneasDaMesmaPlacaCobramUmaVezELiberamUmaVaga() throws Exception {
        enter("EXIT001", "2026-10-29T02:00:00");
        park("EXIT001", 1);
        enter("OTHER01", "2026-10-29T02:00:00");
        List<Callable<Reply>> tasks = IntStream.range(0, 10)
                .<Callable<Reply>>mapToObj(n -> () -> leave("EXIT001", "2026-10-29T03:00:00"))
                .toList();

        List<Reply> replies = inParallel(tasks);

        assertThat(replies).allMatch(r -> r.status() == 200);
        assertThat(replies.stream().filter(r -> "CHARGED".equals(r.body().get("status").asString())))
                .hasSize(1);
        assertThat(reserved()).isEqualTo(1);
        assertThat(get("/revenue?date=2026-10-29").body().get("amount").decimalValue()).isEqualByComparingTo("36.45");
    }

    @Test
    void entradasESaidasMisturadasMantemOContadorIgualAosVeiculosDentro() throws Exception {
        IntStream.rangeClosed(1, 15).forEach(n -> enter("MIX%05d".formatted(n), "2026-10-29T02:00:00"));
        List<Callable<Reply>> tasks = IntStream.rangeClosed(1, 15)
                .boxed()
                .flatMap(n -> Stream.<Callable<Reply>>of(
                        () -> leave("MIX%05d".formatted(n), "2026-10-29T02:30:00"),
                        () -> enter("NEW%05d".formatted(n), "2026-10-29T02:31:00")))
                .toList();

        List<Reply> replies = inParallel(tasks);

        assertThat(replies).noneMatch(r -> r.status() >= 500);
        assertThat(reserved()).isEqualTo(insideCount()).isLessThanOrEqualTo(TestGarage.CONFIG.totalCapacity());
    }
}
