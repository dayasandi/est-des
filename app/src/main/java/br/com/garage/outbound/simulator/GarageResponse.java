package br.com.garage.outbound.simulator;

import br.com.garage.domain.garage.Coordinates;
import br.com.garage.domain.garage.GarageConfig;
import br.com.garage.domain.garage.Sector;
import br.com.garage.domain.garage.SectorCode;
import br.com.garage.domain.garage.Spot;
import br.com.garage.domain.shared.Money;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record GarageResponse(List<SectorDto> garage, List<SpotDto> spots) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SectorDto(
            String sector,
            @JsonProperty("base_price") BigDecimal basePrice,
            @JsonProperty("max_capacity") int maxCapacity) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SpotDto(long id, String sector, BigDecimal lat, BigDecimal lng) {}

    GarageConfig toDomain() {
        List<Sector> sectors = garage.stream()
                .map(s -> new Sector(new SectorCode(s.sector()), new Money(s.basePrice()), s.maxCapacity()))
                .toList();
        List<Spot> domainSpots = spots.stream()
                .map(s -> new Spot(s.id(), new SectorCode(s.sector()), new Coordinates(s.lat(), s.lng())))
                .toList();
        return new GarageConfig(sectors, domainSpots);
    }
}
