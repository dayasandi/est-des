package br.com.garage.contract;

import static br.com.garage.fixture.TestGarage.A;
import static br.com.garage.fixture.TestGarage.B;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.garage.application.port.outbound.SessionRepository;
import br.com.garage.domain.parking.LicensePlate;
import br.com.garage.domain.parking.ParkingSession;
import br.com.garage.domain.parking.ParkingSession.Closed;
import br.com.garage.domain.parking.ParkingSession.Entered;
import br.com.garage.domain.parking.ParkingSession.Parked;
import br.com.garage.domain.shared.Money;
import br.com.garage.fixture.TestGarage;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

public abstract class SessionRepositoryContract {

    private static final Instant ENTRY = Instant.parse("2026-01-01T12:00:00Z");
    private static final LicensePlate PLATE_1 = new LicensePlate("AAA0001");
    private static final LicensePlate PLATE_2 = new LicensePlate("BBB0002");

    protected abstract SessionRepository repository();

    private SessionRepository repo() {
        return repository();
    }

    private static Parked parkedAt(LicensePlate plate, int spotId) {
        return new Entered(plate, ENTRY)
                .parkAt(TestGarage.spot(spotId), Money.of("40.50"), new BigDecimal("1.10"));
    }

    @Test
    void naoHaPermanenciaAtivaParaPlacaDesconhecida() {
        assertThat(repo().findActive(PLATE_1)).isEmpty();
    }

    @Test
    void entradaGravadaFicaAtivaERecuperavel() {
        Entered entered = new Entered(PLATE_1, ENTRY);

        assertThat(repo().add(entered)).isTrue();

        assertThat(repo().findActive(PLATE_1)).contains(entered);
    }

    @Test
    void naoPermiteDuasPermanenciasAtivasParaAMesmaPlaca() {
        assertThat(repo().add(new Entered(PLATE_1, ENTRY))).isTrue();

        assertThat(repo().add(new Entered(PLATE_1, ENTRY.plusSeconds(60)))).isFalse();

        assertThat(repo().findActive(PLATE_1)).contains(new Entered(PLATE_1, ENTRY));
    }

    @Test
    void estacionarSubstituiAEntradaPorUmaPermanenciaEstacionada() {
        repo().add(new Entered(PLATE_1, ENTRY));
        Parked parked = parkedAt(PLATE_1, 3);

        assertThat(repo().park(parked)).isTrue();

        ParkingSession stored = repo().findActive(PLATE_1).orElseThrow();
        assertThat(stored).isInstanceOf(Parked.class);
        Parked storedParked = (Parked) stored;
        assertThat(storedParked.spotId()).isEqualTo(3);
        assertThat(storedParked.sector()).isEqualTo(A);
        assertThat(storedParked.hourlyPrice()).isEqualTo(Money.of("40.50"));
        assertThat(storedParked.priceFactor()).isEqualByComparingTo("1.10");
        assertThat(storedParked.entryTime()).isEqualTo(ENTRY);
    }

    @Test
    void vagaOcupadaNaoPodeSerTomadaPorOutroVeiculo() {
        repo().add(new Entered(PLATE_1, ENTRY));
        repo().add(new Entered(PLATE_2, ENTRY));
        assertThat(repo().park(parkedAt(PLATE_1, 3))).isTrue();

        assertThat(repo().park(parkedAt(PLATE_2, 3))).isFalse();

        assertThat(repo().findActive(PLATE_2)).contains(new Entered(PLATE_2, ENTRY));
    }

    @Test
    void mesmoVeiculoPodeReenviarOEstacionamentoNaMesmaVaga() {
        repo().add(new Entered(PLATE_1, ENTRY));
        assertThat(repo().park(parkedAt(PLATE_1, 3))).isTrue();

        assertThat(repo().park(parkedAt(PLATE_1, 3))).isTrue();
    }

    @Test
    void vagaLiberadaPelaSaidaPodeSerReocupada() {
        repo().add(new Entered(PLATE_1, ENTRY));
        Parked parked = parkedAt(PLATE_1, 3);
        repo().park(parked);
        repo().add(new Entered(PLATE_2, ENTRY));

        repo().close(parked.leave(ENTRY.plusSeconds(3600)));

        assertThat(repo().park(parkedAt(PLATE_2, 3))).isTrue();
    }

    @Test
    void encerrarRemoveAPermanenciaAtivaEPermiteNovaEntradaDaMesmaPlaca() {
        repo().add(new Entered(PLATE_1, ENTRY));
        Parked parked = parkedAt(PLATE_1, 3);
        repo().park(parked);

        Closed closed = parked.leave(ENTRY.plusSeconds(7200));
        repo().close(closed);

        assertThat(repo().findActive(PLATE_1)).isEmpty();
        assertThat(repo().add(new Entered(PLATE_1, ENTRY.plusSeconds(10_000)))).isTrue();
    }

    @Test
    void encerrarDuasVezesAMesmaPermanenciaSoContaNaPrimeira() {
        repo().add(new Entered(PLATE_1, ENTRY));
        Parked parked = parkedAt(PLATE_1, 3);
        repo().park(parked);
        Closed closed = parked.leave(ENTRY.plusSeconds(7200));

        assertThat(repo().close(closed)).isTrue();
        assertThat(repo().close(closed)).isFalse();
    }

    @Test
    void encerrarPlacaSemPermanenciaAtivaDevolveFalso() {
        Closed closed = new Entered(PLATE_1, ENTRY).leave(ENTRY.plusSeconds(60));

        assertThat(repo().close(closed)).isFalse();
    }

    @Test
    void contaApenasEstacionadosDoSetorIgnorandoEntradasEEncerrados() {
        LicensePlate p3 = new LicensePlate("CCC0003");
        LicensePlate p4 = new LicensePlate("DDD0004");
        LicensePlate p5 = new LicensePlate("EEE0005");
        repo().add(new Entered(PLATE_1, ENTRY));
        repo().add(new Entered(PLATE_2, ENTRY));
        repo().add(new Entered(p3, ENTRY));
        repo().add(new Entered(p4, ENTRY));
        repo().add(new Entered(p5, ENTRY));
        repo().park(parkedAt(PLATE_1, 1));
        repo().park(parkedAt(PLATE_2, 2));
        repo().park(parkedAt(p3, 11));
        Parked leaving = parkedAt(p5, 4);
        repo().park(leaving);
        repo().close(leaving.leave(ENTRY.plusSeconds(60)));

        assertThat(repo().countParkedIn(A)).isEqualTo(2);
        assertThat(repo().countParkedIn(B)).isEqualTo(1);
    }
}
