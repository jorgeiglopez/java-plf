package plf.s01_core_language.s01_06_arrays;

public final class ArrayCovariance {

    private ArrayCovariance() {
    }

    public static String triggerArrayStore() {
        Object[] a = new String[2];
        a[1] = "ok";
        try {
            a[0] = Integer.valueOf(42);
            throw new AssertionError("expected ArrayStoreException, none thrown");
        } catch (ArrayStoreException e) {
            return e.getMessage();
        }
    }

    public static Class<?> runtimeComponentType() {
        Object[] a = new String[0];
        return a.getClass();
    }

    public static String whyNoGenericArray() {
        // TODO: replace this stub with your one-sentence derivation and return it.
        throw new UnsupportedOperationException("TODO: complete whyNoGenericArray()");
    }

    public static void main(String[] args) {
        System.out.println("runtime component type: " + runtimeComponentType().getName());
        System.out.println("ArrayStoreException message: " + triggerArrayStore());
    }
}
