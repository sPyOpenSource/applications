
package Factorzations.siqs;

import static Factorzations.siqs.Relation.congruencesFound;
import static Factorzations.siqs.Relation.matrixB;
import static Factorzations.siqs.Siqs.primeTrialDivisionData;

/**
 *
 * @author spy
 */
public class StaticFunctions {
    public static int getIndexFromDivisor(long Divid, int nbrPrimes){
        int left = -1;
        int median = nbrPrimes; 
        int right = median;
        while (left != right)
        {
            median = ((right - left) >> 1) + left;
            int nbr = primeTrialDivisionData[median].value;
            if (nbr < Divid)
            {
                if (median == left &&
                    congruencesFound.get() >= matrixB.length)
                    return 0;
                left = median;
            }
            else if (nbr > Divid)
            {
                right = median;
            } else {
                break;
            }
            if(right - left == 1){
                //System.out.println("not found");
                return 0;
            }
        }
        return median;
    }
    
    public static int JacobiSymbol(int M, int Q){
        // Calculate gcd(M,Q)
        int t1 = M % Q;
        int t2 = Q;
        int t = 1;
        while (t1 != 0){
            int tmp;
            while((t1 & 1) == 0){
                t1 >>= 1;
                if (((t2 & 7) == 3) || ((t2 & 7) == 5))
                {   // m = 3 or m = 5 (mod 8)
                    t = -t;
                }
            }
            tmp = t1; t1 = t2; t2 = tmp;   // Exchange a and m.
            if ((t1 & t2 & 3) == 3)
            {   // a = 3 and m = 3 (mod 4)
                t = -t;
            }
            t1 = t1 % t2;
        }
        if ((t2 == 1) || (t2 == -1))
        {
            return t;
        }
        return 0;
    }
    
    public static int modInv(int NbrMod, int currentPrime)
  {
    int QQ, T1, T3;
    int V1 = 1;
    int V3 = NbrMod;
    int U1 = 0;
    int U3 = currentPrime;
    while (V3 != 0)
    {
      if (U3 < V3 + V3)
      {               // QQ = 1
        T1 = U1 - V1;
        T3 = U3 - V3;
      } else {
        QQ = U3 / V3;
        T1 = U1 - V1 * QQ;
        T3 = U3 - V3 * QQ;
      }
      U1 = V1;
      U3 = V3;
      V1 = T1;
      V3 = T3;
    }
    return U1 + (currentPrime & (U1 >> 31));
  }
    
