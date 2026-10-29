package br.com.garage.inbound.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public record RevenueResponse(BigDecimal amount, String currency, String timestamp) {

    private static final DateTimeFormatter MILLIS_UTC =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    static RevenueResponse brl(BigDecimal amount, Instant queriedAt) {
        return new RevenueResponse(amount, "BRL", MILLIS_UTC.format(queriedAt));
    }
}
