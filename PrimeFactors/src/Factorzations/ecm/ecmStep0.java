
package Factorzations.ecm;

import BigIntegers.AprtCleInteger;
import static BigIntegers.FactorInteger.NLen;
import static BigIntegers.StaticFunctions.BigNbrIsZero;
import static ecm.BigIntegers.ModInv.ModInvBigNbr;
import java.math.BigInteger;

/**
 *
 * @author X. Wang
 */
public class ecmStep0 {
    public final AprtCleInteger N;
    final int[] Aux1 = new int[NLen], Aux2 = new int[NLen], Aux3 = new int[NLen];
    private final int[] A0 = new int[NLen], A02 = new int[NLen], A03 = new int[NLen];
    final int[] AA = new int[NLen], DX  = new int[NLen], DZ  = new int[NLen];
    final int[] GD = new int[NLen], M   = new int[NLen], W3  = new int[NLen];
    final int[] W4 = new int[NLen], X   = new int[NLen], Z   = new int[NLen];
    long L1, L2, LS;
    int Prob;
    final double ProbArray[] = new double[6];
    boolean forcedECM = false;
    static final int limits[] = { 5, 8, 15, 25, 25, 27, 32, 43, 70, 150, 300, 350, 600 };
  
    public ecmStep0(AprtCleInteger N){
        this.N = N;
        for (int I = 0; I < N.size(); I++)
          M[I] = DX[I] = DZ[I] = W3[I] = W4[I] = GD[I] = 0;
        N.setSmallPrime(0, 2);
        long P = 3;
        for (int indexM = 1; indexM < N.getSmallPrimeLength(); indexM++){
          N.setSmallPrime(indexM, (int) P); /* Store prime */
          calculate_new_prime : while (true){
            P += 2;
            for (long Q = 3; Q * Q <= P; Q += 2){ /* Check if P is prime */
                if (P % Q == 0)
                    continue calculate_new_prime; /* Composite */
            }
            break; /* Prime found */
          }
        }
    }
    
    public BigInteger step0(int EC){
        L1 = 2000;
        L2 = 200000;
        LS = 45;
        long Paux = EC;
        N.setNbrPrimes(303); /* Number of primes less than 2000 */
        if (EC > 25){
          if (EC < 326){
            L1 = 50000;
            L2 = 5000000;
            LS = 224;
            Paux = EC - 24;
            N.setNbrPrimes(5133); /* Number of primes less than 50000 */
          } else {
            if (EC < 2000){
              L1 = 1000000;
              L2 = 100000000;
              LS = 1001;
              Paux = EC - 299;
              N.setNbrPrimes(78498); /* Number of primes less than 1000000 */
            } else {
              L1 = 11000000;
              L2 = 1100000000;
              LS = 3316;
              Paux = EC - 1900;
              N.setNbrPrimes(726517); /* Number of primes less than 11000000 */
            }
          }
        }
        
        for (int I = 0; I < 6; I++){
          Prob = (int) Math.round(100 * (1 - Math.exp(- ((double) L1 * (double) Paux) / ProbArray[I])));
        } /* end for */
        StaticFunctions.LongToBigNbr(2 * (EC + 1), Aux1,  N.size());
        StaticFunctions.LongToBigNbr(3 * (EC + 1) * (EC + 1) - 1, Aux2,  N.size());
        ModInvBigNbr(Aux2, Aux2, N.getIntArray(),  N.size());
        StaticFunctions.MultBigNbrModN(Aux1, Aux2, Aux3, N.getIntArray(),  N.size());
        StaticFunctions.MultBigNbrModN(Aux3, N.MontgomeryMultR1, A0, N.getIntArray(),  N.size());
        N.MontgomeryMult(A0, A0, A02);
        N.MontgomeryMult(A02, A0, A03);
        StaticFunctions.SubtractBigNbrModN(A03, A0, Aux1, N.getIntArray(),  N.size());
        StaticFunctions.MultBigNbrByLongModN(A02, 9, Aux2, N.getIntArray(),  N.size());
        StaticFunctions.SubtractBigNbrModN(Aux2, N.MontgomeryMultR1, Aux2, N.getIntArray(),  N.size());
        N.MontgomeryMult(Aux1, Aux2, Aux3);
        if (BigNbrIsZero(Aux3, N.size()))
          return BigInteger.TWO; //continue
        StaticFunctions.MultBigNbrByLongModN(A0, 4, Z, N.getIntArray(),  N.size());
        StaticFunctions.MultBigNbrByLongModN(A02, 6, Aux1, N.getIntArray(),  N.size());
        StaticFunctions.SubtractBigNbrModN(N.MontgomeryMultR1, Aux1, Aux1, N.getIntArray(),  N.size());
        N.MontgomeryMult(A02, A02, Aux2);
        StaticFunctions.MultBigNbrByLongModN(Aux2, 3, Aux2, N.getIntArray(),  N.size());
        StaticFunctions.SubtractBigNbrModN(Aux1, Aux2, Aux1, N.getIntArray(),  N.size());
        StaticFunctions.MultBigNbrByLongModN(A03, 4, Aux2, N.getIntArray(),  N.size());
        ModInvBigNbr(Aux2, Aux2, N.getIntArray(),  N.size());
        N.MontgomeryMult(Aux2, N.MontgomeryMultAfterInv, Aux3);
        N.MontgomeryMult(Aux1, Aux3, A0);
        StaticFunctions.AddBigNbrModN(A0, N.MontgomeryMultR2, Aux1, N.getIntArray(),  N.size());
        StaticFunctions.LongToBigNbr(4, Aux2,  N.size());
        ModInvBigNbr(Aux2, Aux3, N.getIntArray(),  N.size());
        StaticFunctions.MultBigNbrModN(Aux3, N.MontgomeryMultR1, Aux2, N.getIntArray(),  N.size());
        N.MontgomeryMult(Aux1, Aux2, AA);
        StaticFunctions.MultBigNbrByLongModN(A02, 3, Aux1, N.getIntArray(),  N.size());
        StaticFunctions.AddBigNbrModN(Aux1, N.MontgomeryMultR1, X, N.getIntArray(),  N.size());
        return null;
    }
}
