package br.com.garage.observability;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.Locale;

public class EventMetrics {

    private final MeterRegistry registry;

    public EventMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void record(String event, Object outcome) {
        registry.counter("garage.events", "event", event, "outcome", outcomeName(outcome))
                .increment();
    }

    static String outcomeName(Object outcome) {
        String raw = outcome instanceof Enum<?> e ? e.name() : outcome.getClass().getSimpleName();
        return raw.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }
}
