package br.com.garage.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MoneyTest {

    @ParameterizedTest
    @CsvSource({"40.5,40.50", "40.501,40.51", "40.509,40.51", "0.001,0.01", "0,0.00", "10,10.00"})
    void normalizaParaDuasCasasArredondandoParaCima(String input, String expected) {
        assertThat(Money.of(input).amount()).isEqualTo(new BigDecimal(expected));
    }

    @Test
    void valoresIguaisSaoIguaisIndependenteDaEscalaInformada() {
        assertThat(Money.of("10")).isEqualTo(Money.of("10.00")).hasSameHashCodeAs(Money.of("10.0"));
    }

    @Test
    void rejeitaValorNegativo() {
        assertThatThrownBy(() -> Money.of("-0.01")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void somaValores() {
        assertThat(Money.of("40.50").plus(Money.of("4.10"))).isEqualTo(Money.of("44.60"));
    }

    @Test
    void multiplicaPeloFatorExatoEArredondaSoNoFim() {

        assertThat(Money.of("40.50").scaledBy(new BigDecimal("3.75"))).isEqualTo(Money.of("151.88"));
    }
}
