package br.com.garage.config;

import br.com.garage.application.port.inbound.RegisterEntry;
import br.com.garage.application.port.inbound.RegisterExit;
import br.com.garage.application.port.inbound.RegisterParking;
import br.com.garage.inbound.sync.GarageSyncState;
import br.com.garage.inbound.web.GarageReadyInterceptor;
import br.com.garage.inbound.web.WebhookDispatcher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tools.jackson.databind.json.JsonMapper;

@Configuration(proxyBeanMethods = false)
class WebConfiguration implements WebMvcConfigurer {

    private final GarageSyncState state;
    private final JsonMapper mapper;

    WebConfiguration(GarageSyncState state, JsonMapper mapper) {
        this.state = state;
        this.mapper = mapper;
    }

    @Bean
    WebhookDispatcher webhookDispatcher(RegisterEntry entry, RegisterParking parking, RegisterExit exit) {
        return new WebhookDispatcher(entry, parking, exit);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new GarageReadyInterceptor(state, mapper)).addPathPatterns("/webhook");
    }
}
