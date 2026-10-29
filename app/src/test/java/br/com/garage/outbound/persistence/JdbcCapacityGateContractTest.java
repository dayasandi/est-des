package br.com.garage.outbound.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.garage.application.port.outbound.CapacityGate;
import br.com.garage.contract.CapacityGateContract;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JdbcCapacityGateContractTest extends CapacityGateContract {

    private final JdbcCapacityGate gate = new JdbcCapacityGate(MySqlSupport.jdbc());

    @BeforeEach
    void freshGarage() {
        MySqlSupport.resetWithTestGarage();
    }

    @Override
    protected CapacityGate gate() {
        return gate;
    }

    @Test
    void reservasSimultaneasNuncaPassamDaCapacidade() throws Exception {
        gate.configure(5);
        List<Callable<Boolean>> tasks = IntStream.range(0, 50)
                .<Callable<Boolean>>mapToObj(n -> gate::tryReserve)
                .toList();

        List<Boolean> results = new ArrayList<>();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (Future<Boolean> future : executor.invokeAll(tasks)) {
                results.add(future.get());
            }
        }

        assertThat(results).filteredOn(Boolean::booleanValue).hasSize(5);
    }
}
