package plf.s06_concurrency.s06_02_thread_safety_and_locking;

public final class SeatBooking {

    private final int capacity;
    private int booked;

    public SeatBooking(int capacity) {
        this.capacity = capacity;
    }

    public synchronized int booked() {
        return booked;
    }

    public synchronized void add() {
        booked++;
    }

    // ---- IMPLEMENT THIS ONE METHOD ----
    public boolean tryBook() {
        // <-- Task: make this compound action atomic so booked never exceeds capacity
        if (booked() < capacity) {
            add();
            return true;
        }
        return false;
    }
    // -----------------------------------
}
