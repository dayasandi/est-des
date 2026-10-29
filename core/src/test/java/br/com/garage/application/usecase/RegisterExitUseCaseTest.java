package br.com.garage.application.usecase;

import static br.com.garage.fixture.TestGarage.A;
import static br.com.garage.fixture.TestGarage.B;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.garage.application.port.inbound.ExitResult;
import br.com.garage.application.port.inbound.ExitResult.Charged;
import br.com.garage.application.port.inbound.ExitResult.LeftWithoutParking;
import br.com.garage.application.port.inbound.ExitResult.NoActiveSession;
import br.com.garage.application.port.inbound.RegisterExit;
import br.com.garage.domain.parking.LicensePlate;
import br.com.garage.domain.parking.ParkingSession.Closed;
import br.com.garage.domain.parking.ParkingSession.Entered;
import br.com.garage.domain.shared.Money;
import br.com.garage.fixture.DirectTransactionRunner;
import br.com.garage.fixture.InMemoryCapacityGate;
import br.com.garage.fixture.InMemorySessionRepository;
import br.com.garage.fixture.TestGarage;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RegisterExitUseCaseTest {

    private static final Instant ENTRY = Instant.parse("2026-01-01T12:00:00Z");
    private static final LicensePlate PLATE = new LicensePlate("ZUL0001");

    private final InMemorySessionRepository sessions = new InMemorySessionRepository();
    private final InMemoryCapacityGate capacity = new InMemoryCapacityGate();
    private final RegisterExit exit = new RegisterExitUseCase(sessions, capacity, new DirectTransactionRunner());

    @BeforeEach
    void garagemComDuasVagas() {
        capacity.configure(2);
    }

    private void parkAt(LicensePlate plate, int spotId, String factor) {
        Entered entered = new Entered(plate, ENTRY);
        capacity.tryReserve();
        sessions.add(entered);
        Money hourly = TestGarage.spot(spotId).sector().equals(A) ? TestGarage.SECTOR_A.basePrice() : TestGarage.SECTOR_B.basePrice();
        sessions.park(entered.parkAt(TestGarage.spot(spotId), hourly, new BigDecimal(factor)));
    }

    @Test
    void cobraPelaPermanenciaComOPrecoTravadoNoEstacionamentoELiberaAVaga() {
        parkAt(PLATE, 3, "1.10");

        ExitResult result = exit.register(PLATE, ENTRY.plus(Duration.ofMinutes(90)));

        assertThat(result).isEqualTo(new Charged(A, Money.of("89.10")));
        assertThat(sessions.findActive(PLATE)).isEmpty();
        assertThat(capacity.reserved()).isZero();
        assertThat(sessions.closedSessions()).singleElement().satisfies(entry -> {
            assertThat(entry.sector()).contains(A);
            assertThat(entry.session().exitTime()).isEqualTo(ENTRY.plus(Duration.ofMinutes(90)));
        });
    }

    @Test
    void naoCobraAteTrintaMinutosMasLiberaAVaga() {
        parkAt(PLATE, 12, "1.00");

        ExitResult result = exit.register(PLATE, ENTRY.plus(Duration.ofMinutes(30)));

        assertThat(result).isEqualTo(new Charged(B, Money.ZERO));
        assertThat(capacity.reserved()).isZero();
    }

    @Test
    void saidaRepetidaNaoCobraDeNovoNemLiberaVagaDeOutroVeiculo() {
        parkAt(PLATE, 3, "1.00");
        parkAt(new LicensePlate("OUT0002"), 4, "1.00");
        exit.register(PLATE, ENTRY.plus(Duration.ofHours(1)));

        ExitResult repeated = exit.register(PLATE, ENTRY.plus(Duration.ofHours(1)));

        assertThat(repeated).isEqualTo(new NoActiveSession());
        assertThat(sessions.closedSessions()).hasSize(1);
        assertThat(capacity.reserved()).isEqualTo(1);
    }

    @Test
    void placaDesconhecidaNaoMudaNada() {
        assertThat(exit.register(PLATE, ENTRY)).isEqualTo(new NoActiveSession());

        assertThat(sessions.closedSessions()).isEmpty();
    }

    @Test
    void saidaSemTerEstacionadoLiberaACapacidadeSemTarifa() {
        capacity.tryReserve();
        sessions.add(new Entered(PLATE, ENTRY));

        ExitResult result = exit.register(PLATE, ENTRY.plus(Duration.ofHours(5)));

        assertThat(result).isEqualTo(new LeftWithoutParking());
        assertThat(capacity.reserved()).isZero();
        assertThat(sessions.closedSessions()).singleElement().satisfies(entry -> {
            assertThat(entry.session().amount()).isEqualTo(Money.ZERO);
            assertThat(entry.sector()).isEmpty();
        });
    }

    @Test
    void saidaConcorrenteQueInvadeEPerdeACorridaNaoCobraNemLiberaVagaDeNovo() {
        parkAt(PLATE, 3, "1.00");
        InMemorySessionRepository racing = new InMemorySessionRepository() {
            @Override
            public boolean close(Closed session) {
                return false;
            }
        };
        racing.add(new Entered(PLATE, ENTRY));
        racing.park(new Entered(PLATE, ENTRY).parkAt(TestGarage.spot(3), TestGarage.SECTOR_A.basePrice(), BigDecimal.ONE));
        RegisterExit racingExit = new RegisterExitUseCase(racing, capacity, new DirectTransactionRunner());

        ExitResult result = racingExit.register(PLATE, ENTRY.plus(Duration.ofHours(1)));

        assertThat(result).isEqualTo(new NoActiveSession());
        assertThat(capacity.reserved()).isEqualTo(1);
    }

    @Test
    void saidaSemEstacionarQuePerdeACorridaTambemNaoLibera() {
        capacity.tryReserve();
        InMemorySessionRepository racing = new InMemorySessionRepository() {
            @Override
            public boolean close(Closed session) {
                return false;
            }
        };
        racing.add(new Entered(PLATE, ENTRY));
        RegisterExit racingExit = new RegisterExitUseCase(racing, capacity, new DirectTransactionRunner());

        assertThat(racingExit.register(PLATE, ENTRY.plusSeconds(60))).isEqualTo(new NoActiveSession());
        assertThat(capacity.reserved()).isEqualTo(1);
    }

    @Test
    void horarioDeSaidaAnteriorAEntradaNaoFalhaNemCobra() {
        parkAt(PLATE, 3, "1.25");

        assertThat(exit.register(PLATE, ENTRY.minusSeconds(1))).isEqualTo(new Charged(A, Money.ZERO));
    }
}
