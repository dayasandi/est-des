package br.com.garage.application.port.outbound;

import br.com.garage.domain.garage.GarageConfig;

@FunctionalInterface
public interface GarageConfigProvider {

    GarageConfig fetch();
}
