package plf.s06_concurrency.s06_08_futures_and_completablefuture;

import java.util.concurrent.CompletableFuture;

public final class OrderPipeline {

    public static CompletableFuture<Integer> priceOf(int orderId) {
        return CompletableFuture.completedFuture(orderId);
    }

    public static CompletableFuture<Integer> chargeAsync(int amount) {
        return CompletableFuture.supplyAsync(() -> {
            if (amount < 0) {
                throw new IllegalStateException("declined: " + amount);
            }
            return amount + 100;
        });
    }

    // ---- TASK: fix ONLY the composition below. The two methods above are correct. ----
    public static CompletableFuture<Integer> checkout(int orderId) {
        throw new UnsupportedOperationException("TODO");
    }

    public static CompletableFuture<Integer> checkoutWithRecovery(int orderId) {
        throw new UnsupportedOperationException("TODO");
    }
}
