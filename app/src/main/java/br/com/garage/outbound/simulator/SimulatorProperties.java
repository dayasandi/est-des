package br.com.garage.outbound.simulator;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "garage.simulator")
public record SimulatorProperties(
        @DefaultValue("http://localhost:3000") URI url,
        @DefaultValue("2s") Duration connectTimeout,
        @DefaultValue("5s") Duration readTimeout) {}
