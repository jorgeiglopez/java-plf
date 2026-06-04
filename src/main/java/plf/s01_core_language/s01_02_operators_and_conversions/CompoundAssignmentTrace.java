package plf.s01_core_language.s01_02_operators_and_conversions;

public final class CompoundAssignmentTrace {

    private CompoundAssignmentTrace() {
    }

    // do not edit
    public static byte runTheLine() {
        byte b = 100;
        b += b += b;
        return b;
    }

    // do not edit
    public static int runTheVarLine() {
        var v = 100;
        v += v += v;
        return v;
    }

    // --- TODO: replace each throw with the value you predicted, as the right type. ---

    public static int r1_innerSum() {
        throw new UnsupportedOperationException("TODO: what is b + b, and in what type?");
    }

    public static byte r2_innerStored() {
        throw new UnsupportedOperationException("TODO: what byte does (byte)(b + b) become?");
    }

    public static int r3_outerSavedLhs() {
        throw new UnsupportedOperationException("TODO: what value of the outer b was saved first?");
    }

    public static byte r4_finalValue() {
        throw new UnsupportedOperationException("TODO: (byte)(savedLhs + innerStored) == ?");
    }
}
