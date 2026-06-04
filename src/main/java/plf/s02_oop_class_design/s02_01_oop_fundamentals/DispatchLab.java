package plf.s02_oop_class_design.s02_01_oop_fundamentals;

public final class DispatchLab {

    private DispatchLab() { }

    static class Animal {
        // CONSTRAINT: keep EXACTLY greet(Object) + greet(String). Do not add a third overload. // <-- do not touch
        private final String species = "Animal";

        String speak() {
            return species + " makes a generic noise";
        }

        String greet(Object o) {
            return "greet(Object): " + o;
        }

        String greet(String s) {
            return "greet(String): " + s;
        }

        Animal reproduce() {
            return new Animal();
        }
    }

    static class Dog extends Animal {
        @Override
        String speak() {
            return "Woof";
        }

        @Override
        Dog reproduce() {
            return new Dog();
        }
    }

    static class Cat extends Animal {
        @Override
        String speak() {
            return "Meow";
        }
    }

    public static void main(String[] args) {
        Animal a = new Dog();

        System.out.println("1) a.speak()                 -> " + a.speak());
        System.out.println("2) a.greet(\"hi\")             -> " + a.greet("hi"));
        System.out.println("3) a.greet(null)             -> " + a.greet(null));

        var x = a.reproduce(); // <-- static type of x is ??? (predict it)
        System.out.println("4) a.reproduce() runtime obj -> " + x.getClass().getSimpleName());

        try {
            Cat c = (Cat) a; // <-- compiles; predict its fate at runtime
            System.out.println("5) (Cat) a                   -> " + c.speak());
        } catch (ClassCastException e) {
            System.out.println("5) (Cat) a                   -> threw " + e.getClass().getSimpleName());
        }
    }
}
