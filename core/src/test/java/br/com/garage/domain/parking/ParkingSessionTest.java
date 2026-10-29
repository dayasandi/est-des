package br.com.garage.domain.parking;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.garage.domain.garage.Coordinates;
import br.com.garage.domain.garage.SectorCode;
import br.com.garage.domain.garage.Spot;
import br.com.garage.domain.parking.ParkingSession.Closed;
import br.com.garage.domain.parking.ParkingSession.Entered;
import br.com.garage.domain.parking.ParkingSession.Parked;
import br.com.garage.domain.shared.Money;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ParkingSessionTest {

    private static final LicensePlate PLATE = new LicensePlate("ZUL0001");
    private static final Instant ENTRY = Instant.parse("2026-01-01T12:00:00Z");
    private static final Spot SPOT_A = new Spot(
            7, new SectorCode("A"), new Coordinates(new BigDecimal("-23.561564"), new BigDecimal("-46.655861")));

    private final Entered entered = new Entered(PLATE, ENTRY);

    @Test
    void estacionarTravaVagaSetorPrecoEFator() {
        Parked parked = entered.parkAt(SPOT_A, Money.of("40.50"), new BigDecimal("1.10"));

        assertThat(parked.spotId()).isEqualTo(7);
        assertThat(parked.sector()).isEqualTo(new SectorCode("A"));
        assertThat(parked.hourlyPrice()).isEqualTo(Money.of("40.50"));
        assertThat(parked.priceFactor()).isEqualByComparingTo("1.10");
        assertThat(parked.entryTime()).isEqualTo(ENTRY);
    }

    @Test
    void saidaCobraPelaPermanenciaComOPrecoTravadoNoEstacionamento() {
        Parked parked = entered.parkAt(SPOT_A, Money.of("40.50"), new BigDecimal("1.10"));

        Closed closed = parked.leave(ENTRY.plusSeconds(2 * 3600 + 1));

        assertThat(closed.amount()).isEqualTo(Money.of("133.65"));
        assertThat(closed.exitTime()).isEqualTo(ENTRY.plusSeconds(2 * 3600 + 1));
        assertThat(closed.plate()).isEqualTo(PLATE);
    }

    @Test
    void saidaDentroDosTrintaMinutosNaoCobra() {
        Parked parked = entered.parkAt(SPOT_A, Money.of("40.50"), BigDecimal.ONE);

        assertThat(parked.leave(ENTRY.plusSeconds(30 * 60)).amount()).isEqualTo(Money.ZERO);
    }

    @Test
    void saidaComHorarioAnteriorAEntradaNaoCobraNemFalha() {
        Parked parked = entered.parkAt(SPOT_A, Money.of("40.50"), BigDecimal.ONE);

        assertThat(parked.leave(ENTRY.minusSeconds(5)).amount()).isEqualTo(Money.ZERO);
    }

    @Test
    void saidaSemTerEstacionadoNaoTemTarifaPoisOSetorEDesconhecido() {
        Closed closed = entered.leave(ENTRY.plusSeconds(10 * 3600));

        assertThat(closed.amount()).isEqualTo(Money.ZERO);
    }
}
