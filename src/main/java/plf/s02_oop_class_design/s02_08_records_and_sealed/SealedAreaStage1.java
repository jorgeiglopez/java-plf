package plf.s02_oop_class_design.s02_08_records_and_sealed;

public final class SealedAreaStage1 {

    sealed interface Shape permits Circle, Rectangle {
    }

    record Circle(double radius) implements Shape {
        Circle {
            if (radius <= 0) {
                throw new IllegalArgumentException("radius must be > 0, got " + radius);
            }
        }
    }

    record Rectangle(double width, double height) implements Shape {
    }

    // <-- add `Triangle` to `permits` ONLY in your head / in PROBLEM.md, NOT here
    static double area(Shape shape) {
        double result = switch (shape) {
            case Circle c -> Math.PI * c.radius() * c.radius();
            case Rectangle r -> r.width() * r.height();
        };
        return result;
    }
}
