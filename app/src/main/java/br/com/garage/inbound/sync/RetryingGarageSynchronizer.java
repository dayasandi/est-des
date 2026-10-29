package br.com.garage.inbound.sync;

import br.com.garage.application.port.inbound.SyncGarage;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RetryingGarageSynchronizer {

    private static final Logger log = LoggerFactory.getLogger(RetryingGarageSynchronizer.class);

    @FunctionalInterface
    public interface Sleeper {
        void sleep(Duration duration) throws InterruptedException;
    }

    private final SyncGarage sync;
    private final GarageSyncState state;
    private final Sleeper sleeper;
    private final Duration initialDelay;
    private final Duration maxDelay;

    public RetryingGarageSynchronizer(
            SyncGarage sync, GarageSyncState state, Sleeper sleeper, Duration initialDelay, Duration maxDelay) {
        this.sync = sync;
        this.state = state;
        this.sleeper = sleeper;
        this.initialDelay = initialDelay;
        this.maxDelay = maxDelay;
    }

    public void run() {
        Duration delay = initialDelay;
        int attempt = 1;
        while (!state.isReady()) {
            try {
                sync.sync();
                state.markReady();
                log.atInfo().addKeyValue("attempts", attempt).log("garagem sincronizada");
            } catch (RuntimeException e) {
                log.atWarn()
                        .addKeyValue("attempt", attempt)
                        .addKeyValue("retry_in_ms", delay.toMillis())
                        .log("falha ao sincronizar a garagem: {}", e.toString());
                if (!pause(delay)) {
                    return;
                }
                delay = nextDelay(delay);
                attempt++;
            }
        }
    }

    private boolean pause(Duration delay) {
        try {
            sleeper.sleep(delay);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private Duration nextDelay(Duration current) {
        Duration doubled = current.multipliedBy(2);
        return doubled.compareTo(maxDelay) > 0 ? maxDelay : doubled;
    }
}
