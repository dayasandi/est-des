package br.com.garage.fixture;

import br.com.garage.application.port.outbound.TransactionRunner;
import java.util.function.Supplier;

public final class DirectTransactionRunner implements TransactionRunner {

    @Override
    public <T> T inTransaction(Supplier<T> work) {
        return work.get();
    }
}
