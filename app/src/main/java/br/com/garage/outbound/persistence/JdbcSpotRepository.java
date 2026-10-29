package br.com.garage.outbound.persistence;

import br.com.garage.application.port.outbound.SpotRepository;
import br.com.garage.domain.garage.Coordinates;
import br.com.garage.domain.garage.SectorCode;
import br.com.garage.domain.garage.Spot;
import java.util.Collection;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;

public class JdbcSpotRepository implements SpotRepository {

    private final JdbcClient jdbc;

    public JdbcSpotRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void saveAll(Collection<Spot> spots) {
        spots.forEach(spot -> jdbc.sql("""
                        INSERT INTO spot (id, sector_code, lat, lng) VALUES (:id, :sector, :lat, :lng)
                        ON DUPLICATE KEY UPDATE sector_code = VALUES(sector_code), lat = VALUES(lat), lng = VALUES(lng)
                        """)
                .param("id", spot.id())
                .param("sector", spot.sector().value())
                .param("lat", spot.location().lat())
                .param("lng", spot.location().lng())
                .update());
    }

    @Override
    public Optional<Spot> findByLocation(Coordinates location) {
        return jdbc.sql("SELECT id, sector_code, lat, lng FROM spot WHERE lat = :lat AND lng = :lng")
                .param("lat", location.lat())
                .param("lng", location.lng())
                .query((rs, row) -> new Spot(
                        rs.getLong("id"),
                        new SectorCode(rs.getString("sector_code")),
                        new Coordinates(rs.getBigDecimal("lat"), rs.getBigDecimal("lng"))))
                .optional();
    }
}
