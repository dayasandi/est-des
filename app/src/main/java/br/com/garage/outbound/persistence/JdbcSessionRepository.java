package br.com.garage.outbound.persistence;

import br.com.garage.application.port.outbound.SessionRepository;
import br.com.garage.domain.garage.SectorCode;
import br.com.garage.domain.parking.LicensePlate;
import br.com.garage.domain.parking.ParkingSession;
import br.com.garage.domain.parking.ParkingSession.Closed;
import br.com.garage.domain.parking.ParkingSession.Entered;
import br.com.garage.domain.parking.ParkingSession.Parked;
import br.com.garage.domain.shared.Money;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;

public class JdbcSessionRepository implements SessionRepository {

    private final JdbcClient jdbc;

    public JdbcSessionRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<ParkingSession> findActive(LicensePlate plate) {
        return jdbc.sql("""
                        SELECT plate, status, entry_time, spot_id, sector_code, hourly_price, price_factor
                        FROM parking_session WHERE active_plate = :plate
                        """)
                .param("plate", plate.value())
                .query((rs, row) -> toSession(rs))
                .optional();
    }

    @Override
    public boolean add(Entered entered) {
        try {
            jdbc.sql("INSERT INTO parking_session (plate, status, entry_time) VALUES (:plate, 'ENTERED', :entry)")
                    .param("plate", entered.plate().value())
                    .param("entry", Timestamps.toDb(entered.entryTime()))
                    .update();
            return true;
        } catch (DuplicateKeyException alreadyActive) {
            return false;
        }
    }

    @Override
    public boolean park(Parked parked) {
        try {
            int updated = jdbc.sql("""
                            UPDATE parking_session
                            SET status = 'PARKED', spot_id = :spot, sector_code = :sector,
                                hourly_price = :price, price_factor = :factor
                            WHERE active_plate = :plate AND status = 'ENTERED'
                            """)
                    .param("spot", parked.spotId())
                    .param("sector", parked.sector().value())
                    .param("price", parked.hourlyPrice().amount())
                    .param("factor", parked.priceFactor())
                    .param("plate", parked.plate().value())
                    .update();
            return updated == 1 || isParkedAt(parked);
        } catch (DuplicateKeyException spotTaken) {
            return false;
        }
    }

    private boolean isParkedAt(Parked parked) {
        return jdbc.sql("SELECT COUNT(*) FROM parking_session WHERE active_plate = :plate AND occupied_spot = :spot")
                        .param("plate", parked.plate().value())
                        .param("spot", parked.spotId())
                        .query(Integer.class)
                        .single()
                > 0;
    }

    @Override
    public boolean close(Closed closed) {
        return jdbc.sql("""
                        UPDATE parking_session SET status = 'CLOSED', exit_time = :exit, amount = :amount
                        WHERE active_plate = :plate
                        """)
                .param("exit", Timestamps.toDb(closed.exitTime()))
                .param("amount", closed.amount().amount())
                .param("plate", closed.plate().value())
                .update()
                == 1;
    }

    @Override
    public int countParkedIn(SectorCode sector) {
        return jdbc.sql("SELECT COUNT(*) FROM parking_session WHERE status = 'PARKED' AND sector_code = :sector")
                .param("sector", sector.value())
                .query(Integer.class)
                .single();
    }

    private static ParkingSession toSession(ResultSet rs) throws SQLException {
        LicensePlate plate = new LicensePlate(rs.getString("plate"));
        var entryTime = Timestamps.fromDb(rs, "entry_time");
        if ("ENTERED".equals(rs.getString("status"))) {
            return new Entered(plate, entryTime);
        }
        return new Parked(
                plate,
                entryTime,
                rs.getLong("spot_id"),
                new SectorCode(rs.getString("sector_code")),
                new Money(rs.getBigDecimal("hourly_price")),
                rs.getBigDecimal("price_factor"));
    }
}
