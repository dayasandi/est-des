package br.com.garage.application.port.inbound;

import br.com.garage.domain.garage.SectorCode;
import java.time.LocalDate;
import java.util.Optional;

@FunctionalInterface
public interface GetRevenue {

    RevenueResult revenueOf(LocalDate day, Optional<SectorCode> sector);
}
