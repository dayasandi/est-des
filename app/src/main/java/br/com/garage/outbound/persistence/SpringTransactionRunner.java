package br.com.garage.outbound.persistence;

import br.com.garage.application.port.outbound.TransactionRunner;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.transaction.support.TransactionTemplate;

public class SpringTransactionRunner implements TransactionRunner {

    private static final Logger log = LoggerFactory.getLogger(SpringTransactionRunner.class);
    static final int MAX_ATTEMPTS = 3;

    private final TransactionTemplate template;

    public SpringTransactionRunner(TransactionTemplate template) {
        this.template = template;
    }

    @Override
    public <T> T inTransaction(Supplier<T> work) {
        for (int attempt = 1; ; attempt++) {
            try {
                return template.execute(status -> work.get());
            } catch (ConcurrencyFailureException conflict) {
                if (attempt >= MAX_ATTEMPTS) {
                    throw conflict;
                }
                log.atWarn().addKeyValue("attempt", attempt).log("conflito de concorrencia, repetindo a transacao");
            }
        }
    }
}
