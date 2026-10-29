package br.com.garage.inbound.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.garage.domain.parking.LicensePlate;
import br.com.garage.domain.parking.ParkingSession.Entered;
import br.com.garage.fixture.TestGarage;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class RevenueContractTest {

    private final WebTestSupport app = new WebTestSupport(30);

    @BeforeEach
    void exitsOnTheDay() {
        chargedExit("AAA0001", 1, "2026-10-29T10:00:00Z", 1);
        chargedExit("BBB0002", 11, "2026-10-29T11:00:00Z", 2);
        chargedExit("CCC0003", 2, "2026-10-28T11:00:00Z", 1);
    }

    private void chargedExit(String plate, int spotId, String exitAt, long hours) {
        Instant exit = Instant.parse(exitAt);
        var entered = new Entered(new LicensePlate(plate), exit.minusSeconds(hours * 3600));
        app.sessions.add(entered);
        var spot = TestGarage.spot(spotId);
        var price = spot.sector().equals(TestGarage.A) ? TestGarage.SECTOR_A.basePrice() : TestGarage.SECTOR_B.basePrice();
        var parked = entered.parkAt(spot, price, BigDecimal.ONE);
        app.sessions.park(parked);
        app.sessions.close(parked.leave(exit));
    }

    private ResultActions query(String queryString) throws Exception {
        return app.mvc.perform(get("/revenue" + queryString));
    }

    @Test
    void semSetorSomaTodosOsSetoresDoDia() throws Exception {
        query("?date=2026-10-29")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(48.70))
                .andExpect(jsonPath("$.currency").value("BRL"));
    }

    @Test
    void comSetorFiltraPeloSetor() throws Exception {
        query("?date=2026-10-29&sector=A").andExpect(status().isOk()).andExpect(jsonPath("$.amount").value(40.50));
        query("?date=2026-10-29&sector=B").andExpect(status().isOk()).andExpect(jsonPath("$.amount").value(8.20));
    }

    @Test
    void aceitaOsParametrosNoCorpoJson() throws Exception {
        app.mvc.perform(get("/revenue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date":"2026-10-29","sector":"A"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(40.50));
    }

    @Test
    void queryStringTemPrioridadeSobreOCorpo() throws Exception {
        app.mvc.perform(get("/revenue?sector=B")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date":"2026-10-29","sector":"A"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(8.20));
    }

    @Test
    void semDataUsaOHojeEmUtc() throws Exception {
        query("").andExpect(status().isOk()).andExpect(jsonPath("$.amount").value(48.70));
    }

    @Test
    void timestampEOMomentoDaConsultaComMilissegundosEmUtc() throws Exception {
        query("?date=2026-10-29").andExpect(jsonPath("$.timestamp").value("2026-10-29T12:00:00.123Z"));
    }

    @Test
    void diaSemSaidasRetornaZero() throws Exception {
        query("?date=2026-01-01").andExpect(status().isOk()).andExpect(jsonPath("$.amount").value(0.0));
    }

    @Test
    void valorSaiComDuasCasasDecimais() throws Exception {
        query("?date=2026-10-29&sector=A").andExpect(content().string(org.hamcrest.Matchers.containsString("\"amount\":40.50")));
    }

    @Test
    void dataInvalidaRetorna400() throws Exception {
        query("?date=29/10/2026").andExpect(status().isBadRequest());
        query("?date=2026-13-45").andExpect(status().isBadRequest());
    }

    @Test
    void setorInexistenteRetorna404() throws Exception {
        query("?date=2026-10-29&sector=Z").andExpect(status().isNotFound());
    }

    @Test
    void setorComFormatoInvalidoRetorna400() throws Exception {
        query("?date=2026-10-29&sector=ABCDEFGHIJKL").andExpect(status().isBadRequest());
    }
}
