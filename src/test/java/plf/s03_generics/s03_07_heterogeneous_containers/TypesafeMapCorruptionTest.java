package plf.s03_generics.s03_07_heterogeneous_containers;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TypesafeMapCorruptionTest {

    // EXERCISE: starts red
    @Test
    void slotStaysAListOfStringsAfterProcessing() {
        TypesafeMap registry = new TypesafeMap();
        registry.put(List.class, List.of("alpha", "beta", "gamma"));

        NameProcessor.totalLength(registry);

        List<String> names = registry.get(List.class);
        List<String> shouted = new ArrayList<>();
        assertDoesNotThrow(() -> names.forEach(n -> shouted.add(n.toUpperCase())));
        assertEquals(List.of("ALPHA", "BETA", "GAMMA"), shouted);
    }
}
