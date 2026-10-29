package br.com.garage.domain.garage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Coordinates(BigDecimal lat, BigDecimal lng) {

    private static final int SCALE = 6;
    private static final BigDecimal MAX_LAT = BigDecimal.valueOf(90);
    private static final BigDecimal MAX_LNG = BigDecimal.valueOf(180);

    public Coordinates {
        Objects.requireNonNull(lat, "lat");
        Objects.requireNonNull(lng, "lng");
        lat = lat.setScale(SCALE, RoundingMode.HALF_UP);
        lng = lng.setScale(SCALE, RoundingMode.HALF_UP);
        if (lat.abs().compareTo(MAX_LAT) > 0 || lng.abs().compareTo(MAX_LNG) > 0) {
            throw new IllegalArgumentException("coordenadas fora do intervalo valido: " + lat + ", " + lng);
        }
    }
}
