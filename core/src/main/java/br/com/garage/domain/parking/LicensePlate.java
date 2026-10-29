package br.com.garage.domain.parking;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public record LicensePlate(String value) {

    private static final Pattern FORMAT = Pattern.compile("[A-Z0-9-]{1,16}");

    public LicensePlate {
        Objects.requireNonNull(value, "value");
        value = value.trim().toUpperCase(Locale.ROOT);
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("placa invalida: '" + value + "'");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
