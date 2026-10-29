package br.com.garage.application.port.outbound;

import br.com.garage.domain.garage.Sector;
import br.com.garage.domain.garage.SectorCode;
import java.util.Collection;
import java.util.Optional;

public interface SectorRepository {

    void saveAll(Collection<Sector> sectors);

    Optional<Sector> findByCode(SectorCode code);
}
