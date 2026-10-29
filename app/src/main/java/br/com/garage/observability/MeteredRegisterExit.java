package br.com.garage.observability;

import br.com.garage.application.port.inbound.ExitResult;
import br.com.garage.application.port.inbound.RegisterExit;
import br.com.garage.domain.parking.LicensePlate;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;

public class MeteredRegisterExit implements RegisterExit {

    private final RegisterExit delegate;
    private final EventMetrics metrics;
    private final DistributionSummary charged;

    public MeteredRegisterExit(RegisterExit delegate, EventMetrics metrics, MeterRegistry registry) {
        this.delegate = delegate;
        this.metrics = metrics;
        this.charged = DistributionSummary.builder("garage.exit.charged")
                .baseUnit("BRL")
                .description("Valor cobrado por saida")
                .register(registry);
    }

    @Override
    public ExitResult register(LicensePlate plate, Instant exitTime) {
        ExitResult result = delegate.register(plate, exitTime);
        metrics.record("exit", result);
        if (result instanceof ExitResult.Charged c) {
            charged.record(c.amount().amount().doubleValue());
        }
        return result;
    }
}
