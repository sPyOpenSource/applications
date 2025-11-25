
package BigIntegers;

import static BigIntegers.StaticFunctions.BigNbrToBigInt;

/**
 * Example: Curve25519
 * @author X. Wang
 */
public class MontgomeryInteger extends ECMBigInteger {
    public static final double dDosALa31 = (double) DosALa31;
    public static final double dDosALa62 = dDosALa31 * dDosALa31;    
    static final int KARATSUBA_CUTOFF = 64;
    static final boolean KARATSUBA_ENABLED = false;
    long MontgomeryMultN;
    public double dN;
    protected int karatLength;
    static final int[] arrNbr = new int[4 * NLen], arrNbrAux = new int[4 * NLen], arrNbrM = new int[4 * NLen];
    final int[] montgKaratsubaArr = new int[2 * NLen];
    final int KaratsubaTestNbr[] = new int[NLen];
    public final int MontgomeryMultR1[] = new int[NLen], MontgomeryMultR2[] = new int[NLen], MontgomeryMultAfterInv[] = new int[NLen];

    public MontgomeryInteger(String val) {
        super(val);
        setSize(BigNbrToBigInt(this, digits));
        //addSelf2PD();
        if(this.compareTo(ZERO) > 0){
            GetYieldFrequency();
            ComputeMontgomeryParms();
        }
    }
    
    public void MontgomeryMult(int Nbr1[], int Nbr2[], int Prod[]){
        if (KARATSUBA_ENABLED == false || length < KARATSUBA_CUTOFF){
            for (int i = 0; i < length; i++)
                Prod[i] = 0;
            for (int i = 0; i < length; i++){
                long Pr = (long)Nbr1[i] * Nbr2[0] + Prod[0];
                long MontDig = ((int) Pr * MontgomeryMultN) & MaxUInt;
                Pr = MontDig * digits[0] + Pr;
                for (int j = 0; j < length - 1; j++){
                    Pr = (Pr >>> 31) + MontDig * digits[j + 1] + (long)Nbr1[i] * Nbr2[j + 1] + Prod[j + 1];
                    Prod[j] = (int)(Pr & MaxUInt);
                }
                Prod[length - 1] = (int)(Pr >>> 31);
            }
            for (int i = 0; i < length; i++){
                if (Prod[i] < digits[i])
                    return;
            }
            long Pr = 0;
            for (int j = 0; j < length; j++){
                Pr = (Pr >> 31) + Prod[j] - digits[j];
                Prod[j] = (int)(Pr & MaxUInt);
            }
        } else {
          KaratsubaMontgomeryMult(Nbr1, Nbr2, Prod);
        }
  }
    
    // Algorithm REDC: x is the product of both numbers
    //  m = (x mod R) * N' mod R
    //  t = (x + m*N) / R
    //  if t < N
    //     return t
    //  else return t - N
  void KaratsubaMontgomeryMult(int Nbr1[], int Nbr2[], int Prod[]){
    int carry = 0;
    int i;
    for (i = 0; i < karatLength; i++){
      arrNbr[i] = (int)Nbr1[i];
      arrNbr[karatLength + i] = (int)Nbr2[i];
    }
    KaratsubaMultiply(0, karatLength, 2 * karatLength);      // Get x.
    System.arraycopy(arrNbr, 0,                              // Source
                     arrNbrM, 0,                             // Destination
                     2 * karatLength);                       // Length
    System.arraycopy(montgKaratsubaArr, 0,                   // Source
                     arrNbr, 0,                              // Destination
                     karatLength);                           // Length
    KaratsubaMultiply(0, karatLength, 2 * karatLength);      // Get m.
    System.arraycopy(arrNbr, 0,                              // Source
                     arrNbr, 0,                              // Destination
                     karatLength);                           // Length
    System.arraycopy(KaratsubaTestNbr, 0,                    // Source
                     arrNbr, 0,                              // Destination
                     karatLength);                           // Length
    KaratsubaMultiply(0, karatLength, 2 * karatLength);      // Get m * N.
    for (i = 0; i < 2 * karatLength; i++){
      carry += arrNbr[i] + arrNbrM[i];
      if (carry < 0){              // Bit 31 is set.
        arrNbr[i] = carry - (-0x80000000);
        carry = 1;
      } else {
        arrNbr[i] = carry;
        carry = 0;
      }
    }                                                        // t is got.
    if (carry==0){
      for (i = 2 * karatLength - 1; i >= karatLength; i--){
        if (arrNbr[i] > KaratsubaTestNbr[i]){
          carry = 1;
          break;
        }
        if (arrNbr[i] < KaratsubaTestNbr[i])
          break;
      }
    }
    if (carry != 0){
      carry = 0;
      for (i = 2 * karatLength-1; i >= karatLength; i--){
        carry += arrNbr[i] - KaratsubaTestNbr[i];
        if (carry < 0)
        {                         // Bit 31 is set.
          arrNbr[i] = carry - (-0x80000000);
        }
        else
        {
          arrNbr[i] = carry;
        }
      }
    }
    for (i = karatLength; i < 2 * karatLength; i++)
      Prod[i - karatLength] = arrNbr[i];
  }
  
