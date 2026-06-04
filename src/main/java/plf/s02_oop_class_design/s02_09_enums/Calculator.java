package plf.s02_oop_class_design.s02_09_enums;

import java.util.EnumMap;

public final class Calculator {

    public interface Op {
        double apply(double a, double b);
    }

    public enum Basic implements Op {
        ADD { public double apply(double a, double b) { return a + b; } },
        SUB { public double apply(double a, double b) { return a - b; } },
        MUL { public double apply(double a, double b) { return a * b; } },
        DIV { public double apply(double a, double b) { return a / b; } };
    }

    public enum Sci implements Op {
        LOG { public double apply(double a, double b) { return Math.log(a) / Math.log(b); } },
        POW { public double apply(double a, double b) { return Math.pow(a, b); } };
    }

    // <-- Task 2: build an EnumMap<Basic,String> of symbols, iteration in declaration order
    public static EnumMap<Basic, String> basicSymbols() {
        throw new UnsupportedOperationException("TODO");
    }

    public static double run(Op op, double a, double b) {
        return op.apply(a, b);
    }

    private Calculator() {
    }
}
