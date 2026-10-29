package br.com.garage.application.port.outbound;

import br.com.garage.domain.garage.SectorCode;
import br.com.garage.domain.parking.LicensePlate;
import br.com.garage.domain.parking.ParkingSession;
import java.util.Optional;

public interface SessionRepository {

    Optional<ParkingSession> findActive(LicensePlate plate);

    boolean add(ParkingSession.Entered entered);

    boolean park(ParkingSession.Parked parked);

    void close(ParkingSession.Closed closed);

    int countParkedIn(SectorCode sector);
}
