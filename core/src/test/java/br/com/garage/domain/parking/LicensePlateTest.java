package br.com.garage.domain.parking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LicensePlateTest {

    @Test
    void normalizaCaixaEEspacos() {
        assertThat(new LicensePlate("  zul0001 ")).isEqualTo(new LicensePlate("ZUL0001"));
        assertThat(new LicensePlate("abc-1234").value()).isEqualTo("ABC-1234");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "ABC 123", "ABC*123", "ABCDEFGHIJKLMNOPQ"})
    void rejeitaPlacaForaDoFormato(String raw) {
        assertThatThrownBy(() -> new LicensePlate(raw)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aceitaPlacasDeTamanhoVariavel() {
        assertThat(new LicensePlate("TA98547").value()).isEqualTo("TA98547");
        assertThat(new LicensePlate("AMV26962").value()).isEqualTo("AMV26962");
    }
}
