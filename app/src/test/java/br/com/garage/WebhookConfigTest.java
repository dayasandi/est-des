package br.com.garage;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class WebhookConfigTest {

    @Autowired
    private Environment env;

    @Test
    void webhookEscutaNaPorta3003UsadaPeloSimulador() {
        assertThat(env.getProperty("server.port", Integer.class)).isEqualTo(3003);
    }

    @Test
    void virtualThreadsDoJava21EstaoHabilitadas() {
        assertThat(env.getProperty("spring.threads.virtual.enabled", Boolean.class)).isTrue();
    }
}
