package br.com.garage.domain.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Money(BigDecimal amount) {

    public static final Money ZERO = new Money(BigDecimal.ZERO);

    public Money {
        Objects.requireNonNull(amount, "amount");
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("valor monetario nao pode ser negativo: " + amount);
        }
        amount = amount.setScale(2, RoundingMode.CEILING);
    }

    public static Money of(String value) {
        return new Money(new BigDecimal(value));
    }

    public Money plus(Money other) {
        return new Money(amount.add(other.amount));
    }

    public Money scaledBy(BigDecimal multiplier) {
        return new Money(amount.multiply(multiplier));
    }
}
