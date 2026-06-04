package plf.s02_oop_class_design.s02_04_interfaces_and_abstract_classes;

interface AreaDrawable {
    default double area() {
        return 0.0;
    }
}

interface AreaMeasurable {
    default double area() {
        return 1.0;
    }
}

class AreaResolution implements AreaDrawable, AreaMeasurable {

    @Override
    public double area() {
        throw new UnsupportedOperationException("TODO"); // <-- resolve the inherited-default conflict here
    }
}
