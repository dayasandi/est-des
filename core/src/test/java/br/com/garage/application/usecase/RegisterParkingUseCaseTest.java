package br.com.garage.application.usecase;

import static br.com.garage.fixture.TestGarage.A;
import static br.com.garage.fixture.TestGarage.B;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.garage.application.port.inbound.ParkingResult;
import br.com.garage.application.port.inbound.ParkingResult.Accepted;
import br.com.garage.application.port.inbound.ParkingResult.AlreadyParked;
import br.com.garage.application.port.inbound.ParkingResult.NoActiveEntry;
import br.com.garage.application.port.inbound.ParkingResult.ParkedElsewhere;
import br.com.garage.application.port.inbound.ParkingResult.SpotTaken;
import br.com.garage.application.port.inbound.ParkingResult.UnknownSpot;
import br.com.garage.application.port.inbound.RegisterParking;
import br.com.garage.domain.garage.Coordinates;
import br.com.garage.domain.garage.Occupancy;
import br.com.garage.domain.parking.LicensePlate;
import br.com.garage.domain.parking.ParkingSession.Entered;
import br.com.garage.domain.parking.ParkingSession.Parked;
import br.com.garage.domain.pricing.PriceTable;
import br.com.garage.domain.pricing.PricingPolicy;
import br.com.garage.domain.shared.Money;
import br.com.garage.fixture.DirectTransactionRunner;
import br.com.garage.fixture.InMemoryGarageRepositories;
import br.com.garage.fixture.InMemorySessionRepository;
import br.com.garage.fixture.TestGarage;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class RegisterParkingUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");

    private final InMemorySessionRepository sessions = new InMemorySessionRepository();
    private final List<Occupancy> seenByPolicy = new ArrayList<>();

    private RegisterParking parking(PricingPolicy policy) {
        return new RegisterParkingUseCase(
                sessions,
                InMemoryGarageRepositories.spotsOfTestGarage(),
                InMemoryGarageRepositories.sectorsOfTestGarage(),
                policy,
                new DirectTransactionRunner());
    }

    private static LicensePlate plate(int n) {
        return new LicensePlate("CAR%04d".formatted(n));
    }

    private void enter(LicensePlate plate) {
        sessions.add(new Entered(plate, NOW));
    }

    @Test
    void estacionaNoSetorDaVagaEGuardaOPrecoDoSetorEOFatorDaPolitica() {
        enter(plate(1));
        PricingPolicy fixed = occupancy -> new BigDecimal("1.25");

        ParkingResult result = parking(fixed).register(plate(1), TestGarage.location(12));

        assertThat(result).isEqualTo(new Accepted(B, new BigDecimal("1.25")));
        Parked stored = (Parked) sessions.findActive(plate(1)).orElseThrow();
        assertThat(stored.sector()).isEqualTo(B);
        assertThat(stored.spotId()).isEqualTo(12);
        assertThat(stored.hourlyPrice()).isEqualTo(Money.of("4.10"));
        assertThat(stored.priceFactor()).isEqualByComparingTo("1.25");
    }

    @Test
    void entregaAPoliticaALotacaoDoSetorSemContarOProprioVeiculo() {
        IntStream.rangeClosed(1, 3).forEach(n -> {
            enter(plate(n));
            parking(o -> BigDecimal.ONE).register(plate(n), TestGarage.location(n));
        });
        enter(plate(10));
        enter(plate(11));

        parking(occupancy -> {
                    seenByPolicy.add(occupancy);
                    return BigDecimal.ONE;
                })
                .register(plate(10), TestGarage.location(4));
        parking(occupancy -> {
                    seenByPolicy.add(occupancy);
                    return BigDecimal.ONE;
                })
                .register(plate(11), TestGarage.location(11));

        assertThat(seenByPolicy).containsExactly(new Occupancy(3, 10), new Occupancy(0, 20));
    }

    @Test
    void aplicaAsFaixasPadraoConformeOSetorEnche() {
        RegisterParking realPricing = parking(PriceTable.standard());

        List<BigDecimal> factors = IntStream.rangeClosed(1, 10)
                .mapToObj(n -> {
                    enter(plate(n));
                    return ((Accepted) realPricing.register(plate(n), TestGarage.location(n))).priceFactor();
                })
                .toList();

        assertThat(factors)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(
                        bd("0.90"), bd("0.90"), bd("0.90"),
                        bd("1.00"), bd("1.00"),
                        bd("1.10"), bd("1.10"), bd("1.10"),
                        bd("1.25"), bd("1.25"));
    }

    @Test
    void coordenadaSemVagaCadastradaNaoMudaNada() {
        enter(plate(1));
        Coordinates nowhere = new Coordinates(new BigDecimal("1.000000"), new BigDecimal("2.000000"));

        assertThat(parking(o -> BigDecimal.ONE).register(plate(1), nowhere)).isEqualTo(new UnknownSpot());

        assertThat(sessions.findActive(plate(1))).contains(new Entered(plate(1), NOW));
    }

    @Test
    void semEntradaAtivaNaoEstaciona() {
        assertThat(parking(o -> BigDecimal.ONE).register(plate(1), TestGarage.location(1)))
                .isEqualTo(new NoActiveEntry());
        assertThat(sessions.findActive(plate(1))).isEmpty();
    }

    @Test
    void vagaOcupadaPorOutroVeiculoRecusaESegueSoComEntrada() {
        enter(plate(1));
        enter(plate(2));
        RegisterParking parking = parking(o -> BigDecimal.ONE);
        parking.register(plate(1), TestGarage.location(5));

        assertThat(parking.register(plate(2), TestGarage.location(5))).isEqualTo(new SpotTaken());

        assertThat(sessions.findActive(plate(2))).contains(new Entered(plate(2), NOW));
    }

    @Test
    void eventoRepetidoNaMesmaVagaEIdempotente() {
        enter(plate(1));
        RegisterParking parking = parking(o -> BigDecimal.ONE);
        parking.register(plate(1), TestGarage.location(5));

        assertThat(parking.register(plate(1), TestGarage.location(5))).isEqualTo(new AlreadyParked());
    }

    @Test
    void veiculoJaEstacionadoNaoMudaParaOutraVaga() {
        enter(plate(1));
        RegisterParking parking = parking(o -> BigDecimal.ONE);
        parking.register(plate(1), TestGarage.location(5));

        assertThat(parking.register(plate(1), TestGarage.location(6))).isEqualTo(new ParkedElsewhere());

        assertThat(((Parked) sessions.findActive(plate(1)).orElseThrow()).spotId()).isEqualTo(5);
    }

    @Test
    void ocupacaoEContadaPorSetorEntreSetoresDiferentes() {
        enter(plate(1));
        enter(plate(2));
        RegisterParking parking = parking(PriceTable.standard());
        parking.register(plate(1), TestGarage.location(1));

        Accepted inB = (Accepted) parking.register(plate(2), TestGarage.location(11));

        assertThat(inB.sector()).isEqualTo(B);
        assertThat(inB.priceFactor()).isEqualByComparingTo("0.90");
        assertThat(sessions.countParkedIn(A)).isEqualTo(1);
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
