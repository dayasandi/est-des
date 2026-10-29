package br.com.garage.domain.garage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class OccupancyTest {

    @ParameterizedTest(name = "{0}/{1} abaixo de {2}% = {3}")
    @CsvSource({
        "24,100,25,true", "25,100,25,false", "26,100,25,false",
        "2,10,25,true", "3,10,25,false",
        "1,3,34,true", "1,3,33,false"
    })
    void comparaEstritamenteSemArredondarPercentual(int occupied, int capacity, int percent, boolean expected) {
        assertThat(new Occupancy(occupied, capacity).isBelowPercent(percent)).isEqualTo(expected);
    }

    @Test
    void estaCheiaQuandoOcupadasAtingemACapacidade() {
        assertThat(new Occupancy(9, 10).isFull()).isFalse();
        assertThat(new Occupancy(10, 10).isFull()).isTrue();
    }

    @Test
    void naoSofreOverflowComTetoAberto() {
        assertThat(new Occupancy(2_000_000_000, 2_100_000_000).isBelowPercent(Integer.MAX_VALUE))
                .isTrue();
    }

    @Test
    void rejeitaCapacidadeNaoPositivaEOcupacaoNegativa() {
        assertThatThrownBy(() -> new Occupancy(0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Occupancy(-1, 10)).isInstanceOf(IllegalArgumentException.class);
    }
}
