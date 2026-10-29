package br.com.garage.inbound.web;

import br.com.garage.application.port.inbound.ExitResult;
import br.com.garage.application.port.inbound.ParkingResult;
import br.com.garage.application.port.inbound.RegisterEntry;
import br.com.garage.application.port.inbound.RegisterExit;
import br.com.garage.application.port.inbound.RegisterParking;
import br.com.garage.domain.garage.Coordinates;
import br.com.garage.domain.parking.LicensePlate;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class WebhookDispatcher {

    private static final Logger log = LoggerFactory.getLogger(WebhookDispatcher.class);

    private final RegisterEntry entry;
    private final RegisterParking parking;
    private final RegisterExit exit;

    public WebhookDispatcher(RegisterEntry entry, RegisterParking parking, RegisterExit exit) {
        this.entry = entry;
        this.parking = parking;
        this.exit = exit;
    }

    public ResponseEntity<Object> dispatch(WebhookEvent event) {
        try {
            return switch (event) {
                case WebhookEvent.Entry e -> handle(e);
                case WebhookEvent.Parked e -> handle(e);
                case WebhookEvent.Exit e -> handle(e);
            };
        } catch (IllegalArgumentException invalid) {

            throw new InvalidRequestException(invalid.getMessage(), invalid);
        }
    }

    private ResponseEntity<Object> handle(WebhookEvent.Entry event) {
        LicensePlate plate = plateOf(event);
        var outcome = entry.register(plate, EventTimestamps.parse(event.entryTime(), "entry_time"));
        return switch (outcome) {
            case ACCEPTED -> {
                info("ENTRY", plate).log("entrada registrada");
                yield Responses.ok(WebhookResponse.of("ACCEPTED"));
            }
            case ALREADY_INSIDE -> {
                warn("ENTRY", plate).log("entrada repetida ignorada");
                yield Responses.ok(WebhookResponse.of("ALREADY_INSIDE"));
            }
            case GARAGE_FULL -> reject("ENTRY", plate, HttpStatus.CONFLICT, "garagem cheia");
        };
    }

    private ResponseEntity<Object> handle(WebhookEvent.Parked event) {
        LicensePlate plate = plateOf(event);
        if (event.lat() == null || event.lng() == null) {
            throw new InvalidRequestException("campo obrigatorio ausente: lat/lng");
        }
        var location = new Coordinates(BigDecimal.valueOf(event.lat()), BigDecimal.valueOf(event.lng()));
        return switch (parking.register(plate, location)) {
            case ParkingResult.Accepted accepted -> {
                info("PARKED", plate)
                        .addKeyValue("sector", accepted.sector())
                        .addKeyValue("price_factor", accepted.priceFactor())
                        .log("estacionamento registrado");
                yield Responses.ok(WebhookResponse.inSector("ACCEPTED", accepted.sector().value()));
            }
            case ParkingResult.AlreadyParked ignored -> {
                warn("PARKED", plate).log("estacionamento repetido ignorado");
                yield Responses.ok(WebhookResponse.of("ALREADY_PARKED"));
            }
            case ParkingResult.ParkedElsewhere ignored ->
                reject("PARKED", plate, HttpStatus.CONFLICT, "veiculo ja estacionado em outra vaga");
            case ParkingResult.SpotTaken ignored ->
                reject("PARKED", plate, HttpStatus.CONFLICT, "vaga ocupada por outro veiculo");
            case ParkingResult.NoActiveEntry ignored ->
                reject("PARKED", plate, HttpStatus.CONFLICT, "veiculo sem entrada ativa");
            case ParkingResult.UnknownSpot ignored ->
                reject("PARKED", plate, HttpStatus.UNPROCESSABLE_CONTENT, "coordenada nao corresponde a nenhuma vaga");
        };
    }

    private ResponseEntity<Object> handle(WebhookEvent.Exit event) {
        LicensePlate plate = plateOf(event);
        var result = exit.register(plate, EventTimestamps.parse(event.exitTime(), "exit_time"));
        return switch (result) {
            case ExitResult.Charged charged -> {
                info("EXIT", plate)
                        .addKeyValue("sector", charged.sector())
                        .addKeyValue("amount", charged.amount().amount())
                        .log("saida registrada");
                yield Responses.ok(new WebhookResponse("CHARGED", charged.sector().value(), charged.amount().amount()));
            }
            case ExitResult.LeftWithoutParking ignored -> {
                warn("EXIT", plate).log("saida sem ter estacionado: vaga liberada, sem tarifa");
                yield Responses.ok(WebhookResponse.of("LEFT_WITHOUT_PARKING"));
            }
            case ExitResult.NoActiveSession ignored -> {
                warn("EXIT", plate).log("saida sem permanencia ativa ignorada");
                yield Responses.ok(WebhookResponse.of("NO_ACTIVE_SESSION"));
            }
        };
    }

    private ResponseEntity<Object> reject(String event, LicensePlate plate, HttpStatus status, String reason) {
        warn(event, plate).addKeyValue("reason", reason).log("evento recusado");
        return Responses.rejected(status, reason);
    }

    private static LoggingEventBuilder info(String event, LicensePlate plate) {
        return log.atInfo().addKeyValue("event", event).addKeyValue("plate", plate);
    }

    private static LoggingEventBuilder warn(String event, LicensePlate plate) {
        return log.atWarn().addKeyValue("event", event).addKeyValue("plate", plate);
    }

    private static LicensePlate plateOf(WebhookEvent event) {
        if (event.licensePlate() == null) {
            throw new InvalidRequestException("campo obrigatorio ausente: license_plate");
        }
        return new LicensePlate(event.licensePlate());
    }
}
