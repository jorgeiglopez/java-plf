package plf.s02_oop_class_design.s02_06_method_design;

import java.time.LocalDate;
import java.util.List;

public final class Schedule {

    private final List<LocalDate> days;
    private final List<Appointment> appointments;

    public Schedule(List<LocalDate> days, List<Appointment> appointments) {
        this.days = List.copyOf(days);
        this.appointments = List.copyOf(appointments); // <-- fix this arm
    }

    public List<LocalDate> days() {
        return days;
    }

    public List<Appointment> appointments() {
        return appointments;                           // <-- fix this arm
    }
}
