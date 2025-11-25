
package ecm.BigIntegers;

import static BigIntegers.FactorInteger.NLen;
import static calculator.largemodel.Add.AddBigNbr;
import static calculator.largemodel.ChangeSigns.ChSignBigNbr;
import static calculator.largemodel.Divide.DivBigNbrByLong;
import static calculator.largemodel.Subtract.SubtractBigNbr;

/**
 *
 * @author X. Wang
 */
public class GCD {
    static final int CalcAuxGcdU[] = new int[NLen], CalcAuxGcdV[] = new int[NLen];
    static final int CalcAuxGcdT[] = new int[NLen];
    
  // Gcd calculation:
  // Step 1: Set k<-0, and then repeatedly set k<-k+1, u<-u/2, v<-v/2
  //         zero or more times until u and v are not both even.
  // Step 2: If u is odd, set t<-(-v) and go to step 4. Otherwise set t<-u.
  // Step 3: Set t<-t/2
  // Step 4: If t is even, go back to step 3.
  // Step 5: If t>0, set u<-t, otherwise set v<-(-t).
  // Step 6: Set t<-u-v. If t!=0, go back to step 3.
  // Step 7: The GCD is u*2^k.
  public static void GcdBigNbr(int Nbr1[], int Nbr2[], int Gcd[], int NumberLength)
  {
    int i, k;

    System.arraycopy(Nbr1, 0, CalcAuxGcdU, 0, NumberLength);
    System.arraycopy(Nbr2, 0, CalcAuxGcdV, 0, NumberLength);
    for (i = 0; i < NumberLength; i++)
    {
      if (CalcAuxGcdU[i] != 0)
      {
        break;
      }
    }
    if (i == NumberLength)
    {
      System.arraycopy(CalcAuxGcdV, 0, Gcd, 0, NumberLength);
      return;
    }
    for (i = 0; i < NumberLength; i++)
    {
      if (CalcAuxGcdV[i] != 0)
      {
        break;
      }
    }
    if (i == NumberLength)
    {
      System.arraycopy(CalcAuxGcdU, 0, Gcd, 0, NumberLength);
      return;
    }
    if ((CalcAuxGcdU[NumberLength - 1] & 0x40000000) != 0)
    {
      ChSignBigNbr(CalcAuxGcdU, NumberLength);
    }
    if ((CalcAuxGcdV[NumberLength - 1] & 0x40000000) != 0)
    {
      ChSignBigNbr(CalcAuxGcdV, NumberLength);
    }
    k = 0;
    while ((CalcAuxGcdU[0] & 1) == 0 && (CalcAuxGcdV[0] & 1) == 0)
    { // Step 1
      k++;
      DivBigNbrByLong(CalcAuxGcdU, 2, CalcAuxGcdU, NumberLength);
      DivBigNbrByLong(CalcAuxGcdV, 2, CalcAuxGcdV, NumberLength);
    }
    if ((CalcAuxGcdU[0] & 1) == 1)
    { // Step 2
      System.arraycopy(CalcAuxGcdV, 0, CalcAuxGcdT, 0, NumberLength);
      ChSignBigNbr(CalcAuxGcdT, NumberLength);
    } else {
      System.arraycopy(CalcAuxGcdU, 0, CalcAuxGcdT, 0, NumberLength);
    }
    do
    {
      while ((CalcAuxGcdT[0] & 1) == 0)
      { // Step 4
        DivBigNbrByLong(CalcAuxGcdT, 2, CalcAuxGcdT, NumberLength); // Step 3
      }
      if ((CalcAuxGcdT[NumberLength - 1] & 0x40000000) == 0)
      { // Step 5
        System.arraycopy(CalcAuxGcdT, 0, CalcAuxGcdU, 0, NumberLength);
      }
      else
      {
        System.arraycopy(CalcAuxGcdT, 0, CalcAuxGcdV, 0, NumberLength);
        ChSignBigNbr(CalcAuxGcdV, NumberLength);
      }                                                // Step 6
      SubtractBigNbr(CalcAuxGcdU, CalcAuxGcdV, CalcAuxGcdT, NumberLength);
      for (i = 0; i < NumberLength; i++)
      {
        if (CalcAuxGcdT[i] != 0)
        {
          break;
        }
      }
    }
    while (i != NumberLength);
    System.arraycopy(CalcAuxGcdU, 0, Gcd, 0, NumberLength); // Step 7
    while (k > 0)
    {
      AddBigNbr(Gcd, Gcd, Gcd, NumberLength);
      k--;
    }
}
}
