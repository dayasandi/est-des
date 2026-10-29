package br.com.garage.outbound.persistence;

import br.com.garage.application.port.outbound.SectorRepository;
import br.com.garage.domain.garage.Sector;
import br.com.garage.domain.garage.SectorCode;
import br.com.garage.domain.shared.Money;
import java.util.Collection;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;

public class JdbcSectorRepository implements SectorRepository {

    private final JdbcClient jdbc;

    public JdbcSectorRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void saveAll(Collection<Sector> sectors) {
        sectors.forEach(sector -> jdbc.sql("""
                        INSERT INTO sector (code, base_price, max_capacity) VALUES (:code, :price, :capacity)
                        ON DUPLICATE KEY UPDATE base_price = VALUES(base_price), max_capacity = VALUES(max_capacity)
                        """)
                .param("code", sector.code().value())
                .param("price", sector.basePrice().amount())
                .param("capacity", sector.maxCapacity())
                .update());
    }

    @Override
    public Optional<Sector> findByCode(SectorCode code) {
        return jdbc.sql("SELECT code, base_price, max_capacity FROM sector WHERE code = :code")
                .param("code", code.value())
                .query((rs, row) -> new Sector(
                        new SectorCode(rs.getString("code")),
                        new Money(rs.getBigDecimal("base_price")),
                        rs.getInt("max_capacity")))
                .optional();
    }
}
