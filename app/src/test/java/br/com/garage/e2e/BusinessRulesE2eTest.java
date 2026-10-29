package br.com.garage.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class BusinessRulesE2eTest extends E2eSupport {

    @Test
    void precoDinamicoMudaDeFaixaConformeOSetorEncheEFicaTravadoNaEntrada() {
        List<BigDecimal> charged = new ArrayList<>();
        for (int n = 1; n <= 10; n++) {
            String plate = "CAR%04d".formatted(n);
            assertThat(enter(plate, "2026-10-29T02:00:00").status()).isEqualTo(200);
            assertThat(park(plate, n).status()).isEqualTo(200);
        }

        for (int n = 1; n <= 10; n++) {
            Reply reply = leave("CAR%04d".formatted(n), "2026-10-29T03:30:00");
            charged.add(reply.body().get("amount").decimalValue());
        }

        assertThat(charged)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(
                        bd("72.90"), bd("72.90"), bd("72.90"),
                        bd("81.00"), bd("81.00"),
                        bd("89.10"), bd("89.10"), bd("89.10"),
                        bd("101.25"), bd("101.25"));
        assertThat(reserved()).isZero();
    }

    @Test
    void faixaDeUmVeiculoNaoMudaQuandoOSetorEnchePorDepois() {
        enter("EARLY01", "2026-10-29T02:00:00");
        park("EARLY01", 1);
        for (int n = 2; n <= 10; n++) {
            String plate = "LATE%03d".formatted(n);
            enter(plate, "2026-10-29T02:00:00");
            park(plate, n);
        }

        Reply early = leave("EARLY01", "2026-10-29T03:00:00");

        assertThat(early.body().get("amount").decimalValue()).isEqualByComparingTo("36.45");
    }

    @Test
    void lotacaoDoSetorBUsaACapacidadeDoSetorB() {

        for (int n = 1; n <= 5; n++) {
            enter("B%05d".formatted(n), "2026-10-29T02:00:00");
            park("B%05d".formatted(n), 10 + n);
        }
        enter("B00006", "2026-10-29T02:00:00");
        park("B00006", 16);

        Reply fifth = leave("B00005", "2026-10-29T03:00:00");
        Reply sixth = leave("B00006", "2026-10-29T03:00:00");

        assertThat(fifth.body().get("amount").decimalValue()).isEqualByComparingTo("3.69");
        assertThat(sixth.body().get("amount").decimalValue()).isEqualByComparingTo("4.10");
    }

    @Test
    void primeirosTrintaMinutosSaoGratisEDepoisCobraHoraCheiaIncluindoAPrimeira() {
        record Stay(String plate, int spot, String exit, String expected) {}
        List<Stay> stays = List.of(
                new Stay("T00030", 11, "2026-10-29T02:30:00", "0.00"),
                new Stay("T00031", 12, "2026-10-29T02:31:00", "3.69"),
                new Stay("T00060", 13, "2026-10-29T03:00:00", "3.69"),
                new Stay("T00061", 14, "2026-10-29T03:01:00", "7.38"));
        stays.forEach(s -> {
            enter(s.plate(), "2026-10-29T02:00:00");
            park(s.plate(), s.spot());
        });

        stays.forEach(s -> assertThat(leave(s.plate(), s.exit()).body().get("amount").decimalValue())
                .as("%s", s.plate())
                .isEqualByComparingTo(s.expected()));
    }

    @Test
    void garagemCheiaRecusaNovasEntradasAteUmaSaida() {
        for (int n = 1; n <= 30; n++) {
            assertThat(enter("FULL%03d".formatted(n), "2026-10-29T02:00:00").status()).isEqualTo(200);
        }

        Reply refused = enter("FULL031", "2026-10-29T02:00:00");

        assertThat(refused.status()).isEqualTo(409);
        assertThat(reserved()).isEqualTo(30);
        assertThat(jdbc.sql("SELECT COUNT(*) FROM parking_session WHERE plate = 'FULL031'")
                        .query(Integer.class)
                        .single())
                .isZero();

        leave("FULL001", "2026-10-29T02:10:00");

        assertThat(enter("FULL031", "2026-10-29T02:11:00").status()).isEqualTo(200);
        assertThat(reserved()).isEqualTo(30);
    }

    @Test
    void eventosRepetidosSaoIdempotentes() {
        assertThat(enter("DUP0001", "2026-10-29T02:00:00").status()).isEqualTo(200);
        assertThat(enter("DUP0001", "2026-10-29T02:00:05").body().get("status").asString()).isEqualTo("ALREADY_INSIDE");
        assertThat(park("DUP0001", 3).status()).isEqualTo(200);
        assertThat(park("DUP0001", 3).body().get("status").asString()).isEqualTo("ALREADY_PARKED");
        assertThat(reserved()).isEqualTo(1);

        assertThat(leave("DUP0001", "2026-10-29T03:00:00").status()).isEqualTo(200);
        Reply second = leave("DUP0001", "2026-10-29T03:00:00");

        assertThat(second.status()).isEqualTo(200);
        assertThat(second.body().get("status").asString()).isEqualTo("NO_ACTIVE_SESSION");
        assertThat(reserved()).isZero();
        assertThat(get("/revenue?date=2026-10-29").body().get("amount").decimalValue()).isEqualByComparingTo("36.45");
    }

    @Test
    void placaPodeVoltarAEntrarDepoisDeSair() {
        enter("AGAIN01", "2026-10-29T02:00:00");
        park("AGAIN01", 1);
        leave("AGAIN01", "2026-10-29T03:00:00");

        assertThat(enter("AGAIN01", "2026-10-29T05:00:00").status()).isEqualTo(200);
        assertThat(park("AGAIN01", 1).status()).isEqualTo(200);
    }

    @Test
    void receitaSomaSoSaidasDoDiaUtcPorSetorEOTotal() {
        chargedStay("REV0001", 1, "2026-10-29T10:00:00", "2026-10-29T11:00:00");
        chargedStay("REV0002", 11, "2026-10-29T10:00:00", "2026-10-29T12:00:00");
        chargedStay("REV0003", 2, "2026-10-29T22:30:00", "2026-10-30T00:30:00");
        enter("REV0004", "2026-10-29T10:00:00");

        assertThat(revenue("2026-10-29", null)).isEqualByComparingTo("43.83");
        assertThat(revenue("2026-10-29", "A")).isEqualByComparingTo("36.45");
        assertThat(revenue("2026-10-29", "B")).isEqualByComparingTo("7.38");
        assertThat(revenue("2026-10-30", null)).isEqualByComparingTo("72.90");
    }

    @Test
    void respostaDaReceitaTemMoedaETimestampDaConsulta() {
        Reply reply = get("/revenue?date=2026-10-29");

        assertThat(reply.status()).isEqualTo(200);
        assertThat(reply.body().get("currency").asString()).isEqualTo("BRL");
        assertThat(reply.body().get("timestamp").asString()).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z");
        assertThat(reply.body().get("amount").decimalValue()).isEqualByComparingTo("0");
    }

    @Test
    void erroDeRotaEntradaInvalidaSaoRejeitadosSemEfeito() {
        assertThat(get("/revenue?date=hoje").status()).isEqualTo(400);
        assertThat(get("/revenue?date=2026-10-29&sector=Z").status()).isEqualTo(404);
        assertThat(post("/webhook", "{isto nao e json").status()).isEqualTo(400);
        assertThat(post("/webhook", """
                {"license_plate":"X1","event_type":"OUTRO"}""").status()).isEqualTo(400);
        assertThat(park("NOENTRY1", 1).status()).isEqualTo(409);
        enter("X1", "2026-10-29T02:00:00");
        assertThat(post("/webhook", """
                {"license_plate":"X1","lat":1.0,"lng":1.0,"event_type":"PARKED"}""").status()).isEqualTo(422);
        assertThat(reserved()).isEqualTo(1);
    }

    private void chargedStay(String plate, int spot, String entry, String exit) {
        enter(plate, entry);
        park(plate, spot);
        leave(plate, exit);
    }

    private BigDecimal revenue(String date, String sector) {
        String query = "/revenue?date=" + date + (sector == null ? "" : "&sector=" + sector);
        return get(query).body().get("amount").decimalValue();
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
