package br.com.garage.inbound.web;

import br.com.garage.application.port.inbound.GetRevenue;
import br.com.garage.application.port.inbound.RevenueResult;
import br.com.garage.domain.garage.SectorCode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RevenueController {

    private final GetRevenue revenue;
    private final Clock clock;

    public RevenueController(GetRevenue revenue, Clock clock) {
        this.revenue = revenue;
        this.clock = clock;
    }

    @GetMapping("/revenue")
    public ResponseEntity<Object> revenue(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String sector,
            @RequestBody(required = false) RevenueRequest body) {
        LocalDate day = firstPresent(date, body == null ? null : body.date())
                .map(RevenueController::parseDate)
                .orElseGet(() -> LocalDate.now(clock));
        Optional<SectorCode> sectorCode = firstPresent(sector, body == null ? null : body.sector())
                .map(RevenueController::parseSector);

        return switch (revenue.revenueOf(day, sectorCode)) {
            case RevenueResult.Total total ->
                Responses.ok(RevenueResponse.brl(total.amount().amount(), clock.instant()));
            case RevenueResult.UnknownSector ignored ->
                Responses.rejected(HttpStatus.NOT_FOUND, "setor inexistente");
        };
    }

    private static Optional<String> firstPresent(String... candidates) {
        return Stream.of(candidates).filter(value -> value != null && !value.isBlank()).findFirst();
    }

    private static LocalDate parseDate(String raw) {
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("data invalida, use YYYY-MM-DD: '" + raw + "'", e);
        }
    }

    private static SectorCode parseSector(String raw) {
        try {
            return new SectorCode(raw);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("setor invalido: '" + raw + "'", e);
        }
    }
}
