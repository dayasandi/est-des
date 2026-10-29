package br.com.garage.observability;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.garage.application.port.inbound.EntryOutcome;
import br.com.garage.application.port.inbound.ExitResult;
import br.com.garage.application.port.inbound.ParkingResult;
import br.com.garage.domain.parking.LicensePlate;
import br.com.garage.domain.shared.Money;
import br.com.garage.fixture.TestGarage;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class MeteredUseCasesTest {

    private static final LicensePlate PLATE = new LicensePlate("ABC1234");
    private static final Instant NOW = Instant.parse("2026-10-29T12:00:00Z");

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final EventMetrics metrics = new EventMetrics(registry);

    private double count(String event, String outcome) {
        return registry.counter("garage.events", "event", event, "outcome", outcome).count();
    }

    @Test
    void entradaContaPorResultadoEDevolveOResultadoDoCasoDeUso() {
        var entry = new MeteredRegisterEntry((plate, time) -> EntryOutcome.GARAGE_FULL, metrics);

        assertThat(entry.register(PLATE, NOW)).isEqualTo(EntryOutcome.GARAGE_FULL);

        assertThat(count("entry", "garage_full")).isEqualTo(1.0);
        assertThat(count("entry", "accepted")).isZero();
    }

    @Test
    void estacionamentoUsaONomeDoResultadoEmSnakeCase() {
        var parking = new MeteredRegisterParking((plate, location) -> new ParkingResult.SpotTaken(), metrics);

        assertThat(parking.register(PLATE, TestGarage.location(1))).isEqualTo(new ParkingResult.SpotTaken());

        assertThat(count("parked", "spot_taken")).isEqualTo(1.0);
    }

    @Test
    void saidaCobradaRegistraOValorNaDistribuicao() {
        var exit = new MeteredRegisterExit(
                (plate, time) -> new ExitResult.Charged(TestGarage.A, Money.of("72.90")), metrics, registry);

        exit.register(PLATE, NOW);
        exit.register(PLATE, NOW);

        assertThat(count("exit", "charged")).isEqualTo(2.0);
        var summary = registry.get("garage.exit.charged").summary();
        assertThat(summary.count()).isEqualTo(2);
        assertThat(summary.totalAmount()).isEqualTo(145.80);
    }

    @Test
    void saidaSemCobrancaNaoEntraNaDistribuicaoDeValores() {
        var exit = new MeteredRegisterExit((plate, time) -> new ExitResult.NoActiveSession(), metrics, registry);

        exit.register(PLATE, NOW);

        assertThat(count("exit", "no_active_session")).isEqualTo(1.0);
        assertThat(registry.get("garage.exit.charged").summary().count()).isZero();
    }

    @Test
    void nomeDoResultadoViraSnakeCase() {
        assertThat(EventMetrics.outcomeName(EntryOutcome.ALREADY_INSIDE)).isEqualTo("already_inside");
        assertThat(EventMetrics.outcomeName(new ParkingResult.ParkedElsewhere())).isEqualTo("parked_elsewhere");
        assertThat(EventMetrics.outcomeName(new ExitResult.LeftWithoutParking())).isEqualTo("left_without_parking");
    }
}
