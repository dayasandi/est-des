package br.com.garage.outbound.persistence;

import br.com.garage.application.port.outbound.CapacityGate;
import org.springframework.jdbc.core.simple.JdbcClient;

public class JdbcCapacityGate implements CapacityGate {

    private final JdbcClient jdbc;

    public JdbcCapacityGate(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void configure(int totalCapacity) {
        jdbc.sql("""
                        INSERT INTO capacity_counter (id, total, reserved) VALUES (1, :total, 0)
                        ON DUPLICATE KEY UPDATE total = VALUES(total)
                        """)
                .param("total", totalCapacity)
                .update();
    }

    @Override
    public boolean tryReserve() {
        return jdbc.sql("UPDATE capacity_counter SET reserved = reserved + 1 WHERE id = 1 AND reserved < total")
                        .update()
                == 1;
    }

    @Override
    public void release() {
        jdbc.sql("UPDATE capacity_counter SET reserved = reserved - 1 WHERE id = 1 AND reserved > 0")
                .update();
    }
}
