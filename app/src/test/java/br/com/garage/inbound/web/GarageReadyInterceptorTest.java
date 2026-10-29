package br.com.garage.inbound.web;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.garage.inbound.sync.GarageSyncState;
import br.com.garage.inbound.sync.RetryingGarageSynchronizer;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

class GarageReadyInterceptorTest {

    private final GarageSyncState state = new GarageSyncState();
    private final GarageReadyInterceptor interceptor = new GarageReadyInterceptor(state, JsonMapper.builder().build());

    @Test
    void antesDaCargaDaGaragemRecusaComServiceUnavailableERetryAfter() throws Exception {
        var response = new MockHttpServletResponse();

        boolean proceed = interceptor.preHandle(new MockHttpServletRequest(), response, new Object());

        assertThat(proceed).isFalse();
        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getHeader("Retry-After")).isEqualTo("1");
        assertThat(response.getContentType()).startsWith("application/problem+json");
        assertThat(response.getContentAsString()).contains("garagem ainda nao carregada");
    }

    @Test
    void depoisDaCargaDeixaPassar() throws Exception {
        new RetryingGarageSynchronizer(() -> {}, state, d -> {}, Duration.ofMillis(1), Duration.ofMillis(1)).run();

        var response = new MockHttpServletResponse();
        boolean proceed = interceptor.preHandle(new MockHttpServletRequest(), response, new Object());

        assertThat(proceed).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }
}
