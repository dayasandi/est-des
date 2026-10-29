package br.com.garage.application.port.inbound;

import br.com.garage.domain.shared.Money;

public sealed interface RevenueResult {

    record Total(Money amount) implements RevenueResult {}

    record UnknownSector() implements RevenueResult {}
}
