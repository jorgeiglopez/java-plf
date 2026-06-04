package plf.s05_collections.s05_07_immutable_and_sequenced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedCollection;
import org.junit.jupiter.api.Test;

class CollectionLensTest {

    // EXERCISE: starts red
    @Test
    void auditReturnsAStableSnapshot() {
        var mutable = new ArrayList<>(List.of("a", "b"));
        SequencedCollection<String> snapshot = CollectionLens.audit(mutable);

        mutable.add("c");   // caller keeps mutating AFTER the audit

        assertEquals(List.of("a", "b"), List.copyOf(snapshot));
    }

    @Test
    void coldOpen_addFirstOnImmutableList() {
        SequencedCollection<String> immutable = List.of("a", "b");

        // <-- Task 1: replace null with the exception class you predict, then run
        Class<? extends Throwable> predicted = null;

        assertThrows(predicted, () -> immutable.addFirst("z"));
    }

    @Test
    void killShot_reversedRemapsYourVerbs() {
        var m = new LinkedHashMap<String, Integer>();
        m.put("a", 1);
        m.put("b", 2);

        var reversed = m.reversed();
        m.put("c", 3);
        reversed.putFirst("d", 4);

        // <-- Task 2: replace the placeholders with the exact final state you predict
        assertEquals("c", reversed.firstEntry().getKey());
        assertEquals(List.of("REPLACE_ME"), List.copyOf(m.sequencedKeySet()));
    }
}
