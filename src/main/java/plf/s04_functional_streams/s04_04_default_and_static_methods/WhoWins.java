package plf.s04_functional_streams.s04_04_default_and_static_methods;

interface Logger {
    default String format() {
        return "[default]";
    }
}

class ConcreteBase {
    public String format() {
        return "[parent]";
    }
}

public final class WhoWins {

    static final class MyService extends ConcreteBase implements Logger {
    }

    static String concreteCase() {
        return new MyService().format();
    }
}
