package br.com.garage;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class CoreIsolationTest {

    @Test
    void coreNaoEnxergaSpringNemJackson() {
        List.of(
                        "org.springframework.context.ApplicationContext",
                        "com.fasterxml.jackson.databind.ObjectMapper",
                        "tools.jackson.databind.ObjectMapper")
                .forEach(classe -> assertThatThrownBy(() -> Class.forName(classe))
                        .describedAs("%s nao pode estar no classpath do core", classe)
                        .isInstanceOf(ClassNotFoundException.class));
    }
}
