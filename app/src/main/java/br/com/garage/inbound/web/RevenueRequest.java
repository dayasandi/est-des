package br.com.garage.inbound.web;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RevenueRequest(String date, String sector) {}