  private void KaratsubaMultiply(int idxFactor1, int length, int endIndex){
    int idxFactor2 = idxFactor1 + length;
    int i, carry2;
    if (length <= KARATSUBA_CUTOFF){
      // Check if one of the factors is equal to zero.
      for (i = length - 1; i >= 0; i--){
        if (arrNbr[idxFactor1 + i] != 0)
          break;
      }
      if (i >= 0){
        for (i = length - 1; i >= 0; i--){
          if (arrNbr[idxFactor2 + i] != 0)
            break;
        }
      }
      if (i < 0){              // One of the factors is equal to zero.
        for (i = length - 1; i >= 0; i--)
          arrNbr[idxFactor1 + i] = arrNbr[idxFactor2 + i] = 0;
        return;
      }
      NormalMultiply(idxFactor1, length);
    }
        // Length > KARATSUBA_CUTOFF: Use Karatsuba multiplication.
        // At this moment the order is: High1, Low1, High2, Low2.
        // Exchange low part of first factor with high part of 2nd factor.
    int halfLength = length / 2;
    for (i = idxFactor1 + halfLength; i < idxFactor2; i++){
      int tmp = arrNbr[i];
      arrNbr[i] = arrNbr[i + halfLength];
      arrNbr[i + halfLength] = tmp;
    }
        // At this moment the order is: High1, High2, Low1, Low2.
        // Get absolute values of (High1-Low1) and (Low2-High2) and the signs.
    boolean sign = absSubtract(idxFactor1, idxFactor2, endIndex, halfLength);
    sign = absSubtract(idxFactor2 + halfLength, idxFactor1 + halfLength,
                        endIndex + halfLength, halfLength) != sign;
    int middle = endIndex;
    endIndex += length;
                                      // Multiply both low parts.
    KaratsubaMultiply(idxFactor1, halfLength, endIndex);
                                      // Multiply both high parts.
    KaratsubaMultiply(idxFactor2, halfLength, endIndex);
    KaratsubaMultiply(middle, halfLength, endIndex);
    if (sign){            // (High1-Low1) * (Low2-High2) is negative.
      if (absSubtract(idxFactor1, middle, middle, length)){          // Result is still negative.
        absSubtract(idxFactor2, middle, middle, length);
        carry2 = 0;
      } else {
        carry2 = KaratsubaAdd(idxFactor2, middle, middle, length);
      }
    } else {            // (High1-Low1) * (Low2-High2) is non-negative.
      carry2 = KaratsubaAdd(idxFactor1, middle, middle, length);
      carry2 += KaratsubaAdd(idxFactor2, middle, middle, length);
    }
    int carry = 0;
    int offs = idxFactor1 + halfLength;
    for (i = 0; i < length; i++){
      carry += arrNbr[offs + i] + arrNbr[middle + i];
      if (carry < 0){
        arrNbr[offs + i] = carry - (-0x80000000);
        carry = 1;
      } else {
        arrNbr[offs + i] = carry;
        carry = 0;
      }
    }
    arrNbr[idxFactor1 + halfLength + i] += carry + carry2;
    if (arrNbr[idxFactor1 + halfLength + i] < 0){
      arrNbr[idxFactor1 + halfLength + i] -= (-0x80000000);
      for (i = halfLength + 1; i < length; i++){
        if (++arrNbr[idxFactor1 + i] >= 0)
          break;
        arrNbr[idxFactor1 + i] = 0;
      }
    }
  }
  
  // The return value is the sign: true: negative.
  // In result the absolute value of the difference is computed.
  private boolean absSubtract(int idxMinuend, int idxSubtrahend, int idxResult, int length){
    boolean sign = false;
    int carry = 0;
    int i;
    for (i = length - 1; i >= 0; i--){
      if (arrNbr[idxMinuend + i] != arrNbr[idxSubtrahend + i])
        break;
    }
    if (i >= 0 && arrNbr[idxMinuend + i] < arrNbr[idxSubtrahend + i]){
      sign = true;
      i = idxMinuend;
      idxMinuend = idxSubtrahend;
      idxSubtrahend = i;
    }
    for (i = 0; i < length; i++){
      carry = arrNbr[idxMinuend + i] - arrNbr[idxSubtrahend + i] - carry;
      if (carry < 0){
        arrNbr[idxResult + i] = carry + (-0x80000000);
        carry = 1;
      } else {
        arrNbr[idxResult + i] = carry;
        carry = 0;
      }
    }
    return sign;
  }
  
  private int KaratsubaAdd(int idxAddend1, int idxAddend2, int idxSum, int length){
    int carry = 0;
    for (int i = 0; i < length; i++)
    {
      carry += arrNbr[idxAddend1 + i] + arrNbr[idxAddend2 + i];
      if (carry < 0){ // Bit 31 is set.
        arrNbr[idxSum + i] = carry - (-0x80000000);
        carry = 1;
      } else {
        arrNbr[idxSum + i] = carry;
        carry = 0;
      }
    }
    return carry;
  }
  
