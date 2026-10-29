package br.com.garage.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(
        properties = {
            "garage.rate-limit.webhook.burst=5",
            "garage.rate-limit.webhook.refill-per-second=1",
            "garage.rate-limit.revenue.burst=3",
            "garage.rate-limit.revenue.refill-per-second=1"
        })
class RateLimitE2eTest extends E2eSupport {

    @Test
    void excedenteDoWebhookRecebe429ComRetryAfterEMetrica() {
        List<Reply> replies = IntStream.range(0, 12)
                .mapToObj(n -> enter("RL%05d".formatted(n), "2026-10-29T02:00:00"))
                .toList();

        long rejected = replies.stream().filter(r -> r.status() == 429).count();
        Reply sample = replies.stream().filter(r -> r.status() == 429).findFirst().orElseThrow();

        assertThat(rejected).isGreaterThanOrEqualTo(5);
        assertThat(replies.stream().filter(r -> r.status() == 200).count()).isBetween(5L, 7L);
        assertThat(Integer.parseInt(sample.header("Retry-After"))).isGreaterThanOrEqualTo(1);
        assertThat(sample.body().get("detail").asString()).isEqualTo("limite de requisicoes excedido");
        assertThat(raw("/actuator/prometheus"))
                .contains("garage_rate_limit_rejected_total{")
                .contains("policy=\"webhook\"");
    }

    @Test
    void receitaTemLimiteProprioEOsEndpointsDeOperacaoNaoSaoLimitados() {
        List<Reply> revenue =
                IntStream.range(0, 8).mapToObj(n -> get("/revenue?date=2026-10-29")).toList();

        assertThat(revenue.stream().filter(r -> r.status() == 429).count()).isGreaterThanOrEqualTo(4);
        for (int i = 0; i < 30; i++) {
            assertThat(get("/actuator/health/liveness").status()).isEqualTo(200);
        }
    }
}
