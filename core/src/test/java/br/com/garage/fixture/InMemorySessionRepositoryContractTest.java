package br.com.garage.fixture;

import br.com.garage.application.port.outbound.SessionRepository;
import br.com.garage.contract.SessionRepositoryContract;

class InMemorySessionRepositoryContractTest extends SessionRepositoryContract {

    private final InMemorySessionRepository repository = new InMemorySessionRepository();

    @Override
    protected SessionRepository repository() {
        return repository;
    }
}
