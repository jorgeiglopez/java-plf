package plf.s03_generics.s03_02_type_erasure;

public final class ErasedAccess {

    private ErasedAccess() {
    }

    public static <T> T firstElement(java.util.List<T> list) {
        return (T) list.get(0); // ---- DO NOT MODIFY ----
    }

    public static <T> T firstElementSafe(java.util.List<T> list, Class<T> clazz) { // <-- Task 2 here
        throw new UnsupportedOperationException("TODO");
    }
}
