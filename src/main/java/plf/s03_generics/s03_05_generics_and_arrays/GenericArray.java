package plf.s03_generics.s03_05_generics_and_arrays;

import java.lang.reflect.Array;
import java.util.Arrays;

public final class GenericArray {

    private GenericArray() {
    }

    // ---- DO NOT CHANGE: this is the trap under study ----
    @SuppressWarnings("unchecked")
    public static <T> T[] fillBroken(T item, int count) {
        Object[] arr = new Object[count];
        Arrays.fill(arr, item);
        return (T[]) arr;
    }

    public static <T> T[] fillSafe(Class<T> componentType, T item, int count) {
        throw new UnsupportedOperationException("TODO"); // <-- Task 2 here
    }
}
