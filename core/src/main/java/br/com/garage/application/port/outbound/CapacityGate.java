package br.com.garage.application.port.outbound;

public interface CapacityGate {

    void configure(int totalCapacity);

    boolean tryReserve();

    void release();
}
