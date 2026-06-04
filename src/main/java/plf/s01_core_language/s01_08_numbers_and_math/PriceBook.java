package plf.s01_core_language.s01_08_numbers_and_math;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class PriceBook {

    private PriceBook() { }

    public static BigDecimal roundedTotal(List<Double> prices) {
        BigDecimal total = BigDecimal.ZERO;
        for (double p : prices) {
            total = total.add(new BigDecimal(p)); // <-- BUG 1
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    public static boolean sameAmount(BigDecimal a, BigDecimal b) {
        return a.equals(b); // <-- BUG 2
    }

    public static BigDecimal perPersonShare(BigDecimal total, int people) {
        return total.divide(BigDecimal.valueOf(people)); // <-- BUG 3
    }
}
