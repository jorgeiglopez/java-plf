package plf.s02_oop_class_design.s02_10_design_patterns;

import java.util.List;

public final class Receipt {

    public static final class Item {
        private int priceCents;

        public Item(int priceCents) {
            this.priceCents = priceCents;
        }

        public int priceCents() {
            return priceCents;
        }

        public void setPriceCents(int priceCents) {
            this.priceCents = priceCents;
        }
    }

    private final List<Item> items;

    public Receipt(List<Item> items) {
        this.items = List.copyOf(items);
    }

    public List<Item> items() {
        return items;
    }

    public int totalCents() {
        int total = 0;
        for (Item item : items) {
            total += item.priceCents();
        }
        return total;
    }
}