  public long GetYieldFrequency()
    {
        long yieldFreq = 1000000 / (length * length);
        if (yieldFreq > 100000)
            yieldFreq = yieldFreq / 100000 * 100000;
        else if (yieldFreq > 10000)
            yieldFreq = yieldFreq / 10000 * 10000;
        else if (yieldFreq > 1000)
            yieldFreq = yieldFreq / 1000 * 1000;
        else if (yieldFreq > 100)
            yieldFreq = yieldFreq / 100 * 100;
        return yieldFreq;
    }

    public void ComputeMontgomeryParms()
  {
    int N, x;

    dN = (double) digits[length - 1];
    if (length > 1)
      dN += (double) digits[length - 2] / dDosALa31;
    if (length > 2)
      dN += (double) digits[length - 3] / dDosALa62;

    x = N = (int) digits[0]; // 2 least significant bits of inverse correct.
    x = x * (2 - N * x);      // 4 least significant bits of inverse correct.
    x = x * (2 - N * x);      // 8 least significant bits of inverse correct.
    x = x * (2 - N * x);      // 16 least significant bits of inverse correct.
    x = x * (2 - N * x);      // 32 least significant bits of inverse correct.
    MontgomeryMultN = (-x) & 0x7FFFFFFF;
    
    if (KARATSUBA_ENABLED && length >= KARATSUBA_CUTOFF){
      int div = 1;
      while (length > KARATSUBA_CUTOFF){
        div *= 2;
        length = (length + 1) / 2;
      }
      karatLength = length * div;
      for (int i = length; i < karatLength; i++)
        digits[i] = 0;
      for (int i = 0; i < karatLength; i++)
        KaratsubaTestNbr[i] = (int) digits[i];
      montgKaratsubaArr[0] = (int) MontgomeryMultN;
      int j = 1;
      for (int i = 2; i <= length; i <<= 1){
          // Compute x <- x * (2 + N * x)
          System.arraycopy(KaratsubaTestNbr, 0, arrNbr, 0, j);
          System.arraycopy(montgKaratsubaArr, 0, arrNbr, j, j);
          NormalMultiply(0, j);
          if ((arrNbrAux[0] += 2) < 0){
              arrNbrAux[0] &= 0x7FFFFFFF;
              for (int k = 1; k < i; k++){
                  if (++arrNbrAux[k] >= 0)
                      break;
                  arrNbrAux[k] = 0;
              }
          }
          System.arraycopy(arrNbrAux, 0, arrNbr, 0, i);
          System.arraycopy(montgKaratsubaArr, 0, arrNbr, i, i);
          NormalMultiply(0, i);
          System.arraycopy(arrNbrAux, 0, montgKaratsubaArr, 0, i);
          j <<= 1;
      }
    }
    int j = length;
    MontgomeryMultR1[j] = 1;
    do {
      MontgomeryMultR1[--j] = 0;
    } while (j > 0);
    StaticFunctions.AdjustModN(MontgomeryMultR1, digits, length);
    StaticFunctions.MultBigNbrModN(MontgomeryMultR1, MontgomeryMultR1, MontgomeryMultR2, digits, length);
    MontgomeryMult(MontgomeryMultR2, MontgomeryMultR2, MontgomeryMultAfterInv);
    StaticFunctions.AddBigNbrModN(MontgomeryMultR1, MontgomeryMultR1, MontgomeryMultR2, digits, length);
  }
    
    private void NormalMultiply(int idxFactor1, int length){
    long carry, prod;
    int destIndex, j, offs, tmp;
    int idxFactor2 = idxFactor1 + length;
    long sum = 0;
    int lastDestIndex = 2 * length - 1;
    for (destIndex = 0; destIndex < lastDestIndex; destIndex++){
      carry = 0;
      offs = idxFactor2 + destIndex;
      if (destIndex < length){
        for (j = destIndex; j > 0; j -= 2){
          prod = (long)arrNbr[idxFactor1 + j] * (long)arrNbr[offs - j] +
                 (long)arrNbr[idxFactor1 + j - 1] * (long)arrNbr[offs - j + 1];
          sum += prod & MaxUInt;
          carry += prod >>> 31;
        }
        if (j==0){
          prod = (long)arrNbr[idxFactor1 + j] * (long)arrNbr[offs - j];
          sum += prod & MaxUInt;
          carry += prod >>> 31;
        }
      } else {
        for (j = length - 1; j > destIndex - length; j -= 2){
          prod = (long)arrNbr[idxFactor1 + j] * (long)arrNbr[offs - j] +
                 (long)arrNbr[idxFactor1 + j - 1] * (long)arrNbr[offs - j + 1];
          sum += prod & MaxUInt;
          carry += prod >>> 31;
        }
        if (j==destIndex - length){
          prod = (long)arrNbr[idxFactor1 + j] * (long)arrNbr[offs - j];
          sum += prod & MaxUInt;
          carry += prod >>> 31;
        }
      }
      tmp = (int)(sum >>> 31);
      arrNbrAux[destIndex] = (int)sum & 0x7FFFFFFF;
      sum = carry + tmp;
    }
    arrNbrAux[destIndex] = (int)sum;
    System.arraycopy(arrNbrAux, 0, arrNbr, idxFactor1, 2 * length);
  }
}
