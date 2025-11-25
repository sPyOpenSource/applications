
package Factorzations.ecm;

import BigIntegers.AprtCleInteger;
import static BigIntegers.StaticFunctions.BigIntToBigNbr;
import static BigIntegers.StaticFunctions.BigNbrAreEqual;
import Factorzations.Lehman;
import static Factorzations.ecm.ecmStep0.limits;

import java.math.BigInteger;

/**
 *
 * @author X. Wang
 */
public class ECM extends ecmStep2 {
    public BigInteger factor(){
        int EC = 1;
        do {
            while(true){
                BigInteger NN = new Lehman(N, EC).factor();
                if (!NN.equals(BigInteger.ONE)){ // Factor found.
                    return NN;
                }
                L1 = N.toString().length(); // Get number of digits.
                if (L1 > 30 && L1 <= 90 &&    // If between 30 and 90 digits...
                    (N.digitsInGroup & 0x400) == 0)
                {                             // Switch to SIQS checkbox is set.
                    int limit = limits[((int)L1 - 31) / 5];
                    if (EC % 50000000 >= limit && !forcedECM){ // Switch to SIQS.
                        //EC += TYP_SIQS;
                        return BigInteger.ONE;
                    }
                }
                //N.setType(N.NbrFactors - 1, EC);

                BigInteger n = step0(EC++);
                if (n == BigInteger.TWO) continue;
                if (n != null) return n;
                if (step1() == 1) break;
                if (step2() == 1) break;
            }
        } while(BigNbrAreEqual(GD, N.getIntArray(), N.size()));
        return BigIntToBigNbr(GD, N.size());
    }
  
    public ECM(AprtCleInteger NumberToFactor){
        super(NumberToFactor);
        NumberToFactor.GetYieldFrequency();
        NumberToFactor.ComputeMontgomeryParms();
    }
    
    public static void main(String args[]){
        AprtCleInteger N = new AprtCleInteger("1411871524934571474917760655895199855251278439165992573432696950418064945008237961581572291078632362876307204161");
        ECM ecm = new ECM(N);
        System.out.println(ecm.factor());
    }
}
