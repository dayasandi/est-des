package br.com.garage.contract;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.garage.application.port.outbound.CapacityGate;
import org.junit.jupiter.api.Test;

public abstract class CapacityGateContract {

    protected abstract CapacityGate gate();

    @Test
    void concedeReservasAteACapacidadeEDepoisRecusa() {
        gate().configure(2);

        assertThat(gate().tryReserve()).isTrue();
        assertThat(gate().tryReserve()).isTrue();
        assertThat(gate().tryReserve()).isFalse();
    }

    @Test
    void liberarReabreUmaVaga() {
        gate().configure(1);
        assertThat(gate().tryReserve()).isTrue();
        assertThat(gate().tryReserve()).isFalse();

        gate().release();

        assertThat(gate().tryReserve()).isTrue();
    }

    @Test
    void liberarSemReservaNaoCriaCapacidadeExtra() {
        gate().configure(1);

        gate().release();
        gate().release();

        assertThat(gate().tryReserve()).isTrue();
        assertThat(gate().tryReserve()).isFalse();
    }

    @Test
    void reconfigurarNaoApagaAsReservasEmAndamento() {
        gate().configure(2);
        gate().tryReserve();
        gate().tryReserve();

        gate().configure(2);

        assertThat(gate().tryReserve()).isFalse();
    }

    @Test
    void aumentarACapacidadeLiberaNovasReservas() {
        gate().configure(1);
        gate().tryReserve();

        gate().configure(3);

        assertThat(gate().tryReserve()).isTrue();
        assertThat(gate().tryReserve()).isTrue();
        assertThat(gate().tryReserve()).isFalse();
    }
}
