package br.com.garage.domain.garage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CoordinatesTest {

    @Test
    void coordenadasComMesmoValorDecimalSaoIguaisIndependenteDaEscala() {
        Coordinates a = new Coordinates(new BigDecimal("-23.561684"), new BigDecimal("-46.655981"));
        Coordinates b = new Coordinates(new BigDecimal("-23.5616840"), new BigDecimal("-46.65598100"));

        assertThat(a).isEqualTo(b);
    }

    @Test
    void coordenadasDiferentesNaSextaCasaNaoSaoIguais() {
        Coordinates a = new Coordinates(new BigDecimal("-23.561684"), new BigDecimal("-46.655981"));
        Coordinates b = new Coordinates(new BigDecimal("-23.561685"), new BigDecimal("-46.655981"));

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void rejeitaValoresForaDoIntervaloGeografico() {
        assertThatThrownBy(() -> new Coordinates(new BigDecimal("90.1"), BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Coordinates(BigDecimal.ZERO, new BigDecimal("-180.1")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
