package plf.s01_core_language.s01_05_variables_and_initialization;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ConstructionTest {

    @Test
    void constructorSeesPreInitializationValues() {
        Construction.Spider s = new Construction.Spider();
        assertEquals("legs=8 name=null weight=0", s.snapshotInCtor);
    }

    @Test
    void afterConstructionEverythingIsInitialized() {
        Construction.Spider s = new Construction.Spider();
        assertEquals(8, s.legs());
        assertEquals("tarantula", s.name());
        assertEquals(5, s.weight());
    }
}
