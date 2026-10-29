package br.com.garage.inbound.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "garage.rate-limit")
public record RateLimitProperties(@DefaultValue("true") boolean enabled, Rule webhook, Rule revenue) {

    public RateLimitProperties {
        webhook = webhook == null ? new Rule(200, 100) : webhook;
        revenue = revenue == null ? new Rule(30, 10) : revenue;
    }

    public record Rule(int burst, int refillPerSecond) {

        public Rule {
            if (burst <= 0 || refillPerSecond <= 0) {
                throw new IllegalArgumentException("burst e refillPerSecond devem ser positivos");
            }
        }
    }
}
