
package BigIntegers;

import static BigIntegers.FactorInteger.NLen;
import static BigIntegers.StaticFunctions.BigNbrIsZero;
import static Factorzations.ecm.StaticFunctions.MultBigNbrByLongModN;
import static Factorzations.ecm.StaticFunctions.SubtractBigNbrModN;

import static calculator.largemodel.Add.AddBigNbr;
import static calculator.largemodel.Multiply.MultBigNbr;
import static calculator.largemodel.Subtract.SubtractBigNbr;
import static ecm.BigIntegers.GCD.GcdBigNbr;
import ecm.PrimeTest.AprtCleVariable;
import java.math.BigInteger;

/**
 *
 * @author spy
 */
public class AprtCleInteger extends MontgomeryInteger {
    public AprtCleVariable variable = new AprtCleVariable();
    final int biN[] = new int[NLen], biT[] = new int[NLen]; 
    private final int biU[] = new int[NLen]; /* Temp */
    private final int biV[] = new int[NLen]; /* Temp */
    private BigInteger probableFactor;
    
    public AprtCleInteger(String val) {
        super(val);
    }
    
    public long BigNbrModLong(long Nbr2){
        long Rem = 0;

        for (int i = length - 1; i >= 0; i--)
          Rem = ((Rem << 31) + digits[i]) % Nbr2;
        return Rem;
    }
    
    public void JacobiSum(int A, int B, int P, int PK, int PL, int PM, int Q){
    int I, J, K;

    for (I = 0; I < PL; I++){
      for (J = 0; J < length; J++)
        variable.aiJ0[I][J] = 0;
    }
    for (I = 1; I <= Q - 2; I++){
      J = (A * I + B * variable.aiF[I]) % PK;
      if (J < PL){
        StaticFunctions.AddBigNbrModN(variable.aiJ0[J], MontgomeryMultR1, variable.aiJ0[J], digits, length);
      } else {
        for (K = 1; K < P; K++){
          SubtractBigNbrModN(
            variable.aiJ0[J - K * PM],
            MontgomeryMultR1,
            variable.aiJ0[J - K * PM],
            digits,
            length);
        }
      }
    }
  }
  
  // Perform JS <- JS * JW
  public void JS_JW(int PK, int PL, int PM, int P){
    int I, J;
    for (I = 0; I < PL; I++){
      for (J = 0; J < PL; J++){
        int K = (I + J) % PK;
        MontgomeryMult(variable.aiJS[I], variable.aiJW[J], variable.biTmp);
        StaticFunctions.AddBigNbrModN(variable.aiJX[K], variable.biTmp, variable.aiJX[K], digits, length);
      }
    }
    for (I = 0; I < PK; I++){
      for (J = 0; J < length; J++){
        variable.aiJS[I][J] = variable.aiJX[I][J];
        variable.aiJX[I][J] = 0;
      }
    }
    NormalizeJS(PK, PL, PM, P);
  }
  
  // Normalize coefficient of JS
  public void NormalizeJS(int PK, int PL, int PM, int P){
    int I, J;
    for (I = PL; I < PK; I++){
      if (!BigNbrIsZero(variable.aiJS[I], length)){
        for (J = 0; J < length; J++)
          biT[J] = variable.aiJS[I][J];
        for (J = 1; J < P; J++)
          SubtractBigNbrModN(variable.aiJS[I - J * PM], biT, variable.aiJS[I - J * PM], digits, length);
        for (J = 0; J < length; J++)
          variable.aiJS[I][J] = 0;
      }
    }
  }
  
  // Perform JS <- JS ^ 2
  public void JS_2(int PK, int PL, int PM, int P){
    int I, J, K;
    for (I = 0; I < PL; I++){
      K = 2 * I % PK;
      MontgomeryMult(variable.aiJS[I], variable.aiJS[I], variable.biTmp);
      StaticFunctions.AddBigNbrModN(variable.aiJX[K], variable.biTmp, variable.aiJX[K], digits, length);
      StaticFunctions.AddBigNbrModN(variable.aiJS[I], variable.aiJS[I], biT, digits, length);
      for (J = I + 1; J < PL; J++){
        K = (I + J) % PK;
        MontgomeryMult(biT, variable.aiJS[J], variable.biTmp);
        StaticFunctions.AddBigNbrModN(variable.aiJX[K], variable.biTmp, variable.aiJX[K], digits, length);
      }
    }
    for (I = 0; I < PK; I++){
      for (J = 0; J < length; J++){
        variable.aiJS[I][J] = variable.aiJX[I][J];
        variable.aiJX[I][J] = 0;
      }
    }
    NormalizeJS(PK, PL, PM, P);
  }
  
  // Perform JS <- JS ^ E
  public void JS_E(int PK, int PL, int PM, int P){
    int I, J, K;
    long Mask;

    for (I = length - 1; I > 0; I--){
      if (variable.biExp[I] != 0)
        break;
    }
    if (I == 0 && variable.biExp[0] == 1)
    {
      return;
    } // Return if E == 1
    for (K = 0; K < PL; K++){
      for (J = 0; J < length; J++)
        variable.aiJW[K][J] = variable.aiJS[K][J];
    }
    Mask = 0x40000000L;
    while (true){
      if ((variable.biExp[I] & Mask) != 0)
        break;
      Mask /= 2;
    }
    do {
      JS_2(PK, PL, PM, P);
      Mask /= 2;
      if (Mask == 0){
        Mask = 0x40000000L;
        I--;
      }
      if ((variable.biExp[I] & Mask) != 0)
        JS_JW(PK, PL, PM, P);
    } while (I > 0 || Mask != 1);
  }
  
