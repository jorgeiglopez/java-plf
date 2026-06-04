package plf.s03_generics.s03_03_bounded_types_and_wildcards;

// EXERCISE: starts red

import static org.junit.jupiter.api.Assertions.assertEquals;
import static plf.s03_generics.s03_03_bounded_types_and_wildcards.Transfer.copy;
import static plf.s03_generics.s03_03_bounded_types_and_wildcards.Transfer.transfer;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TransferTest {

    @Test
    void callA_intsToNumbers_compilesAndMovesFirstElement() {
        List<Integer> ints = new ArrayList<>(List.of(1, 2, 3));
        List<Number> nums = new ArrayList<>();

        transfer(ints, nums);

        assertEquals(List.of(1), nums);
    }

    @Test
    void callC_sameListBothRoles_compilesAndDuplicatesFirstElement() {
        List<Number> nums = new ArrayList<>(List.of(7, 8, 9));

        transfer(nums, nums);

        assertEquals(List.of(7, 8, 9, 7), nums);
    }

    @Test
    void copy_movesEveryElementFromProducerToConsumer() {
        List<Integer> src = new ArrayList<>(List.of(1, 2, 3));
        List<Number> dst = new ArrayList<>();

        copy(src, dst);

        assertEquals(List.of(1, 2, 3), dst);
    }
}
