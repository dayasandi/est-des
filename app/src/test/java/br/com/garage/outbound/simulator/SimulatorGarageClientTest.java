package br.com.garage.outbound.simulator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.garage.domain.garage.GarageConfig;
import br.com.garage.domain.garage.SectorCode;
import br.com.garage.domain.shared.Money;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SimulatorGarageClientTest {

    private static final String GARAGE_JSON = """
            {"garage":[
              {"sector":"A","base_price":40.5,"max_capacity":10,"open_hour":"00:00","close_hour":"23:59","duration_limit_minutes":1440},
              {"sector":"B","base_price":4.1,"max_capacity":20,"open_hour":"08:00","close_hour":"23:59","duration_limit_minutes":60}],
             "spots":[
              {"id":1,"sector":"A","lat":-23.561684,"lng":-46.655981,"occupied":false},
              {"id":11,"sector":"B","lat":-23.561484,"lng":-46.655781,"occupied":true}]}
            """;

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    private SimulatorGarageClient clientFor(int status, String body) throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/garage", exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        URI url = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
        return new SimulatorGarageClient(new SimulatorProperties(url, Duration.ofSeconds(1), Duration.ofSeconds(2)));
    }

    @Test
    void convertePrecosCapacidadesEVagasDoFormatoDoSimulador() throws IOException {
        GarageConfig config = clientFor(200, GARAGE_JSON).fetch();

        assertThat(config.sectors()).hasSize(2);
        assertThat(config.sectors().getFirst().code()).isEqualTo(new SectorCode("A"));
        assertThat(config.sectors().getFirst().basePrice()).isEqualTo(Money.of("40.50"));
        assertThat(config.sectors().get(1).basePrice()).isEqualTo(Money.of("4.10"));
        assertThat(config.totalCapacity()).isEqualTo(30);
        assertThat(config.spots()).hasSize(2);
        assertThat(config.spots().getFirst().location().lat()).isEqualByComparingTo("-23.561684");
        assertThat(config.spots().getFirst().location().lng()).isEqualByComparingTo("-46.655981");
    }

    @Test
    void erroDoSimuladorViraExcecaoParaQueASincronizacaoTenteDeNovo() throws IOException {
        SimulatorGarageClient client = clientFor(500, "{}");

        assertThatThrownBy(client::fetch).isInstanceOf(RuntimeException.class);
    }

    @Test
    void respostaIncompletaViraExcecao() throws IOException {
        SimulatorGarageClient client = clientFor(200, "{\"garage\":[]}");

        assertThatThrownBy(client::fetch).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void simuladorForaDoArViraExcecao() {
        var unreachable = new SimulatorProperties(URI.create("http://127.0.0.1:1"), Duration.ofMillis(300), Duration.ofSeconds(1));

        assertThatThrownBy(() -> new SimulatorGarageClient(unreachable).fetch()).isInstanceOf(RuntimeException.class);
    }
}
