package br.com.garage.config;

import br.com.garage.inbound.ratelimit.RateLimitFilter;
import br.com.garage.inbound.ratelimit.RateLimitProperties;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RateLimitProperties.class)
class RateLimitConfiguration {

    @Bean
    @ConditionalOnProperty(name = "garage.rate-limit.enabled", havingValue = "true", matchIfMissing = true)
    RateLimitFilter rateLimitFilter(RateLimitProperties properties, MeterRegistry metrics, JsonMapper mapper) {
        return new RateLimitFilter(properties, metrics, mapper);
    }
}
