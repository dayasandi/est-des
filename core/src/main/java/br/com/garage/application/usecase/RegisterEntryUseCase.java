package br.com.garage.application.usecase;

import br.com.garage.application.port.inbound.EntryOutcome;
import br.com.garage.application.port.inbound.RegisterEntry;
import br.com.garage.application.port.outbound.CapacityGate;
import br.com.garage.application.port.outbound.SessionRepository;
import br.com.garage.application.port.outbound.TransactionRunner;
import br.com.garage.domain.parking.LicensePlate;
import br.com.garage.domain.parking.ParkingSession.Entered;
import java.time.Instant;

public final class RegisterEntryUseCase implements RegisterEntry {

    private final SessionRepository sessions;
    private final CapacityGate capacity;
    private final TransactionRunner transactions;

    public RegisterEntryUseCase(SessionRepository sessions, CapacityGate capacity, TransactionRunner transactions) {
        this.sessions = sessions;
        this.capacity = capacity;
        this.transactions = transactions;
    }

    @Override
    public EntryOutcome register(LicensePlate plate, Instant entryTime) {
        return transactions.inTransaction(() -> {
            if (sessions.findActive(plate).isPresent()) {
                return EntryOutcome.ALREADY_INSIDE;
            }
            if (!capacity.tryReserve()) {
                return EntryOutcome.GARAGE_FULL;
            }
            if (!sessions.add(new Entered(plate, entryTime))) {

                capacity.release();
                return EntryOutcome.ALREADY_INSIDE;
            }
            return EntryOutcome.ACCEPTED;
        });
    }
}
