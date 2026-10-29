package br.com.garage.domain.garage;

import java.util.Locale;
import java.util.Objects;

public record SectorCode(String value) {

    private static final int MAX_LENGTH = 8;

    public SectorCode {
        Objects.requireNonNull(value, "value");
        value = value.trim().toUpperCase(Locale.ROOT);
        if (value.isEmpty() || value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("codigo de setor invalido: '" + value + "'");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
