package br.com.garage.domain.garage;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record GarageConfig(List<Sector> sectors, List<Spot> spots) {

    public GarageConfig {
        sectors = List.copyOf(sectors);
        spots = List.copyOf(spots);

        Set<SectorCode> codes = new HashSet<>();
        sectors.forEach(sector -> {
            if (!codes.add(sector.code())) {
                throw new IllegalArgumentException("setor duplicado: " + sector.code());
            }
        });

        Set<Long> ids = new HashSet<>();
        spots.forEach(spot -> {
            if (!ids.add(spot.id())) {
                throw new IllegalArgumentException("vaga duplicada: " + spot.id());
            }
            if (!codes.contains(spot.sector())) {
                throw new IllegalArgumentException("vaga " + spot.id() + " referencia setor desconhecido: " + spot.sector());
            }
        });
    }

    public int totalCapacity() {
        return sectors.stream().mapToInt(Sector::maxCapacity).sum();
    }
}
