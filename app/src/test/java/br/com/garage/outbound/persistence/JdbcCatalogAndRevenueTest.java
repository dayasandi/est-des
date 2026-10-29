package br.com.garage.outbound.persistence;

import static br.com.garage.fixture.TestGarage.A;
import static br.com.garage.fixture.TestGarage.B;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.garage.domain.garage.Coordinates;
import br.com.garage.domain.garage.Sector;
import br.com.garage.domain.garage.SectorCode;
import br.com.garage.domain.parking.LicensePlate;
import br.com.garage.domain.parking.ParkingSession.Entered;
import br.com.garage.domain.shared.Money;
import br.com.garage.fixture.TestGarage;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JdbcCatalogAndRevenueTest {

    private final JdbcSectorRepository sectors = new JdbcSectorRepository(MySqlSupport.jdbc());
    private final JdbcSpotRepository spots = new JdbcSpotRepository(MySqlSupport.jdbc());
    private final JdbcSessionRepository sessions = new JdbcSessionRepository(MySqlSupport.jdbc());
    private final JdbcRevenueReader revenue = new JdbcRevenueReader(MySqlSupport.jdbc());

    @BeforeEach
    void freshGarage() {
        MySqlSupport.resetWithTestGarage();
    }

    @Test
    void setorGravadoERecuperadoComPrecoECapacidade() {
        assertThat(sectors.findByCode(A)).contains(TestGarage.SECTOR_A);
        assertThat(sectors.findByCode(new SectorCode("Z"))).isEmpty();
    }

    @Test
    void gravarOSetorDeNovoAtualizaEmVezDeDuplicar() {
        sectors.saveAll(List.of(new Sector(A, Money.of("50.00"), 12)));

        assertThat(sectors.findByCode(A)).contains(new Sector(A, Money.of("50.00"), 12));
    }

    @Test
    void vagaEEncontradaPelaCoordenadaExata() {
        assertThat(spots.findByLocation(TestGarage.location(12))).contains(TestGarage.spot(12));
    }

    @Test
    void coordenadaDeOutraPosicaoNaoEncontraVaga() {
        Coordinates elsewhere = new Coordinates(new BigDecimal("10.000000"), new BigDecimal("10.000000"));

        assertThat(spots.findByLocation(elsewhere)).isEmpty();
    }

    @Test
    void gravarAsVagasDeNovoEIdempotente() {
        spots.saveAll(TestGarage.SPOTS);

        assertThat(MySqlSupport.jdbc().sql("SELECT COUNT(*) FROM spot").query(Integer.class).single())
                .isEqualTo(30);
    }

    private void closedAt(String plate, int spotId, Instant exit, long hours) {
        LicensePlate licensePlate = new LicensePlate(plate);
        Entered entered = new Entered(licensePlate, exit.minusSeconds(hours * 3600));
        sessions.add(entered);
        var spot = TestGarage.spot(spotId);
        var price = spot.sector().equals(A) ? TestGarage.SECTOR_A.basePrice() : TestGarage.SECTOR_B.basePrice();
        var parked = entered.parkAt(spot, price, BigDecimal.ONE);
        sessions.park(parked);
        sessions.close(parked.leave(exit));
    }

    @Test
    void receitaSomaSoSaidasRealizadasNoDiaUtcPorSetorOuTotal() {
        closedAt("AAA0001", 1, Instant.parse("2026-01-01T10:00:00Z"), 1);
        closedAt("BBB0002", 11, Instant.parse("2026-01-01T11:00:00Z"), 2);
        closedAt("CCC0003", 2, Instant.parse("2026-01-02T00:00:00Z"), 1);
        closedAt("DDD0004", 3, Instant.parse("2025-12-31T23:59:59Z"), 1);
        sessions.add(new Entered(new LicensePlate("EEE0005"), Instant.parse("2026-01-01T09:00:00Z")));

        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-01-02T00:00:00Z");

        assertThat(revenue.total(from, to)).isEqualTo(Money.of("48.70"));
        assertThat(revenue.totalOf(A, from, to)).isEqualTo(Money.of("40.50"));
        assertThat(revenue.totalOf(B, from, to)).isEqualTo(Money.of("8.20"));
    }

    @Test
    void semSaidasNoPeriodoAReceitaEZero() {
        Instant from = Instant.parse("2026-01-01T00:00:00Z");

        assertThat(revenue.total(from, from.plusSeconds(86_400))).isEqualTo(Money.ZERO);
        assertThat(revenue.totalOf(A, from, from.plusSeconds(86_400))).isEqualTo(Money.ZERO);
    }
}
