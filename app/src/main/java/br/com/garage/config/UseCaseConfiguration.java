package br.com.garage.config;

import br.com.garage.application.port.inbound.GetRevenue;
import br.com.garage.application.port.inbound.RegisterEntry;
import br.com.garage.application.port.inbound.RegisterExit;
import br.com.garage.application.port.inbound.RegisterParking;
import br.com.garage.application.port.inbound.SyncGarage;
import br.com.garage.application.port.outbound.CapacityGate;
import br.com.garage.application.port.outbound.GarageConfigProvider;
import br.com.garage.application.port.outbound.RevenueReader;
import br.com.garage.application.port.outbound.SectorRepository;
import br.com.garage.application.port.outbound.SessionRepository;
import br.com.garage.application.port.outbound.SpotRepository;
import br.com.garage.application.port.outbound.TransactionRunner;
import br.com.garage.application.usecase.GetRevenueUseCase;
import br.com.garage.application.usecase.RegisterEntryUseCase;
import br.com.garage.application.usecase.RegisterExitUseCase;
import br.com.garage.application.usecase.RegisterParkingUseCase;
import br.com.garage.application.usecase.SyncGarageUseCase;
import br.com.garage.domain.pricing.PriceTable;
import br.com.garage.domain.pricing.PricingPolicy;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class UseCaseConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    PricingPolicy pricingPolicy() {
        return PriceTable.standard();
    }

    @Bean
    RegisterEntry registerEntry(SessionRepository sessions, CapacityGate capacity, TransactionRunner tx) {
        return new RegisterEntryUseCase(sessions, capacity, tx);
    }

    @Bean
    RegisterParking registerParking(
            SessionRepository sessions,
            SpotRepository spots,
            SectorRepository sectors,
            PricingPolicy pricing,
            TransactionRunner tx) {
        return new RegisterParkingUseCase(sessions, spots, sectors, pricing, tx);
    }

    @Bean
    RegisterExit registerExit(SessionRepository sessions, CapacityGate capacity, TransactionRunner tx) {
        return new RegisterExitUseCase(sessions, capacity, tx);
    }

    @Bean
    GetRevenue getRevenue(RevenueReader revenue, SectorRepository sectors) {
        return new GetRevenueUseCase(revenue, sectors);
    }

    @Bean
    SyncGarage syncGarage(
            GarageConfigProvider provider,
            SectorRepository sectors,
            SpotRepository spots,
            CapacityGate capacity,
            TransactionRunner tx) {
        return new SyncGarageUseCase(provider, sectors, spots, capacity, tx);
    }
}
