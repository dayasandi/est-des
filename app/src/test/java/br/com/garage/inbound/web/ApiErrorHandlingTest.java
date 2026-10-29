package br.com.garage.inbound.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.garage.application.port.inbound.RegisterEntry;
import br.com.garage.application.port.inbound.RegisterExit;
import br.com.garage.application.port.inbound.RegisterParking;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.CannotCreateTransactionException;
import tools.jackson.databind.json.JsonMapper;

class ApiErrorHandlingTest {

    private static final String ENTRY = """
            {"license_plate":"ABC1234","entry_time":"2026-10-29T02:00:00","event_type":"ENTRY"}""";

    private static MockMvc mvcWhereEntryFails(RuntimeException failure) {
        RegisterEntry failing = (plate, time) -> {
            throw failure;
        };
        RegisterParking parking = (plate, location) -> {
            throw new UnsupportedOperationException();
        };
        RegisterExit exit = (plate, time) -> {
            throw new UnsupportedOperationException();
        };
        var dispatcher = new WebhookDispatcher(failing, parking, exit);
        return MockMvcBuilders.standaloneSetup(new WebhookController(dispatcher))
                .setControllerAdvice(new ApiExceptionHandler())
                .setMessageConverters(new JacksonJsonHttpMessageConverter(JsonMapper.builder().build()))
                .build();
    }

    @Test
    void erroInesperadoViraFiveHundredSemVazarACausa() throws Exception {
        mvcWhereEntryFails(new IllegalStateException("senha=segredo jdbc:mysql://interno"))
                .perform(post("/webhook").contentType(MediaType.APPLICATION_JSON).content(ENTRY))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("erro interno"));
    }

    @Test
    void bancoIndisponivelViraServiceUnavailableComRetryAfter() throws Exception {
        mvcWhereEntryFails(new CannotCreateTransactionException("sem conexao"))
                .perform(post("/webhook").contentType(MediaType.APPLICATION_JSON).content(ENTRY))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Retry-After", "1"))
                .andExpect(jsonPath("$.detail").value("servico temporariamente indisponivel"));

        mvcWhereEntryFails(new DataAccessResourceFailureException("queda"))
                .perform(post("/webhook").contentType(MediaType.APPLICATION_JSON).content(ENTRY))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void errosPadraoDoMvcMantemOStatusCorreto() throws Exception {
        MockMvc mvc = mvcWhereEntryFails(new IllegalStateException());

        mvc.perform(get("/webhook")).andExpect(status().isMethodNotAllowed());
        mvc.perform(post("/webhook").contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void relogioDaRotaDeReceitaNaoInterfereNoTratamentoDeErros() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                        new RevenueController((day, sector) -> {
                            throw new CannotCreateTransactionException("sem conexao");
                        }, Clock.systemUTC()))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();

        mvc.perform(get("/revenue?date=2026-10-29")).andExpect(status().isServiceUnavailable());
    }
}
