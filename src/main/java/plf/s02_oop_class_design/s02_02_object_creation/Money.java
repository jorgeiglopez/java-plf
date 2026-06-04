package plf.s02_oop_class_design.s02_02_object_creation;

import java.util.HashMap;
import java.util.Map;

public final class Money {

    private static final int CACHE_MAX = 10;
    private static final Map<Integer, Money> CACHE = new HashMap<>();

    static {
        for (int d = 0; d <= CACHE_MAX; d++) {
            CACHE.put(d, new Money(d * 100L));
        }
    }

    private long cents;

    private Money(long cents) {
        this.cents = cents;
    }

    public static Money of(int dollars) {
        if (dollars >= 0 && dollars <= CACHE_MAX) {
            return CACHE.get(dollars);
        }
        return new Money(dollars * 100L);
    }

    public long cents() {
        return cents;
    }

    public Money withCents(long newCents) {
        this.cents = newCents;
        return this;
    }

    @Override
    public String toString() {
        return "Money(" + cents + ")";
    }
}
