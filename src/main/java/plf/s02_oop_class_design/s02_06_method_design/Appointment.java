package plf.s02_oop_class_design.s02_06_method_design;

import java.time.LocalTime;
import java.util.Objects;

public final class Appointment {

    private LocalTime time;

    public Appointment(LocalTime time) {
        this.time = Objects.requireNonNull(time, "time");
    }

    public Appointment(Appointment other) {       // <-- copy constructor (use it in the fix)
        this.time = other.time;
    }

    public LocalTime getTime() {
        return time;
    }

    public void setTime(LocalTime time) {
        this.time = Objects.requireNonNull(time, "time");
    }

    @Override
    public String toString() {
        return "Appointment[" + time + "]";
    }
}
