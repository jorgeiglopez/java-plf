package plf.s04_functional_streams.s04_03_method_references;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public final class ReceiverSlotLab {

    private ReceiverSlotLab() {
    }

    // Beat 1 - arity shift. Same method name, two receiver slots.
    public static Function<String, String> unboundUpper() {
        throw new UnsupportedOperationException("TODO"); // <-- Beat 1a
    }

    public static Supplier<String> boundUpper(String captured) {
        throw new UnsupportedOperationException("TODO"); // <-- Beat 1b
    }

    // Beat 2 - eager capture (snapshot). Wire a Consumer that adds to `target`
    // via a method reference, then return it. The caller will reassign the
    // variable it passed; the snapshot must keep pointing at the original list.
    public static java.util.function.Consumer<String> adderFor(List<String> target) {
        throw new UnsupportedOperationException("TODO"); // <-- Beat 2
    }

    // Beat 3 - array constructor ref. Collect the stream into a String[].
    public static String[] toStringArray(java.util.stream.Stream<String> stream) {
        throw new UnsupportedOperationException("TODO"); // <-- Beat 3
    }

    // Beat 3 helper - the IntFunction the array ctor ref must satisfy.
    public static IntFunction<String[]> arrayMaker() {
        throw new UnsupportedOperationException("TODO"); // <-- Beat 3 (witness)
    }

    public static void main(String[] args) {
        List<String> first = new ArrayList<>();
        var adder = adderFor(first);
        List<String> second = new ArrayList<>();
        adder.accept("x");
        System.out.println("first  = " + first);
        System.out.println("second = " + second);
    }
}
