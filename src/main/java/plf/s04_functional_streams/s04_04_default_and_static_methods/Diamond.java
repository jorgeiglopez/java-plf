package plf.s04_functional_streams.s04_04_default_and_static_methods;

public final class Diamond {

    interface Left {
        default String id() {
            return "L";
        }
    }

    interface Right {
        default String id() {
            return "R";
        }
    }

    // Inheriting both defaults is a compile error until id() is resolved here.
    static final class Merged implements Left, Right {
        @Override
        public String id() {
            throw new UnsupportedOperationException("TODO: return Left's id via the Interface.super syntax"); // <-- Task here
        }
    }

    static String mergedId() {
        return new Merged().id();
    }
}
