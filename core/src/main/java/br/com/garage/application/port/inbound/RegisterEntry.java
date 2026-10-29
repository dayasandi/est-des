package br.com.garage.application.port.inbound;

import br.com.garage.domain.parking.LicensePlate;
import java.time.Instant;

@FunctionalInterface
public interface RegisterEntry {

    EntryOutcome register(LicensePlate plate, Instant entryTime);
}
