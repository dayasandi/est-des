package br.com.garage.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.garage.application.port.inbound.SyncGarage;
import br.com.garage.application.port.outbound.GarageConfigProvider;
import br.com.garage.fixture.DirectTransactionRunner;
import br.com.garage.fixture.InMemoryCapacityGate;
import br.com.garage.fixture.InMemoryGarageRepositories;
import br.com.garage.fixture.TestGarage;
import org.junit.jupiter.api.Test;

class SyncGarageUseCaseTest {

    private final InMemoryGarageRepositories.Sectors sectors = new InMemoryGarageRepositories.Sectors();
    private final InMemoryGarageRepositories.Spots spots = new InMemoryGarageRepositories.Spots();
    private final InMemoryCapacityGate capacity = new InMemoryCapacityGate();

    private SyncGarage syncWith(GarageConfigProvider provider) {
        return new SyncGarageUseCase(provider, sectors, spots, capacity, new DirectTransactionRunner());
    }

    @Test
    void gravaSetoresVagasECapacidadeTotal() {
        syncWith(() -> TestGarage.CONFIG).sync();

        assertThat(sectors.size()).isEqualTo(2);
        assertThat(spots.size()).isEqualTo(30);
        assertThat(capacity.capacity()).isEqualTo(30);
    }

    @Test
    void sincronizarDeNovoEIdempotente() {
        SyncGarage sync = syncWith(() -> TestGarage.CONFIG);

        sync.sync();
        sync.sync();

        assertThat(sectors.size()).isEqualTo(2);
        assertThat(spots.size()).isEqualTo(30);
        assertThat(capacity.capacity()).isEqualTo(30);
    }

    @Test
    void falhaDaFonteNaoGravaNadaEPropagaOErro() {
        SyncGarage sync = syncWith(() -> {
            throw new IllegalStateException("fonte indisponivel");
        });

        assertThatThrownBy(sync::sync).hasMessage("fonte indisponivel");

        assertThat(sectors.size()).isZero();
        assertThat(spots.size()).isZero();
        assertThat(capacity.capacity()).isZero();
    }
}
