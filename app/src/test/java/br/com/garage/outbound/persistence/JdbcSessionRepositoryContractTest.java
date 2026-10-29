package br.com.garage.outbound.persistence;

import static br.com.garage.fixture.TestGarage.A;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.garage.application.port.outbound.SessionRepository;
import br.com.garage.contract.SessionRepositoryContract;
import br.com.garage.domain.parking.LicensePlate;
import br.com.garage.domain.parking.ParkingSession.Entered;
import br.com.garage.domain.shared.Money;
import br.com.garage.fixture.TestGarage;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JdbcSessionRepositoryContractTest extends SessionRepositoryContract {

    private static final Instant ENTRY = Instant.parse("2026-01-01T12:00:00Z");

    private final JdbcSessionRepository repository = new JdbcSessionRepository(MySqlSupport.jdbc());

    @BeforeEach
    void freshGarage() {
        MySqlSupport.resetWithTestGarage();
    }

    @Override
    protected SessionRepository repository() {
        return repository;
    }

    @Test
    void entradasSimultaneasDaMesmaPlacaGravamApenasUma() throws Exception {
        LicensePlate plate = new LicensePlate("RACE001");

        List<Boolean> results = inParallel(
                20, () -> repository.add(new Entered(plate, ENTRY)));

        assertThat(results).filteredOn(Boolean::booleanValue).hasSize(1);
        assertThat(repository.findActive(plate)).isPresent();
    }

    @Test
    void veiculosSimultaneosNaMesmaVagaApenasUmVence() throws Exception {
        List<LicensePlate> plates = IntStream.range(0, 20)
                .mapToObj(n -> new LicensePlate("SPOT%03d".formatted(n)))
                .toList();
        plates.forEach(plate -> repository.add(new Entered(plate, ENTRY)));

        List<Callable<Boolean>> tasks = new ArrayList<>();
        plates.forEach(plate -> tasks.add(() -> repository.park(
                new Entered(plate, ENTRY).parkAt(TestGarage.spot(1), Money.of("40.50"), BigDecimal.ONE))));
        List<Boolean> results = runAll(tasks);

        assertThat(results).filteredOn(Boolean::booleanValue).hasSize(1);
        assertThat(repository.countParkedIn(A)).isEqualTo(1);
    }

    @Test
    void precisaoDeMilissegundosENormalizadaEmUtc() {
        Instant withMillis = Instant.parse("2026-03-10T23:59:59.123Z");
        LicensePlate plate = new LicensePlate("UTC0001");

        repository.add(new Entered(plate, withMillis));

        assertThat(repository.findActive(plate)).contains(new Entered(plate, withMillis));
    }

    private <T> List<T> inParallel(int times, Callable<T> task) throws Exception {
        return runAll(IntStream.range(0, times).mapToObj(n -> task).toList());
    }

    private <T> List<T> runAll(List<Callable<T>> tasks) throws Exception {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<T>> futures = executor.invokeAll(tasks);
            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) {
                results.add(future.get());
            }
            return results;
        }
    }
}
