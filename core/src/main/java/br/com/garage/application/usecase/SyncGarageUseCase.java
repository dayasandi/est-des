package br.com.garage.application.usecase;

import br.com.garage.application.port.inbound.SyncGarage;
import br.com.garage.application.port.outbound.CapacityGate;
import br.com.garage.application.port.outbound.GarageConfigProvider;
import br.com.garage.application.port.outbound.SectorRepository;
import br.com.garage.application.port.outbound.SpotRepository;
import br.com.garage.application.port.outbound.TransactionRunner;
import br.com.garage.domain.garage.GarageConfig;

public final class SyncGarageUseCase implements SyncGarage {

    private final GarageConfigProvider provider;
    private final SectorRepository sectors;
    private final SpotRepository spots;
    private final CapacityGate capacity;
    private final TransactionRunner transactions;

    public SyncGarageUseCase(
            GarageConfigProvider provider,
            SectorRepository sectors,
            SpotRepository spots,
            CapacityGate capacity,
            TransactionRunner transactions) {
        this.provider = provider;
        this.sectors = sectors;
        this.spots = spots;
        this.capacity = capacity;
        this.transactions = transactions;
    }

    @Override
    public void sync() {

        GarageConfig config = provider.fetch();

        transactions.inTransaction(() -> {
            sectors.saveAll(config.sectors());
            spots.saveAll(config.spots());
            capacity.configure(config.totalCapacity());
            return null;
        });
    }
}