  /* Implementation of algorithm explained in Gower and Wagstaff paper */
  /* The variables with suffix 3 correspond to multiplier = 3          */
  public static int SQUFOF(long N, int queue[])
  {
    double sqrt;
    int Q, Q1, P, P1, L, S;
    int i, j, r, s, t, q;
    int queueHead, queueTail, queueIndex;
    long N3;
    int Q3, Q13, P3, P13, L3, S3;
    int r3, s3, t3, q3;
    int queueHead3, queueTail3, queueIndex3;
    int QRev, Q1Rev, PRev, P1Rev;
    int tRev, qRev, uRev;
    
    /* Step 1: Initialize */
    N3 = 3 * N;
    if ((N & 3) == 1)
      N <<= 1;
    if ((N3 & 3) == 1)
      N3 <<= 1;
    sqrt = Math.sqrt(N);
    S = (int)sqrt;
    if ((long)(S + 1) * (long)(S + 1)<= N)
      S++;
    if ((long)S * (long)S > N)
      S--;
    if ((long)S * (long)S == N)
      return S;
    Q1 = 1;
    P = S;
    Q = (int)N - P * P;
    L = (int)(2 * Math.sqrt(2 * sqrt));
    queueHead = 0;
    queueTail = 0;

    sqrt = Math.sqrt(N3);
    S3 = (int)sqrt;
    if ((long)(S3+1) * (long)(S3 + 1) <= N3)
      S3++;
    if ((long)S3*(long)S3 > N3)
      S3--;
    if ((long)S3*(long)S3 == N3)
      return S3;
    Q13 = 1;
    P3 = S3;
    Q3 = (int)N3 - P3 * P3;
    L3 = (int)(2 * Math.sqrt(2 * sqrt));
    queueHead3 = 100;
    queueTail3 = 100;

    /* Step 2: Cycle forward to find a proper square form */
    for (i = 0; i <= L; i++)
    {
      /* Multiplier == 1 */
      q = (S + P) / Q;
      P1 = q * Q - P;
      if (Q <= L)
      {
        if ((Q & 1) == 0)
        {
          queue[queueHead++] = Q >> 1;
          queue[queueHead++] = P % (Q >> 1);
          if (queueHead == 100)
            queueHead = 0;
        }
        else if (Q+Q<=L)
        {
          queue[queueHead++] = Q;
          queue[queueHead++] = P % Q;
          if (queueHead == 100)
            queueHead = 0;
        }
      }
      t = Q1 + q * (P - P1);
      Q1 = Q;
      Q = t;
      P = P1;
      {
        r = (int)Math.sqrt(Q);
        if (r * r == Q)
        {
          queueIndex = queueTail;
          for (;;)
          {
            if (queueIndex == queueHead)
            {
              /* Step 3: Compute inverse square root of the square form */
              PRev = P;
              Q1Rev = r;
              uRev = (S - PRev) % r;
              uRev += (uRev >> 31) & r;
              PRev = S - uRev;
              QRev = (int)((N - (long)PRev * (long)PRev) / Q1Rev);
              
              /* Step 4: Cycle in the reverse direction to find a factor of N */
              for (j = i; j >= 0; j--)
              {
                qRev = (S + PRev) / QRev;
                P1Rev = qRev * QRev - PRev;
                if (PRev == P1Rev)
                {
                  /* Step 5: Get the factor of N */
                  if ((QRev & 1) == 0)
                    return QRev >> 1;
                  return QRev;
                }
                tRev = Q1Rev + qRev * (PRev - P1Rev);
                Q1Rev = QRev;
                QRev = tRev;
                PRev = P1Rev;
                qRev = (S + PRev) / QRev;
                P1Rev = qRev * QRev - PRev;
                if (PRev == P1Rev)
                {
                  
                  /* Step 5: Get the factor of N */
                  if ((QRev & 1) == 0)
                    return QRev >> 1;
                  return QRev;
                }
                tRev = Q1Rev + qRev * (PRev - P1Rev);
                Q1Rev = QRev;
                QRev = tRev;
                PRev = P1Rev;
              }
              break;
            }
            s = queue[queueIndex++];
            t = queue[queueIndex++];
            if (queueIndex == 100)
              queueIndex = 0;
            if ((P-t)%s == 0)
              break;
          }
          if (r > 1)
            queueTail = queueIndex;
          if (r == 1)
          {
            queueIndex = queueTail;
            for (;;)
            {
              if (queueIndex == queueHead)
                break;
              if (queue[queueIndex] == 1)
                return 0;
              queueIndex += 2;
              if (queueIndex == 100)
                queueIndex = 0;
            }
          }
        }
      }
      q = (S + P) / Q;
      P1 = q * Q - P;
      if (Q <= L)
      {
        if ((Q & 1) == 0)
        {
          queue[queueHead++] = Q >> 1;
          queue[queueHead++] = P % (Q >> 1);
          if (queueHead == 100)
            queueHead = 0;
        }
        else if (Q + Q <= L)
        {
          queue[queueHead++] = Q;
          queue[queueHead++] = P % Q;
          if (queueHead == 100)
            queueHead = 0;
        }
      }
      t = Q1 + q * (P - P1);
      Q1 = Q;
      Q = t;
      P = P1;

      /* Multiplier == 3 */
      q3 = (S3 + P3) / Q3;
      P13 = q3 * Q3 - P3;
      if (Q3 <= L3)
      {
        if ((Q3 & 1) == 0)
        {
          queue[queueHead3++] = Q3 >> 1;
          queue[queueHead3++] = P3 % (Q3 >> 1);
          if (queueHead3 == 200)
            queueHead3 = 100;
        }
        else if (Q3+Q3<=L3)
        {
          queue[queueHead3++] = Q3;
          queue[queueHead3++] = P3 % Q3;
          if (queueHead3 == 200)
            queueHead3 = 100;
        }
      }
      t3 = Q13 + q3 * (P3 - P13);
      Q13 = Q3;
      Q3 = t3;
      P3 = P13;
      {
        r3 = (int)Math.sqrt(Q3);
        if (r3 * r3 == Q3)
        {
          queueIndex3 = queueTail3;
          for (;;)
          {
            if (queueIndex3 == queueHead3)
            {
              /* Step 3: Compute inverse square root of the square form */
              PRev = P3;
              Q1Rev = r3;
              uRev = (S3 - PRev) % r3;
              uRev += (uRev >> 31) & r3;
              PRev = S3 - uRev;
              QRev = (int)((N3 - (long)PRev * (long)PRev) / Q1Rev);
              /* Step 4: Cycle in the reverse direction to find a factor of N */
              for (j = i; j >= 0; j--)
              {
                qRev = (S3 + PRev) / QRev;
                P1Rev = qRev * QRev - PRev;
                if (PRev == P1Rev)
                {
                  /* Step 5: Get the factor of N */
                  if ((QRev & 1) == 0)
                    return QRev >> 1;
                  return QRev;
                }
                tRev = Q1Rev + qRev * (PRev - P1Rev);
                Q1Rev = QRev;
                QRev = tRev;
                PRev = P1Rev;
                qRev = (S3 + PRev) / QRev;
                P1Rev = qRev * QRev - PRev;
                if (PRev == P1Rev)
                {
                  /* Step 5: Get the factor of N */
                  if ((QRev & 1) == 0)
                    return QRev >> 1;
                  return QRev;
                }
                tRev = Q1Rev + qRev * (PRev - P1Rev);
                Q1Rev = QRev;
                QRev = tRev;
                PRev = P1Rev;
              }
              break;
            }
            s3 = queue[queueIndex3++];
            t3 = queue[queueIndex3++];
            if (queueIndex3 == 200)
              queueIndex3 = 100;
            if ((P3-t3)%s3 == 0)
              break;
          }
          if (r3 > 1)
            queueTail3 = queueIndex3;
          if (r3 == 1)
          {
            queueIndex3 = queueTail3;
            for (;;)
            {
              if (queueIndex3 == queueHead3)
                break;
              if (queue[queueIndex3] == 1)
                return 0;
              queueIndex3 += 2;
              if (queueIndex3 == 200)
                queueIndex3 = 100;
            }
          }
        }
      }
      q3 = (S3 + P3) / Q3;
      P13 = q3 * Q3 - P3;
      if (Q3 <= L3)
      {
        if ((Q3 & 1) == 0)
        {
          queue[queueHead3++] = Q3 >> 1;
          queue[queueHead3++] = P3 % (Q3 >> 1);
          if (queueHead3 == 200)
            queueHead3 = 100;
        }
        else if (Q3+Q3<=L3)
        {
          queue[queueHead3++] = Q3;
          queue[queueHead3++] = P3 % Q3;
          if (queueHead3 == 200)
            queueHead3 = 100;
        }
      }
      t3 = Q13 + q3 * (P3 - P13);
      Q13 = Q3;
      Q3 = t3;
      P3 = P13;
    }
    return 0;
  }

