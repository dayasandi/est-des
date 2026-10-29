package br.com.garage.outbound.simulator;

import br.com.garage.application.port.outbound.GarageConfigProvider;
import br.com.garage.domain.garage.GarageConfig;
import java.net.http.HttpClient;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

public class SimulatorGarageClient implements GarageConfigProvider {

    private final RestClient client;

    public SimulatorGarageClient(SimulatorProperties properties) {
        var factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).build());
        factory.setReadTimeout(properties.readTimeout());
        this.client = RestClient.builder()
                .baseUrl(properties.url().toString())
                .requestFactory(factory)
                .build();
    }

    @Override
    public GarageConfig fetch() {
        GarageResponse response = client.get().uri("/garage").retrieve().body(GarageResponse.class);
        if (response == null || response.garage() == null || response.spots() == null) {
            throw new IllegalStateException("resposta de /garage vazia ou incompleta");
        }
        return response.toDomain();
    }
}
