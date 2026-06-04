package plf.s04_functional_streams.s04_01_functional_interfaces;

public final class SamProbe {

    static final int ABSTRACT_METHOD_COUNT_OF_TRAP = 1;   // <-- Task 2: predict the real count

    @FunctionalInterface
    interface PlainSam<T> {
        boolean test(T t);
    }

    @FunctionalInterface
    interface SamWithDefaults<T> {
        boolean test(T t);
        default boolean negate(T t) { return !test(t); }
        default SamWithDefaults<T> self() { return this; }
    }

    @FunctionalInterface
    interface SamWithObjectMethods<T> {
        boolean test(T t);
        boolean equals(Object o);
        int hashCode();
        String toString();
    }

    static boolean isEmptyViaPlain(String s) {
        throw new UnsupportedOperationException("TODO Task 1: target PlainSam with a lambda, then call test");
    }

    static boolean isEmptyViaDefaults(String s) {
        SamWithDefaults<String> p = str -> str.isEmpty();
        return p.test(s);
    }

    static boolean isEmptyViaObjectMethods(String s) {
        SamWithObjectMethods<String> p = str -> str.isEmpty();
        return p.test(s);
    }
}
