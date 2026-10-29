package br.com.garage.domain.pricing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.garage.domain.garage.Occupancy;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PriceTableTest {

    private final PriceTable table = PriceTable.standard();

    @ParameterizedTest(name = "{0}/{1} vagas -> fator {2}")
    @CsvSource({

        "0,100,0.90", "24,100,0.90", "25,100,1.00", "49,100,1.00",
        "50,100,1.10", "74,100,1.10", "75,100,1.25", "99,100,1.25", "100,100,1.25",

        "0,10,0.90", "2,10,0.90", "3,10,1.00", "4,10,1.00",
        "5,10,1.10", "7,10,1.10", "8,10,1.25", "10,10,1.25",

        "4,20,0.90", "5,20,1.00", "9,20,1.00", "10,20,1.10", "14,20,1.10", "15,20,1.25"
    })
    void escolheAFaixaPeloPercentualDeLotacao(int occupied, int capacity, String expectedFactor) {
        BigDecimal factor = table.factorFor(new Occupancy(occupied, capacity));

        assertThat(factor).isEqualByComparingTo(expectedFactor);
    }

    @Test
    void tetoDeCadaFaixaEExclusivo() {

        assertThat(table.factorFor(new Occupancy(25, 100))).isEqualByComparingTo("1.00");
        assertThat(table.factorFor(new Occupancy(2499, 10_000))).isEqualByComparingTo("0.90");
    }

    @Test
    void naoAceitaTabelaVazia() {
        assertThatThrownBy(() -> new PriceTable(List.of())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void exigeTetosEstritamenteCrescentes() {
        List<PriceBand> bands = List.of(
                new PriceBand(50, BigDecimal.ONE),
                new PriceBand(50, BigDecimal.TEN),
                new PriceBand(PriceBand.OPEN_ENDED, BigDecimal.TWO));

        assertThatThrownBy(() -> new PriceTable(bands)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void exigeUltimaFaixaAbertaParaCobrir100PorCento() {
        List<PriceBand> bands = List.of(new PriceBand(50, BigDecimal.ONE), new PriceBand(100, BigDecimal.TEN));

        assertThatThrownBy(() -> new PriceTable(bands))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("aberta");
    }

    @Test
    void faixaRejeitaFatorNaoPositivo() {
        assertThatThrownBy(() -> new PriceBand(25, BigDecimal.ZERO)).isInstanceOf(IllegalArgumentException.class);
    }
}
