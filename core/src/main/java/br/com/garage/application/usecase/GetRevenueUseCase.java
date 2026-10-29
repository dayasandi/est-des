package br.com.garage.application.usecase;

import br.com.garage.application.port.inbound.GetRevenue;
import br.com.garage.application.port.inbound.RevenueResult;
import br.com.garage.application.port.inbound.RevenueResult.Total;
import br.com.garage.application.port.inbound.RevenueResult.UnknownSector;
import br.com.garage.application.port.outbound.RevenueReader;
import br.com.garage.application.port.outbound.SectorRepository;
import br.com.garage.domain.garage.SectorCode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

public final class GetRevenueUseCase implements GetRevenue {

    private final RevenueReader revenue;
    private final SectorRepository sectors;

    public GetRevenueUseCase(RevenueReader revenue, SectorRepository sectors) {
        this.revenue = revenue;
        this.sectors = sectors;
    }

    @Override
    public RevenueResult revenueOf(LocalDate day, Optional<SectorCode> sector) {
        Instant from = day.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant to = day.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        return sector.map(code -> sectors.findByCode(code)
                        .<RevenueResult>map(found -> new Total(revenue.totalOf(found.code(), from, to)))
                        .orElseGet(UnknownSector::new))
                .orElseGet(() -> new Total(revenue.total(from, to)));
    }
}
