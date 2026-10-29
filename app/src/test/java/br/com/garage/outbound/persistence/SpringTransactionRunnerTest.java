package br.com.garage.outbound.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

class SpringTransactionRunnerTest {

    private static final class RecordingTransactionManager implements PlatformTransactionManager {
        final AtomicInteger commits = new AtomicInteger();
        final AtomicInteger rollbacks = new AtomicInteger();

        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
            commits.incrementAndGet();
        }

        @Override
        public void rollback(TransactionStatus status) {
            rollbacks.incrementAndGet();
        }
    }

    private final RecordingTransactionManager manager = new RecordingTransactionManager();
    private final SpringTransactionRunner runner = new SpringTransactionRunner(new TransactionTemplate(manager));

    @Test
    void devolveOResultadoEConfirmaAUmaVez() {
        assertThat(runner.inTransaction(() -> "ok")).isEqualTo("ok");

        assertThat(manager.commits).hasValue(1);
        assertThat(manager.rollbacks).hasValue(0);
    }

    @Test
    void repeteQuandoOBancoDesfazPorDeadlock() {
        AtomicInteger calls = new AtomicInteger();

        String result = runner.inTransaction(() -> {
            if (calls.incrementAndGet() < 3) {
                throw new PessimisticLockingFailureException("deadlock");
            }
            return "ok";
        });

        assertThat(result).isEqualTo("ok");
        assertThat(calls).hasValue(3);
        assertThat(manager.rollbacks).hasValue(2);
        assertThat(manager.commits).hasValue(1);
    }

    @Test
    void desisteDepoisDeTresTentativasEPropagaOConflito() {
        AtomicInteger calls = new AtomicInteger();

        assertThatThrownBy(() -> runner.inTransaction(() -> {
                    calls.incrementAndGet();
                    throw new CannotAcquireLockException("lock");
                }))
                .isInstanceOf(CannotAcquireLockException.class);

        assertThat(calls).hasValue(SpringTransactionRunner.MAX_ATTEMPTS);
    }

    @Test
    void outrasExcecoesNaoSaoRepetidas() {
        AtomicInteger calls = new AtomicInteger();

        assertThatThrownBy(() -> runner.inTransaction(() -> {
                    calls.incrementAndGet();
                    throw new IllegalStateException("bug");
                }))
                .isInstanceOf(IllegalStateException.class);

        assertThat(calls).hasValue(1);
        assertThat(manager.rollbacks).hasValue(1);
    }
}
