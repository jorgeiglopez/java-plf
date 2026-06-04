package plf.s01_core_language.s01_02_operators_and_conversions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Do NOT edit this test. Make it green by editing CompoundAssignmentTrace.
class CompoundAssignmentTraceTest {

    @Test
    void theLineEvaluatesTo44() {
        assertEquals((byte) 44, CompoundAssignmentTrace.runTheLine(),
                "byte b = 100; b += b += b; must be 44");
    }

    @Test
    void r1_innerSumIsIntNotByte() {
        assertEquals(200, CompoundAssignmentTrace.r1_innerSum(),
                "R1: b + b is an int expression, value 200");
    }

    @Test
    void r2_innerCastWrapsViaSignBit() {
        assertEquals((byte) -56, CompoundAssignmentTrace.r2_innerStored(),
                "R2: (byte)200 == -56, not low-byte truncation to a positive number");
    }

    @Test
    void r3_outerSavedLhsIs100NotMinus56() {
        assertEquals(100, CompoundAssignmentTrace.r3_outerSavedLhs(),
                "R3: outer saved LHS is 100 (read before RHS), not -56");
    }

    @Test
    void r4_finalCastIs44() {
        assertEquals((byte) 44, CompoundAssignmentTrace.r4_finalValue(),
                "R4: (byte)(100 + -56) == 44");
    }

    @Test
    void varScaleUpInfersIntSoNoNarrowing() {
        assertEquals(300, CompoundAssignmentTrace.runTheVarLine(),
                "Scale-up: var infers int, so the answer is 300 with no narrowing");
    }
}
