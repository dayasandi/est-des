package br.com.garage.inbound.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record WebhookResponse(String status, String sector, BigDecimal amount) {

    static WebhookResponse of(String status) {
        return new WebhookResponse(status, null, null);
    }

    static WebhookResponse inSector(String status, String sector) {
        return new WebhookResponse(status, sector, null);
    }
}
