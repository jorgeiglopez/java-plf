package plf.s02_oop_class_design.s02_07_object_methods_contracts;

import java.math.BigDecimal;
import java.util.Objects;

public final class Money implements Comparable<Money> {

    private final BigDecimal amount;
    private final String currency;

    public Money(String amount, String currency) {
        this.amount = new BigDecimal(amount);
        this.currency = Objects.requireNonNull(currency);
    }

    public BigDecimal amount() {
        return amount;
    }

    public String currency() {
        return currency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Money m)) {
            return false;
        }
        return amount.equals(m.amount) && currency.equals(m.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }

    @Override
    public String toString() {
        return currency + " " + amount;
    }

    @Override
    public int compareTo(Money o) {
        return amount.compareTo(o.amount); // <-- the bug lives on this line
    }
}
