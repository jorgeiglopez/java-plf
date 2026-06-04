package plf.s02_oop_class_design.s02_05_nested_classes;

import java.lang.reflect.Field;
import java.util.List;

public final class Leak {

    private final byte[] payload;

    public Leak(int sizeBytes) {
        this.payload = new byte[sizeBytes];
    }

    public final class InnerTouchingNothing implements Runnable {
        @Override
        public void run() {
        }
    }

    public final class InnerTouchingPayload implements Runnable {
        @Override
        public void run() {
            int unused = payload.length;
        }
    }

    public static final class StaticNested implements Runnable {
        @Override
        public void run() {
        }
    }

    public Runnable lambdaLocalOnly() {
        int local = 7;
        return () -> {
            int unused = local;
        };
    }

    public Runnable lambdaTouchingPayload() {
        return () -> {
            int unused = payload.length;
        };
    }

    public List<Runnable> allFiveConstructs() {
        return List.of(
                new InnerTouchingNothing(),
                new InnerTouchingPayload(),
                new StaticNested(),
                lambdaLocalOnly(),
                lambdaTouchingPayload());
    }

    public static boolean capturesEnclosing(Object construct) {
        for (Field f : construct.getClass().getDeclaredFields()) {
            if (f.getType().equals(Leak.class)) {
                return true;
            }
        }
        return false;
    }

    public static String whyEmptyInnerDoesNotPin() {
        // <-- write your one-sentence answer here, then return it (replace the throw)
        throw new UnsupportedOperationException("TODO");
    }

    public static void main(String[] args) {
        Leak outer = new Leak(8);
        for (Runnable r : outer.allFiveConstructs()) {
            System.out.println(r.getClass().getName()
                    + " capturesEnclosing=" + capturesEnclosing(r));
        }
    }
}
