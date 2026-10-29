package br.com.garage.application.port.inbound;

import br.com.garage.domain.parking.LicensePlate;
import java.time.Instant;

@FunctionalInterface
public interface RegisterExit {

    ExitResult register(LicensePlate plate, Instant exitTime);
}
