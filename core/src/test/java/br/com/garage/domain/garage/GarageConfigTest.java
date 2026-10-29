package br.com.garage.domain.garage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.garage.domain.shared.Money;
import br.com.garage.fixture.TestGarage;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class GarageConfigTest {

    private static final Sector SECTOR_A = new Sector(new SectorCode("A"), Money.of("10"), 2);

    private static Spot spot(long id, String sector, String lat) {
        return new Spot(id, new SectorCode(sector), new Coordinates(new BigDecimal(lat), new BigDecimal("-46.0")));
    }

    @Test
    void capacidadeTotalESomaDasCapacidadesDosSetores() {
        assertThat(TestGarage.CONFIG.totalCapacity()).isEqualTo(30);
    }

    @Test
    void rejeitaSetorDuplicado() {
        assertThatThrownBy(() -> new GarageConfig(List.of(SECTOR_A, SECTOR_A), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("setor duplicado");
    }

    @Test
    void rejeitaVagaDuplicada() {
        List<Spot> spots = List.of(spot(1, "A", "-23.1"), spot(1, "A", "-23.2"));

        assertThatThrownBy(() -> new GarageConfig(List.of(SECTOR_A), spots))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vaga duplicada");
    }

    @Test
    void rejeitaVagaDeSetorDesconhecido() {
        assertThatThrownBy(() -> new GarageConfig(List.of(SECTOR_A), List.of(spot(1, "Z", "-23.1"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("setor desconhecido");
    }

    @Test
    void rejeitaCapacidadeNaoPositivaNoSetor() {
        assertThatThrownBy(() -> new Sector(new SectorCode("A"), Money.of("10"), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
