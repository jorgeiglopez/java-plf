package plf.s06_concurrency.s06_08_futures_and_completablefuture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderPipelineTest {

    // EXERCISE: starts red
    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void checkoutReturnsTheChargedAmountNotANestedFuture() {
        CompletableFuture<Integer> result = OrderPipeline.checkout(42);

        assertEquals(142, result.join());
    }

    // EXERCISE: starts red
    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void declinedChargeMakesTheOuterFutureFail() {
        CompletableFuture<Integer> result = OrderPipeline.checkout(-1);

        CompletionException ex = assertThrows(CompletionException.class, result::join);
        assertInstanceOf(IllegalStateException.class, ex.getCause());
    }

    // EXERCISE: starts red
    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void recoveryHandlerActuallyFiresOnDecline() {
        CompletableFuture<Integer> result = OrderPipeline.checkoutWithRecovery(-1);

        assertEquals(-999, result.join());
    }
}
