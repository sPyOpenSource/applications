
package BigIntegers;

import static BigIntegers.FactorInteger.MaxUInt;
import static BigIntegers.MontgomeryInteger.dDosALa31;
import static BigIntegers.MontgomeryInteger.dDosALa62;
import java.math.BigInteger;

/**
 *
 * @author xuyi
 */
public class StaticFunctions {
    private static final long DosALa32 = (long) 1 << 32;
    private static final long Mi = 1000000000;

    public static int BigNbrToBigInt(BigInteger N, int TestNbr[])
    {
        byte[] Result;
        long[] Temp;
        int i, j, mask;
        long p;

        Result = N.toByteArray();
        int NumberLength = (Result.length * 8 + 30) / 31;
        Temp = new long[NumberLength + 1];
        j = 0;
        mask = 1;
        p = 0;
        for (i = Result.length - 1; i >= 0; i--){
            p += mask * (long) (Result[i] >= 0 ? Result[i] : Result[i] + 256);
            mask <<= 8;
            if (mask == 0){                        // Overflow
                Temp[j++] = p;
                mask = 1;
                p = 0;
            }
        }
        Temp[j] = p;
        Convert32To31Bits(Temp, TestNbr, NumberLength);
        if (TestNbr[NumberLength - 1] > Mi){
            TestNbr[NumberLength] = 0;
            NumberLength++;
        }
        TestNbr[NumberLength] = 0;
        return NumberLength;
    }
    
    public static void Convert32To31Bits(long[] nbr32bits, int[] TestNbr, int NumberLength)
    {
        int j = 0;
        nbr32bits[NumberLength] = 0;
        for (int i = 0; i < NumberLength; i++){
            int k = i & 0x0000001F;
            if (k == 0){
                TestNbr[i] = (int)(nbr32bits[j] & MaxUInt);
            } else {
                TestNbr[i] = (int)(((nbr32bits[j] >> (32 - k)) | (nbr32bits[j + 1] << k)) & MaxUInt);
                j++;
            }
        }
    }
    
    public static BigInteger BigIntToBigNbr(int[] TestNbr, int NumberLength)
    {
        long[] Temp = new long[NumberLength];
        Convert31To32Bits(Temp, TestNbr, NumberLength);
        int NL = NumberLength * 4;
        byte[] Result = new byte[NL];
        for (int i = 0; i < NumberLength; i++){
            long digit = Temp[i];
            Result[NL - 1 - 4 * i] = (byte) (digit             & 0xFF);
            Result[NL - 2 - 4 * i] = (byte) (digit / 0x100     & 0xFF);
            Result[NL - 3 - 4 * i] = (byte) (digit / 0x10000   & 0xFF);
            Result[NL - 4 - 4 * i] = (byte) (digit / 0x1000000 & 0xFF);
        }
        return new BigInteger(Result);
    }
    
    public static void Convert31To32Bits(long[] nbr32bits, int[] TestNbr, int NumberLength){
        int i = 0;
        for (int j = -1; j < NumberLength; j++){
            int k = i % 31;
            if (k == 0)
                j++;
            if (j == NumberLength)
                break;
            if (j == NumberLength - 1)
                nbr32bits[i] = TestNbr[j] >> k;
            else
                nbr32bits[i] = ((TestNbr[j] >> k) | (TestNbr[j + 1] << (31 - k))) & 0xFFFFFFFFL;
            i++;
        }
        for (; i < NumberLength; i++)
            nbr32bits[i] = 0;
    }
    
    public static void AdjustModN(int Nbr[], int TestNbr[], int NumberLength)
  {
    long carry;
    double dN = (double) TestNbr[NumberLength - 1];
    if (NumberLength > 1)
      dN += (double) TestNbr[NumberLength - 2] / dDosALa31;
    if (NumberLength > 2)
      dN += (double) TestNbr[NumberLength - 3] / dDosALa62;
    double dAux = (double) Nbr[NumberLength] * dDosALa31 + (double) Nbr[NumberLength - 1];
    if (NumberLength > 1)
      dAux += (double) Nbr[NumberLength - 2] / dDosALa31;
    long TrialQuotient = (long) (dAux / dN) + 3;
    if (TrialQuotient >= DosALa32){
      carry = 0;
      for (int i = 0; i < NumberLength; i++){
        carry = Nbr[i + 1] - (TrialQuotient >>> 31) * TestNbr[i] - carry;
        Nbr[i + 1] = (int)(carry & MaxUInt);
        carry = (MaxUInt - carry) >>> 31;
      }
      TrialQuotient &= MaxUInt;
    }
    carry = 0;
    for (int i = 0; i < NumberLength; i++){
      carry = Nbr[i] - TrialQuotient * TestNbr[i] - carry;
      Nbr[i] = (int)(carry & MaxUInt);
      carry = (MaxUInt - carry) >>> 31;
    }
    Nbr[NumberLength] -= (int)carry;
    while ((Nbr[NumberLength] & MaxUInt) != 0){
      carry = 0;
      for (int i = 0; i < NumberLength; i++){
        carry += (long)Nbr[i] + (long)TestNbr[i];
        Nbr[i] = (int)(carry & MaxUInt);
        carry >>= 31;
      }
      Nbr[NumberLength] += (int)carry;
    }
  }

public static void MultBigNbrModN(int Nbr1[], int Nbr2[], int Prod[], int TestNbr[], int NumberLength){
    int i, j;

    if (NumberLength >= 2 && TestNbr[NumberLength - 1] == 0 && TestNbr[NumberLength - 2] < 0x40000000)
        NumberLength--;
    i = NumberLength;
    do {
      Prod[--i] = 0;
    } while (i > 0);
    i = NumberLength;
    do {
      long Nbr = Nbr1[--i];
      j = NumberLength;
      do {
        Prod[j] = Prod[j - 1];
        j--;
      } while (j > 0);
      Prod[0] = 0;
      long Pr = 0;
      for (j = 0; j < NumberLength; j++){
        Pr = (Pr >>> 31) + Nbr * Nbr2[j] + Prod[j];
        Prod[j] = (int)(Pr & MaxUInt);
      }
      Prod[j] += (Pr >>> 31);
      AdjustModN(Prod, TestNbr, NumberLength);
    } while (i > 0);
  }

public static void AddBigNbrModN(int Nbr1[], int Nbr2[], int Sum[], int TestNbr[], int NumberLength){
    long carry = 0;

    for (int i = 0; i < NumberLength; i++){
      carry = (carry >> 31) + (long)Nbr1[i] + (long)Nbr2[i] - (long)TestNbr[i];
      Sum[i] = (int)(carry & MaxUInt);
    }
    if (carry < 0){
      carry = 0;
      for (int i = 0; i < NumberLength; i++){
        carry = (carry >> 31) + (long)Sum[i] + (long)TestNbr[i];
        Sum[i] = (int)(carry & MaxUInt);
      }
    }
  }

public static boolean BigNbrAreEqual(int Nbr1[], int Nbr2[], int NumberLength){
    for (int i = 0; i < NumberLength; i++){
      if (Nbr1[i] != Nbr2[i])
        return false;
    }
    return true;
  }

public static boolean BigNbrIsZero(int Nbr[], int NumberLength){
        for (int i = 0; i < NumberLength; i++)
        {
          if (Nbr[i] != 0)
          {
            return false;
          }
        }
        return true;
    }
}
