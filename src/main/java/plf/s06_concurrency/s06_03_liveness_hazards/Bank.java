package plf.s06_concurrency.s06_03_liveness_hazards;

import java.util.concurrent.CyclicBarrier;

public final class Bank {

    public static final class Account {
        private final int id;
        private long balance;

        public Account(int id, long balance) {
            this.id = id;
            this.balance = balance;
        }

        public int id() {
            return id;
        }

        public synchronized long balance() {
            return balance;
        }

        synchronized void withdraw(long amt) {
            balance -= amt;
        }

        synchronized void deposit(long amt) {
            balance += amt;
        }
    }

    // Acquires `from`'s monitor, then `to`'s monitor, in argument order.
    // Two threads calling transfer(a,b) and transfer(b,a) form a cycle.
    public static void transfer(Account from, Account to, long amt, CyclicBarrier gate) {
        synchronized (from) {
            await(gate);                 // both threads now hold exactly one monitor
            synchronized (to) {
                from.withdraw(amt);
                to.deposit(amt);
            }
        }
    }

    // TODO: break the cycle. Acquire the two monitors in a GLOBAL order that does
    // not depend on argument order, so transfer(a,b) and transfer(b,a) agree.
    public static void safeTransfer(Account from, Account to, long amt) {
        throw new UnsupportedOperationException("TODO"); // <-- Task 1 here
    }

    private static void await(CyclicBarrier gate) {
        if (gate == null) {
            return;
        }
        try {
            gate.await();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Bank() {
    }
}
