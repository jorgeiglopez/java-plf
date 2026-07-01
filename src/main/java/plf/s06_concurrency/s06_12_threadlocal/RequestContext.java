package plf.s06_concurrency.s06_12_threadlocal;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public final class RequestContext {

    static final ThreadLocal<String> CURRENT_USER =
            ThreadLocal.withInitial(() -> "anonymous");

    private RequestContext() {
    }

    public static String handleLogin(ExecutorService pool, String userId) {
        Future<String> f = pool.submit(() -> {
            CURRENT_USER.set(userId);
            return CURRENT_USER.get();
        });
        return await(f);
    }

    public static String handleAnonymousRead(ExecutorService pool) {
        Future<String> f = pool.submit(CURRENT_USER::get); // <-- no set(); whom does this see?
        return await(f);
    }

    public static String handleLoginSafely(ExecutorService pool, String userId) {
        Future<String> f = pool.submit(() -> {
            try {
                CURRENT_USER.set(userId);
                return CURRENT_USER.get();
            } finally {
                CURRENT_USER.remove();
            }
        });
        return await(f);
    }

    private static String await(Future<String> f) {
        try {
            return f.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
