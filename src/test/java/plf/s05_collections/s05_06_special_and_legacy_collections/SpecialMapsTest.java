package plf.s05_collections.s05_06_special_and_legacy_collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import plf.s05_collections.s05_06_special_and_legacy_collections.SpecialMaps.Day;

class SpecialMapsTest {

    // EXERCISE: starts red
    @Test
    void equalMapsRegardlessOfType() {
        boolean predicted = false; // <-- write your prediction

        Map<Day, String> em = SpecialMaps.enumMap();
        Map<Day, String> copy = SpecialMaps.hashCopyOf(em);

        assertEquals(predicted, SpecialMaps.equalsAcrossTypes(em, copy));
    }

    // EXERCISE: starts red
    @Test
    void enumMapIteratesInOrdinalOrder() {
        List<Day> predicted = List.of(); // <-- write your prediction

        assertEquals(predicted, SpecialMaps.iterationOrder(SpecialMaps.enumMap()));
    }

    // EXERCISE: starts red
    @Test
    void identityHashMapCountsByReference() {
        int predicted = 0; // <-- write your prediction

        assertEquals(predicted, SpecialMaps.identityKeyCount());
    }

    // EXERCISE: starts red
    @Test
    void nullValueIsStillAPresentKey() {
        boolean predicted = false; // <-- write your prediction

        assertEquals(predicted, SpecialMaps.nullValueIsPresent());
    }
}
