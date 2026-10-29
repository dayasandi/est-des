package br.com.garage.domain.garage;

public record Occupancy(int occupied, int capacity) {

    public Occupancy {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacidade deve ser positiva: " + capacity);
        }
        if (occupied < 0) {
            throw new IllegalArgumentException("ocupacao nao pode ser negativa: " + occupied);
        }
    }

    public boolean isBelowPercent(int percent) {
        return (long) occupied * 100 < (long) percent * capacity;
    }

    public boolean isFull() {
        return occupied >= capacity;
    }
}
