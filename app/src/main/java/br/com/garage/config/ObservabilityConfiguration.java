package br.com.garage.config;

import br.com.garage.inbound.sync.GarageSyncState;
import br.com.garage.inbound.web.RequestLoggingFilter;
import br.com.garage.observability.CapacityMetrics;
import br.com.garage.observability.EventMetrics;
import br.com.garage.observability.GarageSyncHealthIndicator;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

@Configuration(proxyBeanMethods = false)
class ObservabilityConfiguration {

    @Bean
    EventMetrics eventMetrics(MeterRegistry registry) {
        return new EventMetrics(registry);
    }

    @Bean
    CapacityMetrics capacityMetrics(JdbcClient jdbc) {
        return new CapacityMetrics(jdbc);
    }

    @Bean
    GarageSyncHealthIndicator garageSyncHealthIndicator(GarageSyncState state) {
        return new GarageSyncHealthIndicator(state);
    }

    @Bean
    RequestLoggingFilter requestLoggingFilter() {
        return new RequestLoggingFilter();
    }
}
