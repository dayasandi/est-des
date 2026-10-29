package br.com.garage.inbound.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.garage.domain.parking.LicensePlate;
import br.com.garage.domain.parking.ParkingSession.Entered;
import br.com.garage.domain.parking.ParkingSession.Parked;
import br.com.garage.fixture.TestGarage;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class WebhookContractTest {

    private final WebTestSupport app = new WebTestSupport(30);

    private ResultActions send(String json) throws Exception {
        return app.mvc.perform(post("/webhook").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String entry(String plate, String time) {
        return """
                {"license_plate":"%s","entry_time":"%s","event_type":"ENTRY"}""".formatted(plate, time);
    }

    private static String parked(String plate, int spotId) {
        var location = TestGarage.location(spotId);
        return """
                {"license_plate":"%s","lat":%s,"lng":%s,"event_type":"PARKED"}"""
                .formatted(plate, location.lat().toPlainString(), location.lng().toPlainString());
    }

    private static String exit(String plate, String time) {
        return """
                {"license_plate":"%s","exit_time":"%s","event_type":"EXIT"}""".formatted(plate, time);
    }

    @Test
    void entradaAceitaGravaAPermanenciaLendoHorarioSemFusoComoUtc() throws Exception {
        send(entry("ABC1234", "2026-10-29T02:09:09")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACCEPTED"));

        assertThat(app.sessions.findActive(new LicensePlate("ABC1234")))
                .contains(new Entered(new LicensePlate("ABC1234"), Instant.parse("2026-10-29T02:09:09Z")));
    }

    @Test
    void entradaComFusoExplicitoRespeitaOFuso() throws Exception {
        send(entry("ABC1234", "2026-10-29T02:09:09-03:00")).andExpect(status().isOk());

        assertThat(app.sessions.findActive(new LicensePlate("ABC1234")))
                .contains(new Entered(new LicensePlate("ABC1234"), Instant.parse("2026-10-29T05:09:09Z")));
    }

    @Test
    void entradaRepetidaEIdempotenteERetorna200() throws Exception {
        send(entry("ABC1234", "2026-10-29T02:09:09")).andExpect(status().isOk());

        send(entry("ABC1234", "2026-10-29T02:10:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ALREADY_INSIDE"));

        assertThat(app.capacity.reserved()).isEqualTo(1);
    }

    @Test
    void entradaComGaragemCheiaRetorna409SemGravar() throws Exception {
        WebTestSupport tiny = new WebTestSupport(1);
        tiny.mvc.perform(post("/webhook").contentType(MediaType.APPLICATION_JSON).content(entry("AAA0001", "2026-10-29T02:09:09")))
                .andExpect(status().isOk());

        tiny.mvc.perform(post("/webhook").contentType(MediaType.APPLICATION_JSON).content(entry("BBB0002", "2026-10-29T02:09:10")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("garagem cheia"));

        assertThat(tiny.sessions.findActive(new LicensePlate("BBB0002"))).isEmpty();
    }

    @Test
    void estacionamentoAceitoDevolveOSetor() throws Exception {
        send(entry("ABC1234", "2026-10-29T02:09:09"));

        send(parked("ABC1234", 12))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.sector").value("B"));

        assertThat(app.sessions.findActive(new LicensePlate("ABC1234"))).get().isInstanceOf(Parked.class);
    }

    @Test
    void estacionamentoRepetidoNaMesmaVagaRetorna200() throws Exception {
        send(entry("ABC1234", "2026-10-29T02:09:09"));
        send(parked("ABC1234", 12));

        send(parked("ABC1234", 12)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ALREADY_PARKED"));
    }

    @Test
    void coordenadaSemVagaRetorna422() throws Exception {
        send(entry("ABC1234", "2026-10-29T02:09:09"));

        send("""
                {"license_plate":"ABC1234","lat":-10.0,"lng":-10.0,"event_type":"PARKED"}""")
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void estacionarSemEntradaRetorna409() throws Exception {
        send(parked("ABC1234", 12)).andExpect(status().isConflict());
    }

    @Test
    void vagaOcupadaPorOutroVeiculoRetorna409() throws Exception {
        send(entry("AAA0001", "2026-10-29T02:09:09"));
        send(entry("BBB0002", "2026-10-29T02:09:10"));
        send(parked("AAA0001", 5));

        send(parked("BBB0002", 5)).andExpect(status().isConflict());
    }

    @Test
    void veiculoJaEstacionadoEmOutraVagaRetorna409() throws Exception {
        send(entry("AAA0001", "2026-10-29T02:09:09"));
        send(parked("AAA0001", 5));

        send(parked("AAA0001", 6)).andExpect(status().isConflict());
    }

    @Test
    void saidaCobraEDevolveOValor() throws Exception {
        send(entry("ABC1234", "2026-10-29T02:00:00"));
        send(parked("ABC1234", 1));

        send(exit("ABC1234", "2026-10-29T03:30:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHARGED"))
                .andExpect(jsonPath("$.sector").value("A"))
                .andExpect(jsonPath("$.amount").value(72.90));

        assertThat(app.capacity.reserved()).isZero();
    }

    @Test
    void saidaRepetidaOuDePlacaDesconhecidaRetorna200SemCobrar() throws Exception {
        send(exit("NEVER01", "2026-10-29T03:30:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NO_ACTIVE_SESSION"));
    }

    @Test
    void saidaSemTerEstacionadoLiberaAVagaSemTarifa() throws Exception {
        send(entry("ABC1234", "2026-10-29T02:00:00"));

        send(exit("ABC1234", "2026-10-29T05:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LEFT_WITHOUT_PARKING"));

        assertThat(app.capacity.reserved()).isZero();
    }

    @Test
    void jsonMalformadoRetorna400() throws Exception {
        send("{nao e json").andExpect(status().isBadRequest());
    }

    @Test
    void eventTypeDesconhecidoRetorna400() throws Exception {
        send("""
                {"license_plate":"ABC1234","event_type":"TELEPORT"}""").andExpect(status().isBadRequest());
    }

    @Test
    void eventTypeAusenteRetorna400() throws Exception {
        send("""
                {"license_plate":"ABC1234","entry_time":"2026-10-29T02:09:09"}""").andExpect(status().isBadRequest());
    }

    @Test
    void placaAusenteRetorna400() throws Exception {
        send("""
                {"entry_time":"2026-10-29T02:09:09","event_type":"ENTRY"}""").andExpect(status().isBadRequest());
    }

    @Test
    void placaComFormatoInvalidoRetorna400() throws Exception {
        send(entry("PLACA INVALIDA!", "2026-10-29T02:09:09")).andExpect(status().isBadRequest());
    }

    @Test
    void horarioAusenteOuInvalidoRetorna400() throws Exception {
        send("""
                {"license_plate":"ABC1234","event_type":"ENTRY"}""").andExpect(status().isBadRequest());
        send(entry("ABC1234", "ontem")).andExpect(status().isBadRequest());
        send(exit("ABC1234", "")).andExpect(status().isBadRequest());
    }

    @Test
    void coordenadaAusenteOuForaDoIntervaloRetorna400() throws Exception {
        send("""
                {"license_plate":"ABC1234","event_type":"PARKED"}""").andExpect(status().isBadRequest());
        send("""
                {"license_plate":"ABC1234","lat":123.0,"lng":0.0,"event_type":"PARKED"}""").andExpect(status().isBadRequest());
    }

    @Test
    void camposExtrasNoEventoSaoIgnorados() throws Exception {
        send("""
                {"license_plate":"ABC1234","entry_time":"2026-10-29T02:09:09","event_type":"ENTRY","extra":42}""")
                .andExpect(status().isOk());
    }

    @Test
    void recusasUsamProblemDetailComStatusEDetalhe() throws Exception {
        send(parked("ABC1234", 12))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("veiculo sem entrada ativa"));
    }
}
