package br.com.garage.outbound.persistence;

import br.com.garage.application.port.outbound.RevenueReader;
import br.com.garage.domain.garage.SectorCode;
import br.com.garage.domain.shared.Money;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.jdbc.core.simple.JdbcClient;

public class JdbcRevenueReader implements RevenueReader {

    private static final String BASE = """
            SELECT COALESCE(SUM(amount), 0) FROM parking_session
            WHERE status = 'CLOSED' AND exit_time >= :from AND exit_time < :to
            """;

    private final JdbcClient jdbc;

    public JdbcRevenueReader(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Money total(Instant fromInclusive, Instant toExclusive) {
        return sum(jdbc.sql(BASE), fromInclusive, toExclusive);
    }

    @Override
    public Money totalOf(SectorCode sector, Instant fromInclusive, Instant toExclusive) {
        var statement = jdbc.sql(BASE + " AND sector_code = :sector").param("sector", sector.value());
        return sum(statement, fromInclusive, toExclusive);
    }

    private static Money sum(JdbcClient.StatementSpec statement, Instant from, Instant to) {
        BigDecimal total = statement
                .param("from", Timestamps.toDb(from))
                .param("to", Timestamps.toDb(to))
                .query(BigDecimal.class)
                .single();
        return new Money(total);
    }
}
