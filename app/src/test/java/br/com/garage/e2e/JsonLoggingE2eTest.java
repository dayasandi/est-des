package br.com.garage.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Tag("json-logging")
@ActiveProfiles("json")
@ExtendWith(OutputCaptureExtension.class)
class JsonLoggingE2eTest extends E2eSupport {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private List<JsonNode> jsonLogLines(CapturedOutput output) {
        return Arrays.stream(output.getAll().split("\\R"))
                .filter(line -> line.startsWith("{"))
                .map(JSON::readTree)
                .toList();
    }

    @Test
    void logsSaemEmJsonComRequestIdECamposDoEvento(CapturedOutput output) {
        Reply reply = postWithRequestId("/webhook", """
                {"license_plate":"LOG0001","entry_time":"2026-10-29T02:00:00","event_type":"ENTRY"}""", "req-abc-123");

        assertThat(reply.status()).isEqualTo(200);
        assertThat(reply.header("X-Request-Id")).isEqualTo("req-abc-123");

        List<JsonNode> forThisRequest = jsonLogLines(output).stream()
                .filter(line -> "req-abc-123".equals(line.path("request_id").asString(null)))
                .toList();
        assertThat(forThisRequest).isNotEmpty();
        assertThat(forThisRequest)
                .anySatisfy(line -> {
                    assertThat(line.path("message").asString()).isEqualTo("entrada registrada");
                    assertThat(line.path("level").asString()).isEqualTo("INFO");
                    assertThat(line.path("event").asString()).isEqualTo("ENTRY");
                    assertThat(line.path("plate").asString()).isEqualTo("LOG0001");
                });
        assertThat(forThisRequest)
                .anySatisfy(line -> {
                    assertThat(line.path("message").asString()).isEqualTo("http request");
                    assertThat(line.path("status").asString()).isEqualTo("200");
                    assertThat(line.path("path").asString()).isEqualTo("/webhook");
                });
    }

    @Test
    void eventoRepetidoSaiEmWarnERecusaSaiEmWarnComMotivo(CapturedOutput output) {
        enter("LOG0002", "2026-10-29T02:00:00");
        enter("LOG0002", "2026-10-29T02:00:01");
        park("NOENTRY", 1);

        List<JsonNode> lines = jsonLogLines(output);

        assertThat(lines).anySatisfy(line -> {
            assertThat(line.path("level").asString()).isEqualTo("WARN");
            assertThat(line.path("message").asString()).isEqualTo("entrada repetida ignorada");
        });
        assertThat(lines).anySatisfy(line -> {
            assertThat(line.path("level").asString()).isEqualTo("WARN");
            assertThat(line.path("message").asString()).isEqualTo("evento recusado");
            assertThat(line.path("reason").asString()).isEqualTo("veiculo sem entrada ativa");
        });
    }
}
