package br.com.garage.inbound.sync;

import java.util.concurrent.atomic.AtomicBoolean;

public class GarageSyncState {

    private final AtomicBoolean ready = new AtomicBoolean();

    public boolean isReady() {
        return ready.get();
    }

    void markReady() {
        ready.set(true);
    }
}
