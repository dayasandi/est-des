package br.com.garage.fixture;

import br.com.garage.application.port.outbound.CapacityGate;
import br.com.garage.contract.CapacityGateContract;

class InMemoryCapacityGateContractTest extends CapacityGateContract {

    private final InMemoryCapacityGate gate = new InMemoryCapacityGate();

    @Override
    protected CapacityGate gate() {
        return gate;
    }
}
