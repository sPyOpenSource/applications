package Factorzations.ecm;

import static BigIntegers.FactorInteger.MaxUInt;

/**
 *
 * @author xuyi
 */
public class StaticFunctions extends BigIntegers.StaticFunctions {
    public static long modPow(long NbrMod, long Expon, long currentPrime){
        long Power = 1;
        long Square = NbrMod;
        while (Expon != 0){
            if ((Expon & 1) == 1){
                Power = (Power * Square) % currentPrime;
            }
            Square = (Square * Square) % currentPrime;
            Expon /= 2;
        }
        return Power;
    }
    
    /**********************/
    /* Auxiliary routines */
    /**********************/
  
  /* End of code "borrowed" from Paul Zimmermann's ECM4C */
    
    /*********************************************************/
    /* Start of code "borrowed" from Paul Zimmermann's ECM4C */
    /*********************************************************/
    final static int ADD = 6; /* number of multiplications in an addition */
    final static int DUP = 5; /* number of multiplications in a duplicate */

  
    public static void GenerateSieve(
    int initial,
    byte[] sieve,
    byte[] sieve2310,
    int[] SmallPrime)
  {
    int i, j, Q;
    for (i = 0; i < 23100; i += 2310)
    {
      System.arraycopy(sieve2310, 0, sieve, i, 2310);
    }
    j = 5;
    Q = 13; /* Point to prime 13 */
    do
    {
      if (initial > Q * Q)
      {
        for (i = (int) (((long) initial * ((Q - 1) / 2)) % Q);
          i < 23100;
          i += Q)
        {
          sieve[i] = 1; /* Composite */
        }
      }
      else
      {
        i = Q * Q - initial;
        if (i < 46200)
        {
          for (i = i / 2; i < 23100; i += Q)
          {
            sieve[i] = 1; /* Composite */
          }
        }
        else
        {
          break;
        }
      }
      Q = SmallPrime[++j];
    }
    while (Q < 5000);
  }
    
    /* returns the number of modular multiplications */
  public static int lucas_cost(int n, double v)
  {
    int c, d, e, r;

    d = n;
    r = (int) ((double) d / v + 0.5);
    if (r >= n)
      return (ADD * n);
    d = n - r;
    e = 2 * r - n;
    c = DUP + ADD; /* initial duplicate and final addition */
    while (d != e)
    {
      if (d < e)
      {
        r = d;
        d = e;
        e = r;
      }
      if (4 * d <= 5 * e && ((d + e) % 3) == 0)
      { /* condition 1 */
        r = (2 * d - e) / 3;
        e = (2 * e - d) / 3;
        d = r;
        c += 3 * ADD; /* 3 additions */
      }
      else if (4 * d <= 5 * e && (d - e) % 6 == 0)
      { /* condition 2 */
        d = (d - e) / 2;
        c += ADD + DUP; /* one addition, one duplicate */
      }
      else if (d <= (4 * e))
      { /* condition 3 */
        d -= e;
        c += ADD; /* one addition */
      }
      else if ((d + e) % 2 == 0)
      { /* condition 4 */
        d = (d - e) / 2;
        c += ADD + DUP; /* one addition, one duplicate */
      }
      else if (d % 2 == 0)
      { /* condition 5 */
        d /= 2;
        c += ADD + DUP; /* one addition, one duplicate */
      }
      else if (d % 3 == 0)
      { /* condition 6 */
        d = d / 3 - e;
        c += 3 * ADD + DUP; /* three additions, one duplicate */
      }
      else if ((d + e) % 3 == 0)
      { /* condition 7 */
        d = (d - 2 * e) / 3;
        c += 3 * ADD + DUP; /* three additions, one duplicate */
      }
      else if ((d - e) % 3 == 0)
      { /* condition 8 */
        d = (d - e) / 3;
        c += 3 * ADD + DUP; /* three additions, one duplicate */
      }
      else if (e % 2 == 0)
      { /* condition 9 */
        e /= 2;
        c += ADD + DUP; /* one addition, one duplicate */
      }
    }
    return (c);
  }
  
  public static int Cos(int N){
      switch (N % 8){
        case 0 :
          return 1;
        case 4 :
          return -1;
      }
      return 0;
    }
  
  public static void MultBigNbrByLongModN(int Nbr1[], long Nbr2, int Prod[], int TestNbr[], int NumberLength){
        long Pr;
        int j;

        if (NumberLength >= 2 &&
                    TestNbr[NumberLength - 1] == 0 && TestNbr[NumberLength - 2] < 0x40000000)
          NumberLength--;
        Pr = 0;
        for (j = 0; j < NumberLength; j++)
        {
          Pr = (Pr >>> 31) + Nbr2 * Nbr1[j];
          Prod[j] = (int)(Pr & MaxUInt);
        }
        Prod[j] = (int)(Pr >>> 31);
        AdjustModN(Prod, TestNbr, NumberLength);
    }
  
  public static void SubtractBigNbrModN(int Nbr1[], int Nbr2[], int Diff[], int TestNbr[], int NumberLength){
        long carry = 0;
        int i;

        for (i = 0; i < NumberLength; i++)
        {
          carry = (carry >> 31) + (long)Nbr1[i] - (long)Nbr2[i];
          Diff[i] = (int)(carry & MaxUInt);
        }
        if (carry < 0)
        {
          carry = 0;
          for (i = 0; i < NumberLength; i++)
          {
            carry = (carry >> 31) + (long)Diff[i] + (long)TestNbr[i];
            Diff[i] = (int)(carry & MaxUInt);
          }
        }
    }
  
  public static void LongToBigNbr(long Nbr, int[] Out, int NumberLength){
        Out[0] = (int)(Nbr & MaxUInt);
        Out[1] = (int)((Nbr >> 31) & MaxUInt);
        for (int i = 2; i < NumberLength; i++){
            Out[i] = Nbr < 0 ? (int)MaxUInt : 0;
        }
    }
}
