package plf.s04_functional_streams.s04_02_lambdas;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class CaptureLabTest {

    // EXERCISE: starts red
    @Test
    void predictBothPasses() {
        List<String> predictedPass1 = List.of("ab", "cd", "ef"); // <-- write your prediction
        List<String> predictedPass2 = List.of("ab", "cd", "ef"); // <-- write your prediction

        List<Supplier<String>> suppliers =
                CaptureLab.buildSuppliers(List.of("ab", "cd", "ef"));

        assertEquals(predictedPass1, CaptureLab.runPass(suppliers));
        assertEquals(predictedPass2, CaptureLab.runPass(suppliers));
    }
}
