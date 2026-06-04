package plf.s02_oop_class_design.s02_04_interfaces_and_abstract_classes;

// EXERCISE: starts red
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AreaResolutionTest {

    @Test
    void resolvesToMeasurableDefault() {
        AreaResolution t = new AreaResolution();
        assertEquals(1.0, t.area()); // <-- make AreaResolution.area() pick AreaMeasurable's default
    }
}
