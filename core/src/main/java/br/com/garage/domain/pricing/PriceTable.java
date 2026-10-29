package br.com.garage.domain.pricing;

import br.com.garage.domain.garage.Occupancy;
import java.math.BigDecimal;
import java.util.List;

public record PriceTable(List<PriceBand> bands) implements PricingPolicy {

    public PriceTable {
        bands = List.copyOf(bands);
        if (bands.isEmpty()) {
            throw new IllegalArgumentException("a tabela precisa de ao menos uma faixa");
        }
        for (int i = 1; i < bands.size(); i++) {
            if (bands.get(i).upperPercentExclusive() <= bands.get(i - 1).upperPercentExclusive()) {
                throw new IllegalArgumentException("as faixas devem ter tetos estritamente crescentes");
            }
        }
        if (bands.getLast().upperPercentExclusive() != PriceBand.OPEN_ENDED) {
            throw new IllegalArgumentException("a ultima faixa deve ser aberta para cobrir 100% de lotacao");
        }
    }

    public static PriceTable standard() {
        return new PriceTable(List.of(
                new PriceBand(25, new BigDecimal("0.90")),
                new PriceBand(50, new BigDecimal("1.00")),
                new PriceBand(75, new BigDecimal("1.10")),
                new PriceBand(PriceBand.OPEN_ENDED, new BigDecimal("1.25"))));
    }

    @Override
    public BigDecimal factorFor(Occupancy occupancy) {
        return bands.stream()
                .filter(band -> occupancy.isBelowPercent(band.upperPercentExclusive()))
                .map(PriceBand::factor)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("nenhuma faixa cobre a lotacao " + occupancy));
    }
}
