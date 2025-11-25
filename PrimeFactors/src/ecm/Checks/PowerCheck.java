
package ecm.Checks;

import static Factorzations.ecm.StaticFunctions.modPow;
import BigIntegers.ECMBigInteger;
import java.math.BigInteger;

/**
 *
 * @author X. Wang
 */
public class PowerCheck {
    private final ECMBigInteger NumberToFactor;
    
    public PowerCheck(ECMBigInteger NumberToFactor){
        this.NumberToFactor = NumberToFactor;
    }
    
    public int Check(int i){
        final int maxExpon = (NumberToFactor.bitLength() - 1) / 17;
        int h, j;
        BigInteger root;
        final int prime2310x1[] =
          { 2311, 4621, 9241, 11551, 18481, 25411, 32341, 34651, 43891, 50821 };
        // Primes of the form 2310x + 1.
        boolean expon2 = true, expon3 = true, expon5 = true;
        boolean expon7 = true, expon11 = true;
        for (h = 0; h < prime2310x1.length; h++){
          final long testprime = prime2310x1[h];
          final long mod = NumberToFactor.mod(BigInteger.valueOf(testprime)).intValue();
          if (expon2 && modPow(mod, testprime / 2, testprime) > 1)
            expon2 = false;
          if (expon3 && modPow(mod, testprime / 3, testprime) > 1)
            expon3 = false;
          if (expon5 && modPow(mod, testprime / 5, testprime) > 1)
            expon5 = false;
          if (expon7 && modPow(mod, testprime / 7, testprime) > 1)
            expon7 = false;
          if (expon11 && modPow(mod, testprime / 11, testprime) > 1)
            expon11 = false;
        }
        boolean ProcessExpon[] = new boolean[maxExpon + 1];
        boolean primes[] = new boolean[2 * maxExpon + 3];
        for (h = 2; h <= maxExpon; h++)
          ProcessExpon[h] = true;
        for (h = 2; h < primes.length; h++)
          primes[h] = true;
        for (h = 2; h * h < primes.length; h++){ // Generation of primes
          for (j = h * h; j < primes.length; j += h)
          { // using Eratosthenes sieve
            primes[j] = false;
          }
        }
        for (h = 13; h < primes.length; h++){
          if (primes[h]){
            int processed = 0;
            for (j = 2 * h + 1; j < primes.length; j += 2 * h){
              if (primes[j]){
                long modulus = NumberToFactor.mod(BigInteger.valueOf(j)).longValue();
                if (modPow(modulus, j / h, j) > 1){
                  for (j = h; j <= maxExpon; j += h)
                    ProcessExpon[j] = false;
                  break;
                }
              }
              if (++processed > 10)
                break;
            }
          }
        }
        for (int Exponent = maxExpon; Exponent >= 2; Exponent--){
          if (Exponent % 2 == 0 && !expon2)
            continue; // Not a square
          if (Exponent % 3 == 0 && !expon3)
            continue; // Not a cube
          if (Exponent % 5 == 0 && !expon5)
            continue; // Not a fifth power
          if (Exponent % 7 == 0 && !expon7)
            continue; // Not a 7th power
          if (Exponent % 11 == 0 && !expon11)
            continue; // Not an 11th power
          if (!ProcessExpon[Exponent])
            continue;
          int intLog2N = NumberToFactor.bitLength() - 1;
          double log2N =
            intLog2N
              + Math.log(NumberToFactor.shiftRight(intLog2N - 32).add(BigInteger.ONE).doubleValue())
                / Math.log(2)
              - 32;
          log2N /= Exponent;
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
            BigInteger rootN1 = root.pow(Exponent - 1);
            BigInteger rootN = root.multiply(rootN1);
            BigInteger dif = NumberToFactor.subtract(rootN);
            if (dif.signum() == 0)
            { // Perfect power
              NumberToFactor.setPD(i, root);
              NumberToFactor.setExp(i, NumberToFactor.getExp(i) * Exponent);
              return Exponent;
            }
            BigInteger nextroot =
              dif
                .add(BigInteger.ONE)
                .divide(BigInteger.valueOf(Exponent).multiply(rootN1))
                .add(root)
                .subtract(BigInteger.ONE);
            if (root.compareTo(nextroot) <= 0)
              break; // Not a perfect power
            root = nextroot;
          }
        }
        return 1;
    }
}
