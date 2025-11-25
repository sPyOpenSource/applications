
package Factorzations;

import static com.gazman.math.SqrRoot.bigIntSqRootCeil;
import java.math.BigInteger;

/**
 *
 * @author spy
 */
public class Classical {
    private BigInteger N;
    
    public Classical(BigInteger N){
        this.N = N;
    }
    
    public static boolean odd(BigInteger i){
        return i.testBit(0);
    }
    
    public static BigInteger SqrtFloor(BigInteger x)
        throws IllegalArgumentException {
        if (x.compareTo(BigInteger.ZERO) < 0) {
            throw new IllegalArgumentException("Negative argument.");
        }
        return x.sqrt();
    } // end bigIntSqRootFloor

    public static BigInteger SqrtCeil(BigInteger x)
        throws IllegalArgumentException {
        if (x.compareTo(BigInteger.ZERO) < 0) {
            throw new IllegalArgumentException("Negative argument.");
        }
        return bigIntSqRootCeil(x);
    } // end bigIntSqRootCeil
    
    public BigInteger factor()
    {
        BigInteger sqrt, tmp, ctr;
        
        if (N.compareTo(BigInteger.ONE) != 1){
            System.out.println("Errot: Please insert N >= 2");
            return N;
        }
        System.out.println(N + "=");

        /* Tries to compute m = N mod 2 */
        /* if m == 0 => 2|N [2 is a factor of N] */
        while (!odd(N)){
            System.out.print("2x");
            N = N.divide(BigInteger.TWO);
        }

        /* Checks if N == 1 */
        if (N.compareTo(BigInteger.ONE) == 0){
            //System.out.println("Factorization has been completed in %ld seconds.\n", tm1.tv_sec - tm0.tv_sec); 
            return N;
        }

        /* Checks if N is prime */
        /* Uses a probility primality test that has */
        /* probabity of failure == 0.25 ^ x [here x == 10] */
        if (N.isProbablePrime(1)){
            System.out.print(N + "x");
            //System.out.println("Factorization has been completed in %ld seconds.\n", tm1.tv_sec - tm0.tv_sec); 
            return N;
        }
        ctr = new BigInteger("3"); /* sets ctr = 3 */
        sqrt = SqrtFloor(N);

        if (odd(sqrt))
            sqrt = sqrt.add(BigInteger.ONE);

        /* while ctr < sqrt(N) */
        while (ctr.compareTo(sqrt) < 0 && ctr.compareTo(BigInteger.valueOf(131072)) < 0){
            while (true){
                tmp = N.mod(ctr);
                if (tmp.compareTo(BigInteger.ZERO) == 0){
                    System.out.print(ctr + "x");
                    N = N.divide(ctr);
                    sqrt = SqrtFloor(N);
                    if (odd(sqrt))
                        sqrt = sqrt.add(BigInteger.ONE);
                } else {
                    break;
                }
            }
            ctr = ctr.add(BigInteger.TWO);
            if (N.compareTo(BigInteger.ONE) == 0){
                //System.out.println("Factorization has been completed in %ld seconds.\n", tm1.tv_sec - tm0.tv_sec); 
                return N;
            }
            if (N.isProbablePrime(1)) {
                //System.out.println("Factorization has been completed in %ld seconds.\n", tm1.tv_sec - tm0.tv_sec); 
                return N;
            }
        }
        return N;
    }
}