  /* If 2^value mod value = 2, then the value is a probable prime (value odd) */
  public static boolean isProbablePrime(long value)
  {
    long mask, montgomery2, Pr, Prod0, Prod1, MontDig;
    int N, MontgomeryMultN;
    long BaseHI, BaseLO, valueHI, valueLO;
    int x = N  = (int)value; // 2 least significant bits of inverse correct.
    x = x * (2 - N * x);     // 4 least significant bits of inverse correct.
    x = x * (2 - N * x);     // 8 least significant bits of inverse correct.
    x = x * (2 - N * x);     // 16 least significant bits of inverse correct.
    x = x * (2 - N * x);     // 32 least significant bits of inverse correct.
    MontgomeryMultN = (-x) & 0x7FFFFFFF;
    mask = 1L << 62;
    montgomery2 = 2 * (mask % value);
    if (montgomery2 >= value)
      montgomery2 -= value;
    BaseHI = (int)(montgomery2 >> 31);
    BaseLO = (int)montgomery2 & 0x7FFFFFFF;
    valueHI = (int)(value >> 31);
    valueLO = (int)value & 0x7FFFFFFF;
    while ((mask & value) == 0)
      mask >>= 1;
    mask >>= 1;
    while (mask > 0)
    {
      /* Square the base */
      Pr = BaseLO * BaseLO;
      MontDig = ((int) Pr * MontgomeryMultN) & 0x7FFFFFFFL;
      Prod0 = (Pr = ((MontDig * valueLO + Pr) >>> 31) +
              MontDig * valueHI + BaseLO * BaseHI) & 0x7FFFFFFFL;
      Prod1 = Pr >>> 31;
      Pr = BaseHI * BaseLO + Prod0;
      MontDig = ((int) Pr * MontgomeryMultN) & 0x7FFFFFFFL;
      Prod0 = (Pr = ((MontDig * valueLO + Pr) >>> 31) +
              MontDig * valueHI + BaseHI * BaseHI + Prod1) & 0x7FFFFFFFL;
      Prod1 = Pr >>> 31;
      if (Prod1 > valueHI || (Prod1 == valueHI && Prod0 >= valueLO))
      {
        Prod0 = (Pr = Prod0 - valueLO) & 0x7FFFFFFFL;
        Prod1 = ((Pr >> 31) + Prod1 - valueHI) & 0x7FFFFFFFL;
      }
      BaseLO = Prod0;
      BaseHI = Prod1;

      if ((mask & value) != 0)
      {
        /* Multiply by 2 */
        Pr = 2*((BaseHI << 31) + BaseLO);
        if (Pr >= value)
          Pr -= value;
        BaseHI = (int)(Pr >> 31);
        BaseLO = (int)Pr & 0x7FFFFFFF;
      }
      mask >>= 1;
    }
    Pr = (BaseHI << 31) + BaseLO;
    return Pr == montgomery2;
  }
}
