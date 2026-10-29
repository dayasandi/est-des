package br.com.garage.config;

import br.com.garage.application.port.outbound.CapacityGate;
import br.com.garage.application.port.outbound.RevenueReader;
import br.com.garage.application.port.outbound.SectorRepository;
import br.com.garage.application.port.outbound.SessionRepository;
import br.com.garage.application.port.outbound.SpotRepository;
import br.com.garage.application.port.outbound.TransactionRunner;
import br.com.garage.outbound.persistence.JdbcCapacityGate;
import br.com.garage.outbound.persistence.JdbcRevenueReader;
import br.com.garage.outbound.persistence.JdbcSectorRepository;
import br.com.garage.outbound.persistence.JdbcSessionRepository;
import br.com.garage.outbound.persistence.JdbcSpotRepository;
import br.com.garage.outbound.persistence.SpringTransactionRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration(proxyBeanMethods = false)
class PersistenceConfiguration {

    @Bean
    SectorRepository sectorRepository(JdbcClient jdbc) {
        return new JdbcSectorRepository(jdbc);
    }

    @Bean
    SpotRepository spotRepository(JdbcClient jdbc) {
        return new JdbcSpotRepository(jdbc);
    }

    @Bean
    SessionRepository sessionRepository(JdbcClient jdbc) {
        return new JdbcSessionRepository(jdbc);
    }

    @Bean
    CapacityGate capacityGate(JdbcClient jdbc) {
        return new JdbcCapacityGate(jdbc);
    }

    @Bean
    RevenueReader revenueReader(JdbcClient jdbc) {
        return new JdbcRevenueReader(jdbc);
    }

    @Bean
    TransactionRunner transactionRunner(TransactionTemplate template) {
        return new SpringTransactionRunner(template);
    }
}
