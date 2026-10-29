package br.com.garage.application.usecase;

import static br.com.garage.fixture.TestGarage.A;
import static br.com.garage.fixture.TestGarage.B;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.garage.application.port.inbound.GetRevenue;
import br.com.garage.application.port.inbound.RevenueResult.Total;
import br.com.garage.application.port.inbound.RevenueResult.UnknownSector;
import br.com.garage.domain.garage.SectorCode;
import br.com.garage.domain.parking.LicensePlate;
import br.com.garage.domain.parking.ParkingSession.Entered;
import br.com.garage.domain.shared.Money;
import br.com.garage.fixture.InMemoryGarageRepositories;
import br.com.garage.fixture.InMemoryRevenueReader;
import br.com.garage.fixture.InMemorySessionRepository;
import br.com.garage.fixture.TestGarage;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetRevenueUseCaseTest {

    private static final LocalDate DAY = LocalDate.parse("2026-01-01");

    private final InMemorySessionRepository sessions = new InMemorySessionRepository();
    private final GetRevenue revenue = new GetRevenueUseCase(
            new InMemoryRevenueReader(sessions), InMemoryGarageRepositories.sectorsOfTestGarage());

    private void chargedExit(String plate, int spotId, Instant exitAt, long hours) {
        LicensePlate licensePlate = new LicensePlate(plate);
        Entered entered = new Entered(licensePlate, exitAt.minusSeconds(hours * 3600));
        sessions.add(entered);
        var spot = TestGarage.spot(spotId);
        var price = spot.sector().equals(A) ? TestGarage.SECTOR_A.basePrice() : TestGarage.SECTOR_B.basePrice();
        var parked = entered.parkAt(spot, price, BigDecimal.ONE);
        sessions.park(parked);
        sessions.close(parked.leave(exitAt));
    }

    @Test
    void somaAReceitaDoDiaDeTodosOsSetoresQuandoNaoHaFiltro() {
        chargedExit("AAA0001", 1, Instant.parse("2026-01-01T10:00:00Z"), 1);
        chargedExit("BBB0002", 11, Instant.parse("2026-01-01T11:00:00Z"), 2);

        assertThat(revenue.revenueOf(DAY, Optional.empty())).isEqualTo(new Total(Money.of("48.70")));
    }

    @Test
    void filtraPorSetor() {
        chargedExit("AAA0001", 1, Instant.parse("2026-01-01T10:00:00Z"), 1);
        chargedExit("BBB0002", 11, Instant.parse("2026-01-01T11:00:00Z"), 2);

        assertThat(revenue.revenueOf(DAY, Optional.of(A))).isEqualTo(new Total(Money.of("40.50")));
        assertThat(revenue.revenueOf(DAY, Optional.of(B))).isEqualTo(new Total(Money.of("8.20")));
    }

    @Test
    void oDiaVaiDaMeiaNoiteUtcInclusiveAteAMeiaNoiteSeguinteExclusive() {
        chargedExit("AAA0001", 1, Instant.parse("2025-12-31T23:59:59.999Z"), 1);
        chargedExit("BBB0002", 2, Instant.parse("2026-01-01T00:00:00Z"), 1);
        chargedExit("CCC0003", 3, Instant.parse("2026-01-01T23:59:59.999Z"), 1);
        chargedExit("DDD0004", 4, Instant.parse("2026-01-02T00:00:00Z"), 1);

        assertThat(revenue.revenueOf(DAY, Optional.empty())).isEqualTo(new Total(Money.of("81.00")));
    }

    @Test
    void diaSemSaidasTemReceitaZero() {
        assertThat(revenue.revenueOf(DAY, Optional.empty())).isEqualTo(new Total(Money.ZERO));
        assertThat(revenue.revenueOf(DAY, Optional.of(A))).isEqualTo(new Total(Money.ZERO));
    }

    @Test
    void setorInexistenteEDistintoDeSetorSemReceita() {
        assertThat(revenue.revenueOf(DAY, Optional.of(new SectorCode("Z")))).isEqualTo(new UnknownSector());
    }
}
