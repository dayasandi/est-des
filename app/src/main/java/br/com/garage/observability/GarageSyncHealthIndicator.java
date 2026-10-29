package br.com.garage.observability;

import br.com.garage.inbound.sync.GarageSyncState;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

public class GarageSyncHealthIndicator implements HealthIndicator {

    private final GarageSyncState state;

    public GarageSyncHealthIndicator(GarageSyncState state) {
        this.state = state;
    }

    @Override
    public Health health() {
        return state.isReady()
                ? Health.up().build()
                : Health.outOfService().withDetail("reason", "garagem ainda nao carregada").build();
    }
}
