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
import java.util.Optional;

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
                    case Parked parked -> close(parked.leave(exitTime))
                            .<ExitResult>map(amount -> new Charged(parked.sector(), amount))
                            .orElseGet(NoActiveSession::new);
                    case Entered entered -> close(entered.leave(exitTime))
                            .<ExitResult>map(amount -> new LeftWithoutParking())
                            .orElseGet(NoActiveSession::new);
                    case Closed closed -> new NoActiveSession();
                })
                .orElseGet(NoActiveSession::new));
    }

    private Optional<Money> close(Closed closed) {
        if (!sessions.close(closed)) {
            return Optional.empty();
        }
        capacity.release();
        return Optional.of(closed.amount());
    }
}
