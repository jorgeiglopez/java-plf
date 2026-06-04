package plf.s02_oop_class_design.s02_10_design_patterns;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// EXERCISE: this test starts red.
@DisplayName("Design Patterns: the defensive copy that locked the wrong thing")
class ReceiptTest {

    enum Outcome { THROWS, IGNORED, TOTAL_CHANGES }

    // ---- EDIT ONLY THESE TWO LINES ----
    static final Outcome PREDICTION = Outcome.THROWS;      // <-- name the lie first
    static final int PREDICT_TOTAL_AFTER_MUTATION = 350;   // <-- then the number
    // ------------------------------------

    @Test
    @DisplayName("the unmodifiable spine is the bait")
    void spineIsLocked() {
        List<Receipt.Item> source = new ArrayList<>();
        source.add(new Receipt.Item(100));
        source.add(new Receipt.Item(250));
        Receipt receipt = new Receipt(source);

        assertThrows(UnsupportedOperationException.class,
                () -> receipt.items().add(new Receipt.Item(999)));

        source.add(new Receipt.Item(500));
        assertEquals(350, receipt.totalCents());
    }

    @Test
    @DisplayName("mutate an element through the ORIGINAL reference")
    void elementLeaksThroughSharedReference() {
        List<Receipt.Item> source = new ArrayList<>();
        source.add(new Receipt.Item(100));
        source.add(new Receipt.Item(250));
        Receipt receipt = new Receipt(source);

        Outcome actual;
        int totalAfter;
        try {
            source.get(0).setPriceCents(100_000);
            totalAfter = receipt.totalCents();
            actual = (totalAfter == 350) ? Outcome.IGNORED : Outcome.TOTAL_CHANGES;
        } catch (UnsupportedOperationException e) {
            actual = Outcome.THROWS;
            totalAfter = 350;
        }

        assertEquals(PREDICTION, actual);
        assertEquals(PREDICT_TOTAL_AFTER_MUTATION, totalAfter);
    }
}
