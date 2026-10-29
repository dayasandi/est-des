package br.com.garage.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.garage.application.port.inbound.EntryOutcome;
import br.com.garage.application.port.inbound.RegisterEntry;
import br.com.garage.domain.parking.LicensePlate;
import br.com.garage.domain.parking.ParkingSession.Entered;
import br.com.garage.fixture.DirectTransactionRunner;
import br.com.garage.fixture.InMemoryCapacityGate;
import br.com.garage.fixture.InMemorySessionRepository;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RegisterEntryUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");
    private static final LicensePlate PLATE_1 = new LicensePlate("AAA0001");
    private static final LicensePlate PLATE_2 = new LicensePlate("BBB0002");
    private static final LicensePlate PLATE_3 = new LicensePlate("CCC0003");

    private final InMemorySessionRepository sessions = new InMemorySessionRepository();
    private final InMemoryCapacityGate capacity = new InMemoryCapacityGate();
    private final RegisterEntry entry = new RegisterEntryUseCase(sessions, capacity, new DirectTransactionRunner());

    @BeforeEach
    void garagemComDuasVagas() {
        capacity.configure(2);
    }

    @Test
    void aceitaAEntradaEReservaUmaVaga() {
        assertThat(entry.register(PLATE_1, NOW)).isEqualTo(EntryOutcome.ACCEPTED);

        assertThat(sessions.findActive(PLATE_1)).contains(new Entered(PLATE_1, NOW));
        assertThat(capacity.reserved()).isEqualTo(1);
    }

    @Test
    void entradaRepetidaDaMesmaPlacaNaoReservaOutraVaga() {
        entry.register(PLATE_1, NOW);

        assertThat(entry.register(PLATE_1, NOW.plusSeconds(5))).isEqualTo(EntryOutcome.ALREADY_INSIDE);

        assertThat(capacity.reserved()).isEqualTo(1);
        assertThat(sessions.findActive(PLATE_1)).contains(new Entered(PLATE_1, NOW));
    }

    @Test
    void recusaAEntradaQuandoAGaragemEstaCheiaSemCriarPermanencia() {
        entry.register(PLATE_1, NOW);
        entry.register(PLATE_2, NOW);

        assertThat(entry.register(PLATE_3, NOW)).isEqualTo(EntryOutcome.GARAGE_FULL);

        assertThat(sessions.findActive(PLATE_3)).isEmpty();
        assertThat(capacity.reserved()).isEqualTo(2);
    }

    @Test
    void placaQueJaEstaDentroContinuaSendoRepeticaoMesmoComAGaragemCheia() {
        entry.register(PLATE_1, NOW);
        entry.register(PLATE_2, NOW);

        assertThat(entry.register(PLATE_1, NOW)).isEqualTo(EntryOutcome.ALREADY_INSIDE);
    }

    @Test
    void devolveAReservaQuandoUmEventoConcorrenteDaMesmaPlacaVenceACorrida() {
        InMemorySessionRepository racing = new InMemorySessionRepository() {
            @Override
            public boolean add(Entered entered) {
                return false;
            }
        };
        RegisterEntry racingEntry = new RegisterEntryUseCase(racing, capacity, new DirectTransactionRunner());

        assertThat(racingEntry.register(PLATE_1, NOW)).isEqualTo(EntryOutcome.ALREADY_INSIDE);

        assertThat(capacity.reserved()).isZero();
    }
}
