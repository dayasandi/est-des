package br.com.garage.application.usecase;

import br.com.garage.application.port.inbound.ParkingResult;
import br.com.garage.application.port.inbound.ParkingResult.Accepted;
import br.com.garage.application.port.inbound.ParkingResult.AlreadyParked;
import br.com.garage.application.port.inbound.ParkingResult.NoActiveEntry;
import br.com.garage.application.port.inbound.ParkingResult.ParkedElsewhere;
import br.com.garage.application.port.inbound.ParkingResult.SpotTaken;
import br.com.garage.application.port.inbound.ParkingResult.UnknownSpot;
import br.com.garage.application.port.inbound.RegisterParking;
import br.com.garage.application.port.outbound.SectorRepository;
import br.com.garage.application.port.outbound.SessionRepository;
import br.com.garage.application.port.outbound.SpotRepository;
import br.com.garage.application.port.outbound.TransactionRunner;
import br.com.garage.domain.garage.Coordinates;
import br.com.garage.domain.garage.Occupancy;
import br.com.garage.domain.garage.Sector;
import br.com.garage.domain.garage.Spot;
import br.com.garage.domain.parking.LicensePlate;
import br.com.garage.domain.parking.ParkingSession;
import br.com.garage.domain.parking.ParkingSession.Closed;
import br.com.garage.domain.parking.ParkingSession.Entered;
import br.com.garage.domain.parking.ParkingSession.Parked;
import br.com.garage.domain.pricing.PricingPolicy;
import java.math.BigDecimal;

public final class RegisterParkingUseCase implements RegisterParking {

    private final SessionRepository sessions;
    private final SpotRepository spots;
    private final SectorRepository sectors;
    private final PricingPolicy pricing;
    private final TransactionRunner transactions;

    public RegisterParkingUseCase(
            SessionRepository sessions,
            SpotRepository spots,
            SectorRepository sectors,
            PricingPolicy pricing,
            TransactionRunner transactions) {
        this.sessions = sessions;
        this.spots = spots;
        this.sectors = sectors;
        this.pricing = pricing;
        this.transactions = transactions;
    }

    @Override
    public ParkingResult register(LicensePlate plate, Coordinates location) {
        return transactions.inTransaction(() -> spots.findByLocation(location)
                .<ParkingResult>map(spot -> registerAt(plate, spot))
                .orElseGet(UnknownSpot::new));
    }

    private ParkingResult registerAt(LicensePlate plate, Spot spot) {
        return sessions.findActive(plate)
                .<ParkingResult>map(session -> switch (session) {
                    case Entered entered -> park(entered, spot);
                    case Parked parked -> parked.spotId() == spot.id() ? new AlreadyParked() : new ParkedElsewhere();
                    case Closed closed -> new NoActiveEntry();
                })
                .orElseGet(NoActiveEntry::new);
    }

    private ParkingResult park(Entered entered, Spot spot) {
        Sector sector = sectors.findByCode(spot.sector())
                .orElseThrow(() -> new IllegalStateException("setor da vaga nao cadastrado: " + spot.sector()));

        Occupancy occupancy = new Occupancy(sessions.countParkedIn(sector.code()), sector.maxCapacity());
        BigDecimal factor = pricing.factorFor(occupancy);

        ParkingSession.Parked parked = entered.parkAt(spot, sector.basePrice(), factor);
        return sessions.park(parked) ? new Accepted(sector.code(), factor) : new SpotTaken();
    }
}
