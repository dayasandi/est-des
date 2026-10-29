package br.com.garage.domain.pricing;

import br.com.garage.domain.shared.Money;
import java.math.BigDecimal;
import java.time.Duration;

public final class ParkingFee {

    static final Duration FREE_PERIOD = Duration.ofMinutes(30);
    private static final long MILLIS_PER_HOUR = Duration.ofHours(1).toMillis();

    private ParkingFee() {}

    public static Money charge(Duration stay, Money hourlyPrice, BigDecimal priceFactor) {
        if (stay.compareTo(FREE_PERIOD) <= 0) {
            return Money.ZERO;
        }
        long billableHours = Math.ceilDiv(stay.toMillis(), MILLIS_PER_HOUR);
        return hourlyPrice.scaledBy(priceFactor.multiply(BigDecimal.valueOf(billableHours)));
    }
}
