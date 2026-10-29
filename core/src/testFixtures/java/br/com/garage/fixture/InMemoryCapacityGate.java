package br.com.garage.fixture;

import br.com.garage.application.port.outbound.CapacityGate;

public final class InMemoryCapacityGate implements CapacityGate {

    private int capacity;
    private int reserved;

    @Override
    public void configure(int totalCapacity) {
        this.capacity = totalCapacity;
    }

    @Override
    public boolean tryReserve() {
        if (reserved >= capacity) {
            return false;
        }
        reserved++;
        return true;
    }

    @Override
    public void release() {
        reserved = Math.max(0, reserved - 1);
    }

    public int reserved() {
        return reserved;
    }

    public int capacity() {
        return capacity;
    }
}
