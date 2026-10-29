package br.com.garage.domain.pricing;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.garage.domain.shared.Money;
import java.math.BigDecimal;
import java.time.Duration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ParkingFeeTest {

    private static final Money HOURLY = Money.of("40.50");

    @ParameterizedTest(name = "{0}s de permanencia -> R$ {1}")
    @CsvSource({
        "-60,0.00",
        "0,0.00",
        "1799,0.00",
        "1800,0.00",
        "1801,40.50",
        "3600,40.50",
        "3601,81.00",
        "5400,81.00",
        "7200,81.00",
        "7201,121.50",
        "86400,972.00"
    })
    void cobraHoraCheiaDepoisDosTrintaMinutosGratis(long seconds, String expected) {
        Money fee = ParkingFee.charge(Duration.ofSeconds(seconds), HOURLY, BigDecimal.ONE);

        assertThat(fee).isEqualTo(Money.of(expected));
    }

    @ParameterizedTest(name = "{0}ms de permanencia -> R$ {1}")
    @CsvSource({"1800000,0.00", "1800001,40.50", "3600000,40.50", "3600001,81.00"})
    void fracaoDeSegundoTambemContaParaArredondarParaCima(long millis, String expected) {
        Money fee = ParkingFee.charge(Duration.ofMillis(millis), HOURLY, BigDecimal.ONE);

        assertThat(fee).isEqualTo(Money.of(expected));
    }

    @ParameterizedTest(name = "R$ {0} x {1} por 1h -> R$ {2}")
    @CsvSource({
        "40.50,0.90,36.45",
        "40.50,1.00,40.50",
        "40.50,1.10,44.55",
        "40.50,1.25,50.63",
        "4.10,0.90,3.69",
        "4.10,1.10,4.51",
        "4.10,1.25,5.13"
    })
    void aplicaOFatorDePrecoEArredondaOsCentavosParaCima(String hourly, String factor, String expected) {
        Money fee = ParkingFee.charge(Duration.ofHours(1), Money.of(hourly), new BigDecimal(factor));

        assertThat(fee).isEqualTo(Money.of(expected));
    }

    @ParameterizedTest(name = "{0}h com fator {1} -> R$ {2}")
    @CsvSource({"2,1.10,89.10", "3,0.90,109.35", "24,1.25,1215.00"})
    void multiplicaAsHorasAntesDeArredondar(long hours, String factor, String expected) {
        Money fee = ParkingFee.charge(Duration.ofHours(hours), HOURLY, new BigDecimal(factor));

        assertThat(fee).isEqualTo(Money.of(expected));
    }
}
