package br.com.garage.application.port.inbound;

import br.com.garage.domain.garage.SectorCode;
import br.com.garage.domain.shared.Money;

public sealed interface ExitResult {

    record Charged(SectorCode sector, Money amount) implements ExitResult {}

    record LeftWithoutParking() implements ExitResult {}

    record NoActiveSession() implements ExitResult {}
}
