package plf.s03_generics.s03_03_bounded_types_and_wildcards;

import java.util.List;

public final class Transfer {

    private Transfer() {
    }

    // ---- DO NOT EDIT: this method is the subject of the prediction table ----
    static <T extends Number> void transfer(List<? extends T> src, List<? super T> dst) {
        dst.add(src.get(0));
    }
    // ---- END DO NOT EDIT ----

    static void copy(List<? extends Number> src, List<? super Number> dst) {
        throw new UnsupportedOperationException("TODO"); // <-- Task 3 here
    }
}
