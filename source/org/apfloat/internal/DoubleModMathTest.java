package org.example;

import org.apfloat.internal.DoubleModMath;
import org.junit.Test;

public class TestModPow {
    @Test
    public void testModPow() {
        DoubleModMath dmm = new DoubleModMath();
        dmm.setModulus(1);
        double result = dmm.modPow(4, -4);
        // some assertions are omitted.
    }
}

