package br.com.garage.fixture;

import br.com.garage.domain.garage.Coordinates;
import br.com.garage.domain.garage.GarageConfig;
import br.com.garage.domain.garage.Sector;
import br.com.garage.domain.garage.SectorCode;
import br.com.garage.domain.garage.Spot;
import br.com.garage.domain.shared.Money;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;

public final class TestGarage {

    public static final SectorCode A = new SectorCode("A");
    public static final SectorCode B = new SectorCode("B");

    public static final Sector SECTOR_A = new Sector(A, Money.of("40.50"), 10);
    public static final Sector SECTOR_B = new Sector(B, Money.of("4.10"), 20);

    public static final List<Spot> SPOTS = IntStream.rangeClosed(1, 30)
            .mapToObj(id -> new Spot(id, id <= 10 ? A : B, location(id)))
            .toList();

    public static final GarageConfig CONFIG = new GarageConfig(List.of(SECTOR_A, SECTOR_B), SPOTS);

    private TestGarage() {}

    public static Spot spot(int id) {
        return SPOTS.get(id - 1);
    }

    public static Coordinates location(int spotId) {
        BigDecimal step = new BigDecimal("0.00002").multiply(BigDecimal.valueOf(spotId - 1L));
        return new Coordinates(new BigDecimal("-23.561684").add(step), new BigDecimal("-46.655981").add(step));
    }
}
