package br.com.garage.inbound.web;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "event_type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = WebhookEvent.Entry.class, name = "ENTRY"),
    @JsonSubTypes.Type(value = WebhookEvent.Parked.class, name = "PARKED"),
    @JsonSubTypes.Type(value = WebhookEvent.Exit.class, name = "EXIT")
})
public sealed interface WebhookEvent {

    String licensePlate();

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Entry(
            @JsonProperty("license_plate") String licensePlate, @JsonProperty("entry_time") String entryTime)
            implements WebhookEvent {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Parked(
            @JsonProperty("license_plate") String licensePlate,
            @JsonProperty("lat") Double lat,
            @JsonProperty("lng") Double lng)
            implements WebhookEvent {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Exit(
            @JsonProperty("license_plate") String licensePlate, @JsonProperty("exit_time") String exitTime)
            implements WebhookEvent {}
}
