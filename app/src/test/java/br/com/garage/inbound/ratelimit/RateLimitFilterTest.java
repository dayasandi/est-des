package br.com.garage.inbound.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bucket4j.TimeMeter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

class RateLimitFilterTest {

    private static final class ManualTime implements TimeMeter {
        private long nanos;

        void advance(Duration duration) {
            nanos += duration.toNanos();
        }

        @Override
        public long currentTimeNanos() {
            return nanos;
        }

        @Override
        public boolean isWallClockBased() {
            return false;
        }
    }

    private final ManualTime time = new ManualTime();
    private final SimpleMeterRegistry metrics = new SimpleMeterRegistry();
    private final RateLimitProperties properties = new RateLimitProperties(
            true, new RateLimitProperties.Rule(3, 1), new RateLimitProperties.Rule(1, 1));
    private final RateLimitFilter filter =
            new RateLimitFilter(properties, metrics, JsonMapper.builder().build(), time);

    private MockHttpServletResponse call(String path, String client) throws Exception {
        var request = new MockHttpServletRequest("POST", path);
        request.setRemoteAddr(client);
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    @Test
    void deixaPassarAteOLimiteDoBaldeERecusaComTooManyRequests() throws Exception {
        assertThat(call("/webhook", "10.0.0.1").getStatus()).isEqualTo(200);
        assertThat(call("/webhook", "10.0.0.1").getStatus()).isEqualTo(200);
        assertThat(call("/webhook", "10.0.0.1").getStatus()).isEqualTo(200);

        MockHttpServletResponse rejected = call("/webhook", "10.0.0.1");

        assertThat(rejected.getStatus()).isEqualTo(429);
        assertThat(rejected.getHeader("Retry-After")).isEqualTo("1");
        assertThat(rejected.getContentType()).startsWith("application/problem+json");
        assertThat(rejected.getContentAsString()).contains("limite de requisicoes excedido");
    }

    @Test
    void cadaClienteTemOSeuProprioBalde() throws Exception {
        for (int i = 0; i < 3; i++) {
            call("/webhook", "10.0.0.1");
        }

        assertThat(call("/webhook", "10.0.0.1").getStatus()).isEqualTo(429);
        assertThat(call("/webhook", "10.0.0.2").getStatus()).isEqualTo(200);
    }

    @Test
    void cadaRotaTemOSeuProprioLimite() throws Exception {
        assertThat(call("/revenue", "10.0.0.1").getStatus()).isEqualTo(200);
        assertThat(call("/revenue", "10.0.0.1").getStatus()).isEqualTo(429);
        assertThat(call("/webhook", "10.0.0.1").getStatus()).isEqualTo(200);
    }

    @Test
    void reabasteceNaTaxaConfigurada() throws Exception {
        for (int i = 0; i < 3; i++) {
            call("/webhook", "10.0.0.1");
        }
        assertThat(call("/webhook", "10.0.0.1").getStatus()).isEqualTo(429);

        time.advance(Duration.ofSeconds(1));

        assertThat(call("/webhook", "10.0.0.1").getStatus()).isEqualTo(200);
        assertThat(call("/webhook", "10.0.0.1").getStatus()).isEqualTo(429);
    }

    @Test
    void informaOsTokensRestantes() throws Exception {
        assertThat(call("/webhook", "10.0.0.1").getHeader("X-RateLimit-Remaining")).isEqualTo("2");
        assertThat(call("/webhook", "10.0.0.1").getHeader("X-RateLimit-Remaining")).isEqualTo("1");
    }

    @Test
    void contaAsRecusasEmMetrica() throws Exception {
        for (int i = 0; i < 5; i++) {
            call("/webhook", "10.0.0.1");
        }

        assertThat(metrics.counter("garage.rate_limit.rejected", "policy", "webhook").count())
                .isEqualTo(2.0);
    }

    @Test
    void naoLimitaRotasForaDaApi() throws Exception {
        for (int i = 0; i < 50; i++) {
            assertThat(call("/actuator/health", "10.0.0.1").getStatus()).isEqualTo(200);
        }
    }
}
