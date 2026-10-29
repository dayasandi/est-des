package br.com.garage.config;

import br.com.garage.application.port.inbound.SyncGarage;
import br.com.garage.application.port.outbound.GarageConfigProvider;
import br.com.garage.inbound.sync.GarageSyncStarter;
import br.com.garage.inbound.sync.GarageSyncState;
import br.com.garage.inbound.sync.RetryingGarageSynchronizer;
import br.com.garage.outbound.simulator.SimulatorGarageClient;
import br.com.garage.outbound.simulator.SimulatorProperties;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SimulatorProperties.class)
class SyncConfiguration {

    @Bean
    GarageSyncState garageSyncState() {
        return new GarageSyncState();
    }

    @Bean
    @ConditionalOnMissingBean(GarageConfigProvider.class)
    GarageConfigProvider simulatorGarageClient(SimulatorProperties properties) {
        return new SimulatorGarageClient(properties);
    }

    @Bean
    RetryingGarageSynchronizer garageSynchronizer(SyncGarage sync, GarageSyncState state) {
        return new RetryingGarageSynchronizer(
                sync, state, duration -> Thread.sleep(duration), Duration.ofMillis(500), Duration.ofSeconds(10));
    }

    @Bean
    @ConditionalOnProperty(name = "garage.sync.enabled", havingValue = "true", matchIfMissing = true)
    GarageSyncStarter garageSyncStarter(RetryingGarageSynchronizer synchronizer) {
        return new GarageSyncStarter(synchronizer);
    }
}
