package br.com.garage.domain.pricing;

import java.math.BigDecimal;
import java.util.Objects;

public record PriceBand(int upperPercentExclusive, BigDecimal factor) {

    public static final int OPEN_ENDED = Integer.MAX_VALUE;

    public PriceBand {
        Objects.requireNonNull(factor, "factor");
        if (upperPercentExclusive <= 0) {
            throw new IllegalArgumentException("teto da faixa deve ser positivo: " + upperPercentExclusive);
        }
        if (factor.signum() <= 0) {
            throw new IllegalArgumentException("fator de preco deve ser positivo: " + factor);
        }
    }
}
