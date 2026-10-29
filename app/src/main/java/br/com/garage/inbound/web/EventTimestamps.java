package br.com.garage.inbound.web;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;

final class EventTimestamps {

    private EventTimestamps() {}

    static Instant parse(String raw, String field) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidRequestException("campo obrigatorio ausente: " + field);
        }
        try {
            TemporalAccessor parsed = DateTimeFormatter.ISO_DATE_TIME.parseBest(
                    raw.trim(), OffsetDateTime::from, LocalDateTime::from);
            return switch (parsed) {
                case OffsetDateTime offset -> offset.toInstant();
                case LocalDateTime local -> local.toInstant(ZoneOffset.UTC);
                default -> throw new IllegalStateException("tipo inesperado: " + parsed.getClass());
            };
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("horario invalido em " + field + ": '" + raw + "'", e);
        }
    }
}
