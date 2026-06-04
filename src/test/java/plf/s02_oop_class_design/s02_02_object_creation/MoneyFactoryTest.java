package plf.s02_oop_class_design.s02_02_object_creation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

// EXERCISE: starts red
@DisplayName("Object creation: a caching factory hands you a shared instance")
class MoneyFactoryTest {

    // ---- EDIT ONLY THESE FOUR LINES ----
    static final boolean PREDICT_NEW_EQ_NEW = true;
    static final boolean PREDICT_OF_EQ_OF = false;
    static final long PREDICT_FRESH_CENTS_AFTER_MUTATION = 500;
    static final long PREDICT_CACHED_CENTS_AFTER_MUTATION = 500;
    // ------------------------------------

    @Test
    @DisplayName("new always makes a fresh object; of(small) reuses a cached one")
    void identityContractDiffersByCreationPath() {
        assertEquals(PREDICT_NEW_EQ_NEW, freshFive() == freshFive(),
                "REVEAL: two `new` results are never == (a constructor must return a fresh object)");

        assertEquals(PREDICT_OF_EQ_OF, Money.of(5) == Money.of(5),
                "REVEAL: of(5) == of(5) is true (the factory cached 0..10 and returns the same instance)");
    }

    @Test
    @DisplayName("the SAME fluent call is safe via `new`, poisonous via the cached factory")
    void sameCallSiteOneSurvives() {
        Money fresh = freshFive().withCents(99);
        assertEquals(99, fresh.cents(), "the object you just built reflects the mutation");
        assertEquals(PREDICT_FRESH_CENTS_AFTER_MUTATION, freshFive().cents(),
                "REVEAL: a brand-new five is still 500 — nothing shared was touched");

        Money poisoned = Money.of(5).withCents(99);
        assertEquals(99, poisoned.cents(), "the returned reference reflects the mutation");
        assertSame(Money.of(5), poisoned, "of(5) and `poisoned` are the very same object");
        assertEquals(PREDICT_CACHED_CENTS_AFTER_MUTATION, Money.of(5).cents(),
                "REVEAL: every of(5) in the whole JVM now reads 99 — the shared five is corrupted");
    }

    @Test
    @DisplayName("large values are not cached, so of(500) behaves like new")
    void cacheBoundaryIsObservable() {
        assertFalse(Money.of(500) == Money.of(500),
                "of(500) is above the cache ceiling, so each call builds a fresh object");
        assertTrue(Money.of(10) == Money.of(10),
                "of(10) is the last cached value");
    }

    private static Money freshFive() {
        return Money.of(500).withCents(500);
    }
}
