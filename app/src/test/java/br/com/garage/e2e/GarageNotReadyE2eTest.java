package br.com.garage.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import br.com.garage.application.port.outbound.GarageConfigProvider;
import br.com.garage.fixture.TestGarage;
import br.com.garage.inbound.sync.GarageSyncState;
import br.com.garage.testsupport.SharedMySql;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(GarageNotReadyE2eTest.FlakySimulator.class)
class GarageNotReadyE2eTest {

    static final AtomicInteger CALLS = new AtomicInteger();

    @TestConfiguration(proxyBeanMethods = false)
    static class FlakySimulator {

        @Bean
        @Primary
        GarageConfigProvider flaky() {
            return () -> {
                if (CALLS.incrementAndGet() <= 2) {
                    throw new IllegalStateException("simulador ainda subindo");
                }
                return TestGarage.CONFIG;
            };
        }
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", SharedMySql::jdbcUrl);
        registry.add("spring.datasource.username", SharedMySql::username);
        registry.add("spring.datasource.password", SharedMySql::password);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private GarageSyncState state;

    private HttpResponse<String> call(String method, String path, String body) throws Exception {
        var publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body);
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .method(method, publisher)
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void recusaComServiceUnavailableEnquantoNaoCarregouEDepoisSincronizaSozinha() throws Exception {
        String entry = """
                {"license_plate":"NR00001","entry_time":"2026-10-29T02:00:00","event_type":"ENTRY"}""";

        HttpResponse<String> refused = call("POST", "/webhook", entry);
        assertThat(refused.statusCode()).isEqualTo(503);
        assertThat(refused.headers().firstValue("Retry-After")).contains("1");
        assertThat(call("GET", "/actuator/health/readiness", null).statusCode()).isEqualTo(503);

        await().atMost(Duration.ofSeconds(20)).until(state::isReady);

        assertThat(CALLS.get()).isGreaterThanOrEqualTo(3);
        assertThat(call("GET", "/actuator/health/readiness", null).statusCode()).isEqualTo(200);
        assertThat(call("POST", "/webhook", entry).statusCode()).isIn(200);
    }
}
