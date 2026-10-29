package br.com.garage.inbound.web;

import br.com.garage.application.usecase.GetRevenueUseCase;
import br.com.garage.application.usecase.RegisterEntryUseCase;
import br.com.garage.application.usecase.RegisterExitUseCase;
import br.com.garage.application.usecase.RegisterParkingUseCase;
import br.com.garage.domain.pricing.PriceTable;
import br.com.garage.fixture.DirectTransactionRunner;
import br.com.garage.fixture.InMemoryCapacityGate;
import br.com.garage.fixture.InMemoryGarageRepositories;
import br.com.garage.fixture.InMemoryRevenueReader;
import br.com.garage.fixture.InMemorySessionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

final class WebTestSupport {

    static final Instant NOW = Instant.parse("2026-10-29T12:00:00.123Z");

    final InMemorySessionRepository sessions = new InMemorySessionRepository();
    final InMemoryCapacityGate capacity = new InMemoryCapacityGate();
    final MockMvc mvc;

    WebTestSupport(int totalCapacity) {
        capacity.configure(totalCapacity);
        var tx = new DirectTransactionRunner();
        var sectors = InMemoryGarageRepositories.sectorsOfTestGarage();
        var dispatcher = new WebhookDispatcher(
                new RegisterEntryUseCase(sessions, capacity, tx),
                new RegisterParkingUseCase(
                        sessions, InMemoryGarageRepositories.spotsOfTestGarage(), sectors, PriceTable.standard(), tx),
                new RegisterExitUseCase(sessions, capacity, tx));
        var revenue = new RevenueController(
                new GetRevenueUseCase(new InMemoryRevenueReader(sessions), sectors),
                Clock.fixed(NOW, ZoneOffset.UTC));
        this.mvc = MockMvcBuilders.standaloneSetup(new WebhookController(dispatcher), revenue)
                .setControllerAdvice(new ApiExceptionHandler())
                .setMessageConverters(new JacksonJsonHttpMessageConverter(JsonMapper.builder().build()))
                .build();
    }
}
