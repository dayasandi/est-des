package br.com.garage.observability;

import br.com.garage.application.port.inbound.ParkingResult;
import br.com.garage.application.port.inbound.RegisterParking;
import br.com.garage.domain.garage.Coordinates;
import br.com.garage.domain.parking.LicensePlate;

public class MeteredRegisterParking implements RegisterParking {

    private final RegisterParking delegate;
    private final EventMetrics metrics;

    public MeteredRegisterParking(RegisterParking delegate, EventMetrics metrics) {
        this.delegate = delegate;
        this.metrics = metrics;
    }

    @Override
    public ParkingResult register(LicensePlate plate, Coordinates location) {
        ParkingResult result = delegate.register(plate, location);
        metrics.record("parked", result);
        return result;
    }
}
