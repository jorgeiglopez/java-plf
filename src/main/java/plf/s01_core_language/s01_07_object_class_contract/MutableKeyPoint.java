package plf.s01_core_language.s01_07_object_class_contract;

import java.util.Objects;

// Do NOT "fix" this by changing equals/hashCode — they are already correct. See PROBLEM.md.
public final class MutableKeyPoint {

    int x;
    int y;

    public MutableKeyPoint(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MutableKeyPoint p)) {
            return false;
        }
        return x == p.x && y == p.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "MutableKeyPoint(" + x + ", " + y + ")";
    }
}
