package plf.s02_oop_class_design.s02_09_enums;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CalculatorTest {

    @Test
    void interfaceEnumStaysPolymorphic() {
        Calculator.Op op = Calculator.Basic.ADD;
        assertEquals(7.0, Calculator.run(op, 3, 4));
        assertEquals(8.0, Calculator.run(Calculator.Sci.POW, 2, 3));
    }

    @Test
    void enumSetIteratesInDeclarationOrderNotInsertionOrder() {
        EnumSet<Calculator.Basic> set =
                EnumSet.of(Calculator.Basic.MUL, Calculator.Basic.ADD);
        List<Calculator.Basic> order = new ArrayList<>(set);
        assertEquals(List.of(Calculator.Basic.ADD, Calculator.Basic.MUL), order);
    }

    // EXERCISE: starts red
    @Test
    void basicSymbolsAreOrdinalOrdered() {
        EnumMap<Calculator.Basic, String> symbols = Calculator.basicSymbols(); // <-- implement Task 2 to turn this green
        assertEquals("[+, -, *, /]", symbols.values().toString());
    }
}
