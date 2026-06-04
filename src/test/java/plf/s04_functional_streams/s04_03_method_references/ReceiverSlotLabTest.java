package plf.s04_functional_streams.s04_03_method_references;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ReceiverSlotLabTest {

    // EXERCISE: starts red
    @Test
    void beat1_arityShift() {
        String predictUnbound = "?"; // <-- predict ReceiverSlotLab.unboundUpper().apply("hi")
        String predictBound = "?";   // <-- predict ReceiverSlotLab.boundUpper("hi").get()

        assertEquals(predictUnbound, ReceiverSlotLab.unboundUpper().apply("hi"));
        assertEquals(predictBound, ReceiverSlotLab.boundUpper("hi").get());
    }

    // EXERCISE: starts red
    @Test
    void beat2_eagerCaptureSnapshot() {
        List<String> original = new ArrayList<>();
        Consumer<String> adder = ReceiverSlotLab.adderFor(original);

        List<String> reassigned = new ArrayList<>();
        adder.accept("x");

        List<String> predictOriginal = List.of();   // <-- which list got "x"?
        List<String> predictReassigned = List.of();  // <-- and which stayed empty?

        assertEquals(predictOriginal, original);
        assertEquals(predictReassigned, reassigned);
    }

    // EXERCISE: starts red
    @Test
    void beat3_arrayConstructorRef() {
        String[] predict = {}; // <-- predict the collected array

        assertArrayEquals(predict, ReceiverSlotLab.toStringArray(Stream.of("a", "b", "c")));
    }
}
