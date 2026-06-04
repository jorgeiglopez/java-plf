package plf.s03_generics.s03_01_generic_types_and_methods;

public final class MaxBound {

    private MaxBound() {
    }

    public static <T extends Comparable<T>> T max(T a, T b) {
        return a.compareTo(b) >= 0 ? a : b;
    }

    // ---- EDIT ONLY THIS METHOD ----
    public static String maxOfTwoStrings() {
        throw new UnsupportedOperationException("TODO"); // <-- Task 2 here
    }
}
