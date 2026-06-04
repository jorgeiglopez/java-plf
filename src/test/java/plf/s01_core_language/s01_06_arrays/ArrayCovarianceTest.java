package plf.s01_core_language.s01_06_arrays;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArrayCovarianceTest {

    @Test
    void covariantStore_throwsArrayStoreException_withOffendingClassName() {
        assertEquals("java.lang.Integer", ArrayCovariance.triggerArrayStore());
    }

    @Test
    void arrayComponentType_isReified() {
        assertSame(String[].class, ArrayCovariance.runtimeComponentType());
    }

    @Test
    void storingAStringValueIntoCovariantArray_isFine() {
        assertEquals("java.lang.Integer", ArrayCovariance.triggerArrayStore());
    }

    // EXERCISE: this test starts red.
    @Test
    void whyNoGenericArray_isCommitted() {
        String answer = ArrayCovariance.whyNoGenericArray();
        assertEquals(true, answer != null && !answer.isBlank(),
                "complete ArrayCovariance.whyNoGenericArray() with your derivation");
    }
}
