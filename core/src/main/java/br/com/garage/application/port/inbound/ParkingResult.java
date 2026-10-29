package br.com.garage.application.port.inbound;

import br.com.garage.domain.garage.SectorCode;
import java.math.BigDecimal;

public sealed interface ParkingResult {

    record Accepted(SectorCode sector, BigDecimal priceFactor) implements ParkingResult {}

    record AlreadyParked() implements ParkingResult {}

    record ParkedElsewhere() implements ParkingResult {}

    record UnknownSpot() implements ParkingResult {}

    record SpotTaken() implements ParkingResult {}

    record NoActiveEntry() implements ParkingResult {}
}
