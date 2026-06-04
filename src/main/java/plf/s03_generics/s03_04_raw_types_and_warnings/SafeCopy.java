package plf.s03_generics.s03_04_raw_types_and_warnings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class SafeCopy {

    private SafeCopy() {
    }

    // ---- EDIT ONLY copyOf (body and the suppression) ----
    @SuppressWarnings("unchecked")
    public static <T> List<T> copyOf(Class<T> type, Collection<?> src) {
        return (List<T>) new ArrayList<Object>(src);   // <-- Task 2 & 3 here
    }
    // -----------------------------------------------------

    // do not modify processFirst
    public static <T extends CharSequence> int processFirst(List<T> items) {
        T first = items.get(0);
        return first.length();
    }
}
