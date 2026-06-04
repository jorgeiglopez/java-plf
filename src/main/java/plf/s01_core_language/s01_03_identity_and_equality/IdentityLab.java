package plf.s01_core_language.s01_03_identity_and_equality;

public final class IdentityLab {

    private IdentityLab() { }

    public static void f(StringBuilder x, StringBuilder y) {
        x.append("!");
        y = x;
    }

    public static void main(String[] args) {
        StringBuilder a = new StringBuilder("A");
        StringBuilder b = new StringBuilder("B");
        StringBuilder origA = a;
        StringBuilder origB = b;

        f(a, b);

        System.out.println("1) a            = \"" + a + "\"");
        System.out.println("2) b            = \"" + b + "\"");
        System.out.println("3) a == origA   -> " + (a == origA));
        System.out.println("4) b == origB   -> " + (b == origB));
        System.out.println("5) a.equals(b)  -> " + a.equals(b));
    }
}
