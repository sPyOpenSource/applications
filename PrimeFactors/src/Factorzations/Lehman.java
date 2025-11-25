
package Factorzations;

import static BigIntegers.FactorInteger.BigInt3;
import java.math.BigInteger;

/**
 *
 * @author spy
 */
public class Lehman extends Thread{
    private final BigInteger nbr;
    private final int k;
    static final long bitsSqr[] = {
      0x0000000000000003L, // 3
      0x0000000000000013L, // 5
      0x0000000000000017L, // 7
      0x000000000000023BL, // 11
      0x000000000000161BL, // 13
      0x000000000001A317L, // 17
      0x0000000000030AF3L, // 19
      0x000000000005335FL, // 23
      0x0000000013D122F3L, // 29
      0x00000000121D47B7L, // 31
      0x000000165E211E9BL, // 37
      0x000001B382B50737L, // 41
      0x0000035883A3EE53L, // 43
      0x000004351B2753DFL, // 47
      0x0012DD703303AED3L, // 53
      0x022B62183E7B92BBL, // 59
      0x1713E6940A59F23BL, // 61
    };
    static final int primes[] = { 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61 };

    public Lehman(BigInteger nbr, int k){
        this.nbr = nbr;
        this.k = k;
    }
    
    @Override
    public void run(){
        BigInteger result = factor();
        if (!result.equals(BigInteger.ONE)){
            System.out.println(result);
        }
    }
    
    // Perform Lehman algorithm
    public BigInteger factor(){
      int nbrs[] = new int[17];
      int diffs[] = new int[17];
      int i, m;
      BigInteger root, rootN, dif, nextroot;
      BigInteger bM, r;
      if (!nbr.testBit(0)){ // nbr Even
        r = BigInteger.ZERO;
        m = 1;
        bM = BigInteger.ONE;
      } else {
        if (k % 2 == 0){ // k Even
          r = BigInteger.ONE;
          m = 2;
          bM = BigInteger.TWO;
        } else { // k Odd
          r = BigInteger.valueOf(k).add(nbr).and(BigInt3);
          m = 4;
          bM = BigInteger.valueOf(4);
        }
      }
      BigInteger sqr = nbr.multiply(BigInteger.valueOf(k)).shiftLeft(2);
      int intLog2N = sqr.bitLength() - 1;
      double log2N = intLog2N + Math.log(sqr.shiftRight(intLog2N - 32).add(BigInteger.ONE).doubleValue())
              / Math.log(2) - 32;
      log2N /= 2;
      if (log2N < 32) {
        root = BigInteger.valueOf((long) Math.exp(log2N * Math.log(2)));
      } else {
        intLog2N = (int) Math.floor(log2N) - 32;
        root =
          BigInteger
            .valueOf((long) Math.exp((log2N - intLog2N) * Math.log(2)) + 10)
            .shiftLeft(intLog2N);
      }
      while (true){
        rootN = root.multiply(root);
        dif = sqr.subtract(rootN);
        if (dif.signum() == 0)
        { // Perfect power
          break;
        }
        nextroot =
          dif.add(BigInteger.ONE).divide(BigInteger.TWO.multiply(root)).add(root).subtract(
            BigInteger.ONE);
        if (root.compareTo(nextroot) <= 0)
          break; // Not a perfect power
        root = nextroot;
      }
      BigInteger a = root;
      while (!a.mod(bM).equals(r) || a.multiply(a).compareTo(sqr) < 0)
        a = a.add(BigInteger.ONE);
      BigInteger c = a.multiply(a).subtract(sqr);
      for (i = 0; i < 17; i++){
        final BigInteger prime = BigInteger.valueOf(primes[i]);
        nbrs[i] = c.mod(prime).intValue();
        diffs[i] = bM.multiply(a.shiftLeft(1).add(bM)).mod(prime).intValue();
      }
      for (int j = 0; j < 10000; j++){
        for (i = 0; i < 17; i++){
          if ((bitsSqr[i] & (1L << nbrs[i])) == 0)
          { // Not a perfect square
            break;
          }
        }
        if (i == 17){ // Test for perfect square
          BigInteger val = a.add(BigInteger.valueOf(m * j));
          c = val.multiply(val).subtract(sqr);
          intLog2N = c.bitLength() - 1;
          log2N = intLog2N + Math.log(c.shiftRight(intLog2N - 32).add(BigInteger.ONE).doubleValue())
                  / Math.log(2) - 32;
          log2N /= 2;
          if (log2N < 32){
            root = BigInteger.valueOf((long) Math.exp(log2N * Math.log(2)));
          } else {
            intLog2N = (int) Math.floor(log2N) - 32;
            root =
              BigInteger
                .valueOf((long) Math.exp((log2N - intLog2N) * Math.log(2)) + 10)
                .shiftLeft(intLog2N);
          }
          while (true){
            rootN = root.multiply(root);
            dif = c.subtract(rootN);
            if (dif.signum() == 0){ // Perfect power -> factor found
              root = nbr.gcd(val.add(root));
              if (root.compareTo(BigInteger.valueOf(10000)) > 0)
                return root; // Return non-trivial found
            }
            nextroot =
              dif.add(BigInteger.ONE).divide(BigInteger.TWO.multiply(root)).add(root).subtract(BigInteger.ONE);
            if (root.compareTo(nextroot) <= 0)
              break; // Not a perfect power
            root = nextroot;
          }
        }
        for (i = 0; i < 17; i++){
          nbrs[i] = (nbrs[i] + diffs[i]) % primes[i];
          diffs[i] = (diffs[i] + 2 * m * m) % primes[i];
        }
      }
      return BigInteger.ONE; // Factor not found
    }
}
