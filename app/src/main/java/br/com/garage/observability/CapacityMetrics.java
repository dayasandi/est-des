package br.com.garage.observability;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.jdbc.core.simple.JdbcClient;

public class CapacityMetrics implements MeterBinder {

    private final JdbcClient jdbc;

    public CapacityMetrics(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        Gauge.builder("garage.capacity", () -> read("total"))
                .tag("state", "total")
                .description("Vagas totais da garagem")
                .register(registry);
        Gauge.builder("garage.capacity", () -> read("reserved"))
                .tag("state", "reserved")
                .description("Vagas reservadas (veiculos dentro da garagem)")
                .register(registry);
    }

    private double read(String column) {
        try {
            return jdbc.sql("SELECT " + column + " FROM capacity_counter WHERE id = 1")
                    .query(Integer.class)
                    .optional()
                    .map(Integer::doubleValue)
                    .orElse(0.0);
        } catch (RuntimeException e) {
            return Double.NaN;
        }
    }
}
