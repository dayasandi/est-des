package br.com.garage.fixture;

import br.com.garage.application.port.outbound.SectorRepository;
import br.com.garage.application.port.outbound.SpotRepository;
import br.com.garage.domain.garage.Coordinates;
import br.com.garage.domain.garage.Sector;
import br.com.garage.domain.garage.SectorCode;
import br.com.garage.domain.garage.Spot;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class InMemoryGarageRepositories {

    public static final class Sectors implements SectorRepository {

        private final Map<SectorCode, Sector> byCode = new HashMap<>();

        @Override
        public void saveAll(Collection<Sector> sectors) {
            sectors.forEach(sector -> byCode.put(sector.code(), sector));
        }

        @Override
        public Optional<Sector> findByCode(SectorCode code) {
            return Optional.ofNullable(byCode.get(code));
        }

        public int size() {
            return byCode.size();
        }
    }

    public static final class Spots implements SpotRepository {

        private final Map<Coordinates, Spot> byLocation = new HashMap<>();

        @Override
        public void saveAll(Collection<Spot> spots) {
            spots.forEach(spot -> byLocation.put(spot.location(), spot));
        }

        @Override
        public Optional<Spot> findByLocation(Coordinates location) {
            return Optional.ofNullable(byLocation.get(location));
        }

        public int size() {
            return byLocation.size();
        }
    }

    private InMemoryGarageRepositories() {}

    public static Sectors sectorsOfTestGarage() {
        Sectors sectors = new Sectors();
        sectors.saveAll(TestGarage.CONFIG.sectors());
        return sectors;
    }

    public static Spots spotsOfTestGarage() {
        Spots spots = new Spots();
        spots.saveAll(TestGarage.CONFIG.spots());
        return spots;
    }
}
