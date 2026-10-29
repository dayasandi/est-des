package br.com.garage.fixture;

import br.com.garage.application.port.outbound.RevenueReader;
import br.com.garage.domain.garage.SectorCode;
import br.com.garage.domain.shared.Money;
import java.time.Instant;
import java.util.function.Predicate;

public final class InMemoryRevenueReader implements RevenueReader {

    private final InMemorySessionRepository sessions;

    public InMemoryRevenueReader(InMemorySessionRepository sessions) {
        this.sessions = sessions;
    }

    @Override
    public Money total(Instant fromInclusive, Instant toExclusive) {
        return sum(entry -> inRange(entry, fromInclusive, toExclusive));
    }

    @Override
    public Money totalOf(SectorCode sector, Instant fromInclusive, Instant toExclusive) {
        return sum(entry -> inRange(entry, fromInclusive, toExclusive)
                && entry.sector().filter(sector::equals).isPresent());
    }

    private Money sum(Predicate<InMemorySessionRepository.ClosedEntry> filter) {
        return sessions.closedSessions().stream()
                .filter(filter)
                .map(entry -> entry.session().amount())
                .reduce(Money.ZERO, Money::plus);
    }

    private static boolean inRange(InMemorySessionRepository.ClosedEntry entry, Instant from, Instant to) {
        Instant exit = entry.session().exitTime();
        return !exit.isBefore(from) && exit.isBefore(to);
    }
}
