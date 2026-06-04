package plf.s05_collections.s05_05_ordering;

import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

public final class Points {

    public record Point(int x, int y) implements Comparable<Point> {

        public int distSq() {
            return x * x + y * y;
        }

        @Override
        public int compareTo(Point o) {
            return Comparator.comparingInt(Point::distSq)
                    .thenComparingInt(Point::x)
                    .thenComparingInt(Point::y)
                    .compare(this, o);
        }
    }

    public static final List<Point> FOUR = List.of(
            new Point(1, 5),   // distSq 26
            new Point(5, 1),   // distSq 26
            new Point(3, 4),   // distSq 25
            new Point(2, 2));  // distSq 8

    public static TreeSet<Point> naturalOrdered() {
        return new TreeSet<>(FOUR);
    }

    // ---- EDIT ONLY THE COMPARATOR PASSED HERE (Task 2) ----
    public static Comparator<Point> byDistance() {
        return Comparator.comparingInt(Point::distSq);
    }

    public static TreeSet<Point> comparatorOrdered() {
        TreeSet<Point> set = new TreeSet<>(byDistance());
        set.addAll(FOUR);
        return set;
    }

    public static void main(String[] args) {
        System.out.println("natural    size = " + naturalOrdered().size());
        System.out.println("comparator size = " + comparatorOrdered().size());
        System.out.println("natural    = " + naturalOrdered());
        System.out.println("comparator = " + comparatorOrdered());
    }
}
