package br.com.garage.application.port.outbound;

import br.com.garage.domain.garage.SectorCode;
import br.com.garage.domain.shared.Money;
import java.time.Instant;

public interface RevenueReader {

    Money total(Instant fromInclusive, Instant toExclusive);

    Money totalOf(SectorCode sector, Instant fromInclusive, Instant toExclusive);
}
