package plf.s02_oop_class_design.s02_08_records_and_sealed;

public final class SealedAreaStage2 {

    sealed interface Shape permits Circle, Rectangle {   // <-- Task 2: add Triangle here
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

    // <-- Task 2: declare `record Triangle(double base, double height) implements Shape {}`

    static double area(Shape shape) {
        return switch (shape) {
            case Circle c -> Math.PI * c.radius() * c.radius();
            case Rectangle r -> r.width() * r.height();
            default -> 0.0;
        };
    }
}
