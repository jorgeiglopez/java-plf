package plf.s04_functional_streams.s04_08_optional;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserLookupTest {

    private UserLookup lookup() {
        return new UserLookup(List.of(
                new UserLookup.User("u1", "ann@x.io", Optional.of("Q")),
                new UserLookup.User("u2", "", Optional.empty())));
    }

    @Test
    void presentEmailIsReturned() {
        UserLookup l = lookup();
        assertEquals("ann@x.io", l.emailOrAudit("u1"));
    }

    @Test
    void blankOrMissingFallsBackToAudit() {
        UserLookup l = lookup();
        assertEquals("", l.emailOrAudit("u2"));
        assertEquals("", l.emailOrAudit("nope"));
    }

    @Test
    void middleNamePresent() {
        assertEquals(Optional.of("Q"), lookup().middleNameOf("u1"));
    }

    @Test
    void middleNameAbsentUser() {
        assertEquals(Optional.empty(), lookup().middleNameOf("nope"));
    }

    // EXERCISE: starts red
    @Test
    void presentEmailMustNotTouchAuditLog() {
        UserLookup l = lookup();
        l.emailOrAudit("u1");
        assertEquals(0, l.auditCalls(),
                "audit() must not run when the email is present");
    }
}
