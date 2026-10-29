package br.com.garage.domain.parking;

import br.com.garage.domain.garage.SectorCode;
import br.com.garage.domain.garage.Spot;
import br.com.garage.domain.pricing.ParkingFee;
import br.com.garage.domain.shared.Money;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public sealed interface ParkingSession {

    LicensePlate plate();

    Instant entryTime();

    record Entered(LicensePlate plate, Instant entryTime) implements ParkingSession {

        public Entered {
            Objects.requireNonNull(plate, "plate");
            Objects.requireNonNull(entryTime, "entryTime");
        }

        public Parked parkAt(Spot spot, Money hourlyPrice, BigDecimal priceFactor) {
            return new Parked(plate, entryTime, spot.id(), spot.sector(), hourlyPrice, priceFactor);
        }

        public Closed leave(Instant exitTime) {
            return new Closed(plate, entryTime, exitTime, Money.ZERO);
        }
    }

    record Parked(
            LicensePlate plate,
            Instant entryTime,
            long spotId,
            SectorCode sector,
            Money hourlyPrice,
            BigDecimal priceFactor)
            implements ParkingSession {

        public Parked {
            Objects.requireNonNull(plate, "plate");
            Objects.requireNonNull(entryTime, "entryTime");
            Objects.requireNonNull(sector, "sector");
            Objects.requireNonNull(hourlyPrice, "hourlyPrice");
            Objects.requireNonNull(priceFactor, "priceFactor");
        }

        public Closed leave(Instant exitTime) {
            Duration stay = Duration.between(entryTime, exitTime);
            return new Closed(plate, entryTime, exitTime, ParkingFee.charge(stay, hourlyPrice, priceFactor));
        }
    }

    record Closed(LicensePlate plate, Instant entryTime, Instant exitTime, Money amount) implements ParkingSession {

        public Closed {
            Objects.requireNonNull(plate, "plate");
            Objects.requireNonNull(entryTime, "entryTime");
            Objects.requireNonNull(exitTime, "exitTime");
            Objects.requireNonNull(amount, "amount");
        }
    }
}
