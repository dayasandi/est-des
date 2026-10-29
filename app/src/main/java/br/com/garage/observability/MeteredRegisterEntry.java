package br.com.garage.observability;

import br.com.garage.application.port.inbound.EntryOutcome;
import br.com.garage.application.port.inbound.RegisterEntry;
import br.com.garage.domain.parking.LicensePlate;
import java.time.Instant;

public class MeteredRegisterEntry implements RegisterEntry {

    private final RegisterEntry delegate;
    private final EventMetrics metrics;

    public MeteredRegisterEntry(RegisterEntry delegate, EventMetrics metrics) {
        this.delegate = delegate;
        this.metrics = metrics;
    }

    @Override
    public EntryOutcome register(LicensePlate plate, Instant entryTime) {
        EntryOutcome outcome = delegate.register(plate, entryTime);
        metrics.record("entry", outcome);
        return outcome;
    }
}
