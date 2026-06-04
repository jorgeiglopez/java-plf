package plf.s03_generics.s03_06_generics_and_varargs;

import java.util.List;

public final class UnsafeVarargs {

    private UnsafeVarargs() {
    }

    @SafeVarargs
    static <T> T[] toArray(T... args) {
        return args;
    }

    // ---- DO NOT change toArray above ----

    static <T> T[] pickTwo(T a, T b, T c) {
        return toArray(a, b);
    }

    static <T> List<T> pickTwoSafe(T a, T b, T c) {
        throw new UnsupportedOperationException("TODO"); // <-- Task 2 here
    }
}
