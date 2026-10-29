package br.com.garage.e2e;

import static org.awaitility.Awaitility.await;

import br.com.garage.application.port.outbound.GarageConfigProvider;
import br.com.garage.fixture.TestGarage;
import br.com.garage.inbound.sync.GarageSyncState;
import br.com.garage.testsupport.SharedMySql;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "garage.rate-limit.webhook.burst=100000",
            "garage.rate-limit.webhook.refill-per-second=100000",
            "garage.rate-limit.revenue.burst=100000",
            "garage.rate-limit.revenue.refill-per-second=100000"
        })
@Import(E2eSupport.FixedGarage.class)
abstract class E2eSupport {

    record Reply(int status, JsonNode body, HttpResponse<String> raw) {

        String header(String name) {
            return raw.headers().firstValue(name).orElse(null);
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedGarage {

        @Bean
        @Primary
        GarageConfigProvider fixedGarage() {
            return () -> TestGarage.CONFIG;
        }
    }

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @LocalServerPort
    private int port;

    @Autowired
    protected JdbcClient jdbc;

    @Autowired
    private GarageSyncState syncState;

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", SharedMySql::jdbcUrl);
        registry.add("spring.datasource.username", SharedMySql::username);
        registry.add("spring.datasource.password", SharedMySql::password);
    }

    @BeforeEach
    void cleanState() {
        await().atMost(Duration.ofSeconds(20)).until(syncState::isReady);
        jdbc.sql("DELETE FROM parking_session").update();
        jdbc.sql("UPDATE capacity_counter SET reserved = 0").update();
    }

    protected Reply post(String path, String json) {
        return send(HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build());
    }

    protected Reply postWithRequestId(String path, String json, String requestId) {
        return send(HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .header("X-Request-Id", requestId)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build());
    }

    protected Reply get(String pathAndQuery) {
        return send(HttpRequest.newBuilder(uri(pathAndQuery)).GET().build());
    }

    protected String raw(String pathAndQuery) {
        return get(pathAndQuery).raw().body();
    }

    private URI uri(String pathAndQuery) {
        return URI.create("http://localhost:" + port + pathAndQuery);
    }

    private Reply send(HttpRequest request) {
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            JsonNode json = body == null || body.isBlank() || !body.trim().startsWith("{")
                    ? JSON.createObjectNode()
                    : JSON.readTree(body);
            return new Reply(response.statusCode(), json, response);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    protected Reply enter(String plate, String time) {
        return post("/webhook", """
                {"license_plate":"%s","entry_time":"%s","event_type":"ENTRY"}""".formatted(plate, time));
    }

    protected Reply park(String plate, int spotId) {
        var location = TestGarage.location(spotId);
        return post("/webhook", """
                {"license_plate":"%s","lat":%s,"lng":%s,"event_type":"PARKED"}"""
                .formatted(plate, location.lat().toPlainString(), location.lng().toPlainString()));
    }

    protected Reply leave(String plate, String time) {
        return post("/webhook", """
                {"license_plate":"%s","exit_time":"%s","event_type":"EXIT"}""".formatted(plate, time));
    }

    protected int reserved() {
        return jdbc.sql("SELECT reserved FROM capacity_counter WHERE id = 1").query(Integer.class).single();
    }
}
