package plf.s01_core_language.s01_05_variables_and_initialization;

public final class Construction {

    public abstract static class Animal {
        public final String snapshotInCtor;

        protected Animal() {
            this.snapshotInCtor =
                    "legs=" + legs() + " name=" + name() + " weight=" + weight();
        }

        public abstract int legs();

        public abstract String name();

        public abstract int weight();
    }

    public static final class Spider extends Animal {
        public final int legs = 8;

        public String name = "tarantula";

        public final int weight = computeWeight();

        @Override
        public int legs() {
            return legs;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public int weight() {
            return weight;
        }

        private static int computeWeight() {
            return 5;
        }
    }

    private Construction() {
    }

    public static void main(String[] args) {
        Spider s = new Spider();
        System.out.println("ctor saw   -> " + s.snapshotInCtor);
        System.out.println("after ctor -> legs=" + s.legs()
                + " name=" + s.name() + " weight=" + s.weight());
    }
}
