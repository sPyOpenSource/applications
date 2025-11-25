
package com.gazman.math;

import java.math.BigInteger;

/**
 * Source: http://stackoverflow.com/questions/4407839/how-can-i-find-the-square-root-of-a-java-biginteger
 */

@SuppressWarnings("SpellCheckingInspection")
public class SqrRoot {

    public static BigInteger bigIntSqRootCeil(BigInteger x)
            throws IllegalArgumentException {
        BigInteger y = x.sqrt();
        if (x.compareTo(y.multiply(y)) == 0) {
            return y;
        }
        return y.add(BigInteger.ONE);
    }
    
}
