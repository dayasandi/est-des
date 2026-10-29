package br.com.garage.domain.garage;

import br.com.garage.domain.shared.Money;
import java.util.Objects;

public record Sector(SectorCode code, Money basePrice, int maxCapacity) {

    public Sector {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(basePrice, "basePrice");
        if (maxCapacity <= 0) {
            throw new IllegalArgumentException("capacidade do setor deve ser positiva: " + maxCapacity);
        }
    }
}
