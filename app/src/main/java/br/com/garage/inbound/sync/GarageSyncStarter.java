package br.com.garage.inbound.sync;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

public class GarageSyncStarter implements ApplicationListener<ApplicationReadyEvent> {

    private final RetryingGarageSynchronizer synchronizer;

    public GarageSyncStarter(RetryingGarageSynchronizer synchronizer) {
        this.synchronizer = synchronizer;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        Thread.ofVirtual().name("garage-sync").start(synchronizer::run);
    }
}
