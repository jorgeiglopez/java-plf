package plf.s06_concurrency.s06_06_synchronizers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class RelayRaceTest {

    // EXERCISE: starts red
    @Test
    void predictionMatchesActualTranscript() throws InterruptedException {
        List<String> actual = RelayRace.run();
        assertEquals(actual, RelayRacePredictions.predicted());
    }
}
