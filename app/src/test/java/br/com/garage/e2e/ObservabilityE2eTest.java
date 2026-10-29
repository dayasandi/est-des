package br.com.garage.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ObservabilityE2eTest extends E2eSupport {

    @Test
    void semRequestIdRecebidoGeraUmEDevolveNoCabecalho() {
        Reply reply = enter("LOG0003", "2026-10-29T02:00:00");

        assertThat(reply.header("X-Request-Id")).matches("[0-9a-f-]{36}");
    }

    @Test
    void prometheusExpoeMetricasDeNegocioCapacidadeEHttp() {
        enter("MET0001", "2026-10-29T02:00:00");
        park("MET0001", 1);
        leave("MET0001", "2026-10-29T03:00:00");
        enter("MET0002", "2026-10-29T02:00:00");

        String metrics = raw("/actuator/prometheus");

        assertThat(metrics)
                .contains("garage_events_total{")
                .contains("event=\"entry\"")
                .contains("outcome=\"accepted\"")
                .contains("event=\"exit\"")
                .contains("outcome=\"charged\"")
                .contains("garage_exit_charged")
                .contains("http_server_requests_seconds")
                .contains("garage_capacity{")
                .contains("state=\"total\"")
                .contains("state=\"reserved\"");
        assertThat(metrics).containsPattern("garage_capacity\\{[^}]*state=\"reserved\"[^}]*\\} 1\\.0");
        assertThat(metrics).containsPattern("garage_capacity\\{[^}]*state=\"total\"[^}]*\\} 30\\.0");
    }

    @Test
    void livenessEReadinessFicamUpDepoisDaCargaDaGaragem() {
        assertThat(get("/actuator/health/liveness").status()).isEqualTo(200);

        Reply readiness = get("/actuator/health/readiness");

        assertThat(readiness.status()).isEqualTo(200);
        assertThat(readiness.body().path("status").asString()).isEqualTo("UP");
    }
}
