package br.com.garage.domain.pricing;

import br.com.garage.domain.garage.Occupancy;
import java.math.BigDecimal;

@FunctionalInterface
public interface PricingPolicy {

    BigDecimal factorFor(Occupancy occupancy);
}
