package br.com.garage.application.port.outbound;

import br.com.garage.domain.garage.Coordinates;
import br.com.garage.domain.garage.Spot;
import java.util.Collection;
import java.util.Optional;

public interface SpotRepository {

    void saveAll(Collection<Spot> spots);

    Optional<Spot> findByLocation(Coordinates location);
}