  // Normalize coefficient of JW
  public void NormalizeJW(int PK, int PL, int PM, int P){
    int I, J;
    for (I = PL; I < PK; I++){
      if (!BigNbrIsZero(variable.aiJW[I], length)){
        for (J = 0; J < length; J++)
          biT[J] = variable.aiJW[I][J];
        for (J = 1; J < P; J++)
          SubtractBigNbrModN(variable.aiJW[I - J * PM], biT, variable.aiJW[I - J * PM], digits, length);
        for (J = 0; J < length; J++)
          variable.aiJW[I][J] = 0;
      }
    }
  }
  
  public void BigNbrModN(int Nbr[], int Length, int Mod[])
  {
    int i, j;
    for (i = 0; i < length; i++)
    {
      Mod[i] = Nbr[i + Length - length];
    }
    Mod[i] = 0;
    StaticFunctions.AdjustModN(Mod, digits, length);
    for (i = Length - length - 1; i >= 0; i--)
    {
      for (j = length; j > 0; j--)
      {
        Mod[j] = Mod[j - 1];
      }
      Mod[0] = Nbr[i];
      StaticFunctions.AdjustModN(Mod, digits, length);
    }
  }
  
  // Compare Nbr1^2 vs. Nbr2
  public int CompareSquare(int biS[], int biTmp[]){
    int I, k;

    for (I = length - 1; I > 0; I--){
      if (biS[I] != 0)
        break;
    }
    k = length / 2;
    if (length % 2 == 0){
      if (I >= k)
      {
        return 1;
      } // Nbr1^2 > Nbr2
      if (I < k - 1 || biS[k - 1] < 65536)
      {
        return -1;
      } // Nbr1^2 < Nbr2
    } else {
      if (I < k)
      {
        return -1;
      } // Nbr1^2 < Nbr2
      if (I > k || biS[k] >= 65536)
      {
        return 1;
      } // Nbr1^2 > Nbr2
    }
    MultBigNbr(biS, biS, biTmp, length);
    SubtractBigNbr(biTmp, digits, biTmp, length);
    if (BigNbrIsZero(biTmp, length))
    {
      return 0;
    } // Nbr1^2 == Nbr2
    if (biTmp[length - 1] >= 0)
    {
      return 1;
    } // Nbr1^2 > Nbr2
    return -1; // Nbr1^2 < Nbr2
  }
  
  // Prime checking routine
  // Return codes: 0 = Number is prime.
  //               1 = Number is composite.
  public boolean isProbablePrimeRobin(){
    int j, k;
    int exp = subtract(BigInteger.ONE).getLowestSetBit();
    long Base = 1;
    int nbrBases = bitLength() / 2;
    for (int baseNbr = 0; baseNbr < nbrBases; baseNbr++){
      if (Base < 3){
        Base++;
      } else {
        calculate_new_prime : while (true){
          Base += 2;
          for (long Q = 3; Q * Q <= Base; Q += 2){ /* Check if Base is prime */
            if (Base % Q == 0)
              continue calculate_new_prime; /* Composite */
          }
          break; /* Prime found */
        }
      } /* end if */
      /*System.out.println(
        "Rabin probabilistic prime check routine\nBase used: "
          + Base
          + " ("
          + baseNbr * 100 / nbrBases
          + "%)");*/
      System.arraycopy(MontgomeryMultR1, 0, biN, 0, length);
      int index = length - 1;
      long mask = 0x40000000L;
      for (k = length * 31; k > exp; k--){
        MontgomeryMult(biN, biN, biT);
        if ((digits[index] & mask) != 0){
          MultBigNbrByLongModN(biT, Base, biT, digits, length);
        }
        System.arraycopy(biT, 0, biN, 0, length);
        mask >>= 1;
        if (mask == 0){
          index--;
          mask = 0x40000000L;
        }
      }
      for (j = 0; j < length; j++){
        if (biN[j] != MontgomeryMultR1[j])
          break;
      }
      if (j == length)
      {
        continue;
      } /* Probable prime, go to next base */
      for (k = 0; k < exp; k++){
        AddBigNbr(biN, MontgomeryMultR1, biT, length);
        for (j = 0; j < length; j++){
          if (biT[j] != digits[j])
            break;
        }
        if (j == length)
        {
          break;
        } /* Probable prime, go to next base */
        MontgomeryMult(biN, biN, biT);
        /* Check whether square equals 1 */
        for (j = 0; j < length; j++)
        {
          if (biT[j] != MontgomeryMultR1[j])
          {
            break;
          }
        }
        if (j == length)
        { // Check whether the number can be factored
          // by computing gcd(temp-1, N).
          SubtractBigNbrModN(biN, MontgomeryMultR1, biU, digits, length);
          GcdBigNbr(biU, digits, biV, length);
          if (biV[0] == 1)
            {
          for (j = 1; j < length; j++)
          {
            if (biV[j] != 0)
            {
              break;
            }
          }
            } else {
                  j = 0;
            }
          if (j < length)
          {  // biV is a proper factor.
            probableFactor = StaticFunctions.BigIntToBigNbr(biV, length);
            return false; // Composite number
          }
        }
        System.arraycopy(biT, 0, biN, 0, length);
      }
      if (k == exp){
          return false;
      }
    }
    return true; /* Indicate probable prime */
  }
}
