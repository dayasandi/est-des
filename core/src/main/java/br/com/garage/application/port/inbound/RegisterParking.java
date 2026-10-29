package br.com.garage.application.port.inbound;

import br.com.garage.domain.garage.Coordinates;
import br.com.garage.domain.parking.LicensePlate;

@FunctionalInterface
public interface RegisterParking {

    ParkingResult register(LicensePlate plate, Coordinates location);
}
