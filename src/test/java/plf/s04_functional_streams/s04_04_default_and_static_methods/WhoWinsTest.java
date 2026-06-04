package plf.s04_functional_streams.s04_04_default_and_static_methods;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WhoWinsTest {

    // EXERCISE: starts red
    @Test
    void predictWhoWins() {
        String predictedConcrete = "???"; // <-- write your prediction

        assertEquals(predictedConcrete, WhoWins.concreteCase());
    }

    // EXERCISE: starts red
    @Test
    void diamondResolvesToLeft() {
        assertEquals("L", Diamond.mergedId());
    }
}
