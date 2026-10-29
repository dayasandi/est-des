package br.com.garage.application.usecase;

import br.com.garage.application.port.inbound.ExitResult;
import br.com.garage.application.port.inbound.ExitResult.Charged;
import br.com.garage.application.port.inbound.ExitResult.LeftWithoutParking;
import br.com.garage.application.port.inbound.ExitResult.NoActiveSession;
import br.com.garage.application.port.inbound.RegisterExit;
import br.com.garage.application.port.outbound.CapacityGate;
import br.com.garage.application.port.outbound.SessionRepository;
import br.com.garage.application.port.outbound.TransactionRunner;
import br.com.garage.domain.parking.LicensePlate;
import br.com.garage.domain.parking.ParkingSession.Closed;
import br.com.garage.domain.parking.ParkingSession.Entered;
import br.com.garage.domain.parking.ParkingSession.Parked;
import br.com.garage.domain.shared.Money;
import java.time.Instant;

public final class RegisterExitUseCase implements RegisterExit {

    private final SessionRepository sessions;
    private final CapacityGate capacity;
    private final TransactionRunner transactions;

    public RegisterExitUseCase(SessionRepository sessions, CapacityGate capacity, TransactionRunner transactions) {
        this.sessions = sessions;
        this.capacity = capacity;
        this.transactions = transactions;
    }

    @Override
    public ExitResult register(LicensePlate plate, Instant exitTime) {
        return transactions.inTransaction(() -> sessions.findActive(plate)
                .<ExitResult>map(session -> switch (session) {
                    case Parked parked -> new Charged(parked.sector(), close(parked.leave(exitTime)));
                    case Entered entered -> {
                        close(entered.leave(exitTime));
                        yield new LeftWithoutParking();
                    }
                    case Closed closed -> new NoActiveSession();
                })
                .orElseGet(NoActiveSession::new));
    }

    private Money close(Closed closed) {
        sessions.close(closed);
        capacity.release();
        return closed.amount();
    }
}
