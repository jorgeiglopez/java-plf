package plf.s02_oop_class_design.s02_03_class_design_principles;

import java.util.Date;

public class Period {

    public final Date start;   // <-- Task 2 (accessibility): make private
    public final Date end;     // <-- Task 2 (accessibility): make private

    public Period(Date start, Date end) {
        if (start.after(end)) {
            throw new IllegalArgumentException(start + " after " + end);
        }
        this.start = start;    // <-- Task 1 (inbound door): copy the argument here
        this.end = end;        // <-- Task 1 (inbound door): copy the argument here
    }

    public Date getStart() {
        return new Date(start.getTime());
    }

    public Date getEnd() {
        return new Date(end.getTime());
    }
}
