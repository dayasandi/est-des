package br.com.garage.fixture;

import br.com.garage.application.port.outbound.SessionRepository;
import br.com.garage.domain.garage.SectorCode;
import br.com.garage.domain.parking.LicensePlate;
import br.com.garage.domain.parking.ParkingSession;
import br.com.garage.domain.parking.ParkingSession.Closed;
import br.com.garage.domain.parking.ParkingSession.Entered;
import br.com.garage.domain.parking.ParkingSession.Parked;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemorySessionRepository implements SessionRepository {

    public record ClosedEntry(Closed session, Optional<SectorCode> sector) {}

    private final Map<LicensePlate, ParkingSession> active = new LinkedHashMap<>();
    private final List<ClosedEntry> closed = new ArrayList<>();

    @Override
    public Optional<ParkingSession> findActive(LicensePlate plate) {
        return Optional.ofNullable(active.get(plate));
    }

    @Override
    public boolean add(Entered entered) {
        return active.putIfAbsent(entered.plate(), entered) == null;
    }

    @Override
    public boolean park(Parked parked) {
        boolean spotTaken = active.values().stream()
                .anyMatch(other -> other instanceof Parked p
                        && p.spotId() == parked.spotId()
                        && !p.plate().equals(parked.plate()));
        if (spotTaken) {
            return false;
        }
        active.put(parked.plate(), parked);
        return true;
    }

    @Override
    public void close(Closed session) {
        Optional<SectorCode> sector = Optional.ofNullable(active.remove(session.plate()))
                .filter(Parked.class::isInstance)
                .map(previous -> ((Parked) previous).sector());
        closed.add(new ClosedEntry(session, sector));
    }

    @Override
    public int countParkedIn(SectorCode sector) {
        return (int) active.values().stream()
                .filter(session -> session instanceof Parked p && p.sector().equals(sector))
                .count();
    }

    public List<ClosedEntry> closedSessions() {
        return List.copyOf(closed);
    }
}
