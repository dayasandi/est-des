package br.com.garage;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

class WebhookConfigTest {

    private static Properties config;

    @BeforeAll
    static void loadYaml() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml"));
        config = yaml.getObject();
    }

    @Test
    void webhookEscutaNaPorta3003UsadaPeloSimulador() {
        assertThat(config.getProperty("server.port")).isEqualTo("3003");
    }

    @Test
    void virtualThreadsDoJava21EstaoHabilitadas() {
        assertThat(config.getProperty("spring.threads.virtual.enabled")).isEqualTo("true");
    }
}
