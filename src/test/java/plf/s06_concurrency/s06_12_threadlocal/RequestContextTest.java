package plf.s06_concurrency.s06_12_threadlocal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RequestContextTest {

    private ExecutorService pool;

    @BeforeEach
    void setUp() {
        pool = Executors.newSingleThreadExecutor();
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    void leakedIdentity_demonstratesTheBug() {
        assertEquals("alice", RequestContext.handleLogin(pool, "alice"));
        assertEquals("alice", RequestContext.handleAnonymousRead(pool));
    }

    // EXERCISE: starts red
    @Test
    void safeLogin_reArmsTheInitializer() {
        assertEquals("alice", RequestContext.handleLoginSafely(pool, "alice"));
        assertEquals("anonymous", RequestContext.handleAnonymousRead(pool));
    }
}
