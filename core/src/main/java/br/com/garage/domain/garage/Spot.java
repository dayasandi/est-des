package br.com.garage.domain.garage;

import java.util.Objects;

public record Spot(long id, SectorCode sector, Coordinates location) {

    public Spot {
        Objects.requireNonNull(sector, "sector");
        Objects.requireNonNull(location, "location");
    }
}
