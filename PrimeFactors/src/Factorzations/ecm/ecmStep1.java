 
package Factorzations.ecm;

import BigIntegers.AprtCleInteger;
import static BigIntegers.FactorInteger.NLen;
import static BigIntegers.StaticFunctions.BigNbrAreEqual;
import static BigIntegers.StaticFunctions.BigNbrIsZero;
import static ecm.BigIntegers.GCD.GcdBigNbr;

/**
 * neutral element
 * @author X. Wang
 */
public class ecmStep1 extends ecmStep0 
{
    protected int indexPrimes, indexM, u;
    private final int[] Aux4 = new int[NLen];
    final int[] Xaux = new int[NLen];
    final int[] Zaux = new int[NLen];
    final byte[] sieve = new byte[23100];
    final byte[] sieve2310 = new byte[2310];
    final int[] W1 = new int[NLen];
    final int[] W2 = new int[NLen];
    int[] TX = new int[NLen], TZ = new int[NLen], UX = new int[NLen], UZ = new int[NLen];
    final int GcdAccumulated[] = new int[NLen];
    final int BigNbr1[] = new int[NLen];
    private long P;
        
    public ecmStep1(AprtCleInteger val) {
        super(val);
        BigNbr1[0] = 1;
        for (int i = 1; i < NLen; i++)
        {
          BigNbr1[i] = 0;
        }
    }
    
    /**************/
    /* First step */
    /**************/
    public int step1(){
        System.arraycopy(X, 0, Xaux, 0, N.size());
        System.arraycopy(Z, 0, Zaux, 0, N.size());
        System.arraycopy(N.MontgomeryMultR1, 0, GcdAccumulated, 0, N.size());
        for (int Pass = 0; Pass < 2; Pass++){
          /* For powers of 2 */
          indexPrimes = 0;
          for (long I = 1; I <= L1; I <<= 1)
            duplicate(X, Z, X, Z);
          for (long I = 3; I <= L1; I *= 3){
            duplicate(W1, W2, X, Z);
            add3(X, Z, X, Z, W1, W2, X, Z);
          }

          if (Pass == 0){
            N.MontgomeryMult(GcdAccumulated, Z, Aux1);
            System.arraycopy(Aux1, 0, GcdAccumulated, 0, N.size());
          } else {
            GcdBigNbr(Z, N.getIntArray(), GD, N.size());
            if (!BigNbrAreEqual(GD, BigNbr1, N.size()))
              return 1;
          }

          /* for powers of odd primes */

          indexM = 1;
          do {
            indexPrimes++;
            P = N.getSmallPrime(indexM);
            for (long IP = P; IP <= L1; IP *= P)
              prac((int) P, X, Z, W1, W2, W3, W4);
            indexM++;
            if (Pass == 0){
              N.MontgomeryMult(GcdAccumulated, Z, Aux1);
              System.arraycopy(Aux1, 0, GcdAccumulated, 0, N.size());
            } else {
              GcdBigNbr(Z, N.getIntArray(), GD, N.size());
              if (!BigNbrAreEqual(GD, BigNbr1, N.size()))
                return 1;
            }
          }
          while (N.getSmallPrime(indexM - 1) <= LS);
          P += 2;

          /* Initialize sieve2310[n]: 1 if gcd(P+2n,2310) > 1, 0 otherwise */
          u = (int) P;
          for (int i = 0; i < 2310; i++){
            sieve2310[i] =
              (u % 3 == 0
                || u % 5 == 0
                || u % 7 == 0
                || u % 11 == 0 ? (byte) 1 : (byte) 0);
            u += 2;
          }
          do {
            /* Generate sieve */
            StaticFunctions.GenerateSieve((int) P, sieve, sieve2310, N.getSmallPrimes());

            /* Walk through sieve */

            for (int i = 0; i < 23100; i++){
              if (sieve[i] != 0)
                continue; /* Do not process composites */
              if (P + 2 * i > L1)
                break;
              indexPrimes++;
              prac((int) (P + 2 * i), X, Z, W1, W2, W3, W4);
              if (Pass == 0){
                N.MontgomeryMult(GcdAccumulated, Z, Aux1);
                System.arraycopy(Aux1, 0, GcdAccumulated, 0, N.size());
              } else {
                GcdBigNbr(Z, N.getIntArray(), GD, N.size());
                if (!BigNbrAreEqual(GD, BigNbr1, N.size()))
                  return 1;
              }
            }
            P += 46200;
          } while (P < L1);
          if (Pass == 0){
            if (BigNbrIsZero(GcdAccumulated, N.size()))
            { // If GcdAccumulated is
              System.arraycopy(Xaux, 0, X, 0, N.size());
              System.arraycopy(Zaux, 0, Z, 0, N.size());
              continue; // multiple of TestNbr, continue.
            }
            GcdBigNbr(GcdAccumulated, N.getIntArray(), GD, N.size());
            if (!BigNbrAreEqual(GD, BigNbr1, N.size()))
              return 1;
            break;
          }
        } /* end for Pass */
        return 0;
    }
    
  /* computes 2P=(x2:z2) from P=(x1:z1), with 5 mul, 4 add/sub, 5 mod.
       Uses the following global variables:
       - n : number to factor
       - b : (a+2)/4 mod n
       - u, v, w : auxiliary variables
  Modifies: x2, z2, u, v, w
  */
  void duplicate(int[] x2, int[] z2, int[] x1, int[] z1)
  {
    int[] u = UZ;
    int[] v = TX;
    int[] w = TZ;
    StaticFunctions.AddBigNbrModN(x1, z1, w, N.getIntArray(), N.size());      // w = x1+z1
    N.MontgomeryMult(w, w, u);       // u = (x1+z1)^2
    StaticFunctions.SubtractBigNbrModN(x1, z1, w, N.getIntArray(), N.size()); // w = x1-z1
    N.MontgomeryMult(w, w, v);       // v = (x1-z1)^2
    N.MontgomeryMult(u, v, x2);      // x2 = u*v = (x1^2 - z1^2)^2
    StaticFunctions.SubtractBigNbrModN(u, v, w, N.getIntArray(), N.size());   // w = u-v = 4*x1*z1
    N.MontgomeryMult(AA, w, u);
    StaticFunctions.AddBigNbrModN(u, v, u, N.getIntArray(), N.size());        // u = (v+b*w)
    N.MontgomeryMult(w, u, z2);      // z2 = (w*u)
  }
  
    /* adds Q=(x2:z2) and R=(x1:z1) and puts the result in (x3:z3),
         using 5/6 mul, 6 add/sub and 6 mod. One assumes that Q-R=P or R-Q=P where P=(x:z).
         Uses the following global variables:
         - n : number to factor
         - x, z : coordinates of P
         - u, v, w : auxiliary variables
    Modifies: x3, z3, u, v, w.
    (x3,z3) may be identical to (x2,z2) and to (x,z)
    */
    void add3(
      int[] x3,
      int[] z3,
      int[] x2,
      int[] z2,
      int[] x1,
      int[] z1,
      int[] x,
      int[] z)
    {
      int[] t = TX;
      int[] u = TZ;
      int[] v = UX;
      int[] w = UZ;
      StaticFunctions.SubtractBigNbrModN(x2, z2, v, N.getIntArray(), N.size()); // v = x2-z2
      StaticFunctions.AddBigNbrModN(x1, z1, w, N.getIntArray(), N.size());      // w = x1+z1
      N.MontgomeryMult(v, w, u);       // u = (x2-z2)*(x1+z1)
      StaticFunctions.AddBigNbrModN(x2, z2, w, N.getIntArray(), N.size());      // w = x2+z2
      StaticFunctions.SubtractBigNbrModN(x1, z1, t, N.getIntArray(), N.size()); // t = x1-z1
      N.MontgomeryMult(t, w, v);       // v = (x2+z2)*(x1-z1)
      StaticFunctions.AddBigNbrModN(u, v, t, N.getIntArray(), N.size());        // t = 2*(x1*x2-z1*z2)
      N.MontgomeryMult(t, t, w);       // w = 4*(x1*x2-z1*z2)^2
      StaticFunctions.SubtractBigNbrModN(u, v, t, N.getIntArray(), N.size());   // t = 2*(x2*z1-x1*z2)
      N.MontgomeryMult(t, t, v);       // v = 4*(x2*z1-x1*z2)^2
      if (BigNbrAreEqual(x, x3, N.size()))
      {
        System.arraycopy(x, 0, u, 0, N.size());
        System.arraycopy(w, 0, t, 0, N.size());
        N.MontgomeryMult(z, t, w);
        N.MontgomeryMult(v, u, z3);
        System.arraycopy(w, 0, x3, 0, N.size());
      } else {
        N.MontgomeryMult(w, z, x3); // x3 = 4*z*(x1*x2-z1*z2)^2
        N.MontgomeryMult(x, v, z3); // z3 = 4*x*(x2*z1-x1*z2)^2
      }
    }
  
  /* computes nP from P=(x:z) and puts the result in (x:z). Assumes n>2. */
  void prac(
    int n,
    int[] x,
    int[] z,
    int[] xT,
    int[] zT,
    int[] xT2,
    int[] zT2)
  {
    int d, e, r, i;
    int[] t;
    int[] xA = x, zA = z;
    int[] xB = Aux1, zB = Aux2;
    int[] xC = Aux3, zC = Aux4;
    double v[] =
      {
        1.61803398875,
        1.72360679775,
        1.618347119656,
        1.617914406529,
        1.612429949509,
        1.632839806089,
        1.620181980807,
        1.580178728295,
        1.617214616534,
        1.38196601125 };

    /* chooses the best value of v */
    r = StaticFunctions.lucas_cost(n, v[0]);
    i = 0;
    for (d = 1; d < 10; d++)
    {
      e = StaticFunctions.lucas_cost(n, v[d]);
      if (e < r)
      {
        r = e;
        i = d;
      }
    }
    d = n;
    r = (int) ((double) d / v[i] + 0.5);
    /* first iteration always begins by Condition 3, then a swap */
    d = n - r;
    e = 2 * r - n;
    System.arraycopy(xA, 0, xB, 0, N.size()); // B = A
    System.arraycopy(zA, 0, zB, 0, N.size());
    System.arraycopy(xA, 0, xC, 0, N.size()); // C = A
    System.arraycopy(zA, 0, zC, 0, N.size());
    duplicate(xA, zA, xA, zA); /* A=2*A */
    while (d != e)
    {
      if (d < e)
      {
        r = d;
        d = e;
        e = r;
        t = xA;
        xA = xB;
        xB = t;
        t = zA;
        zA = zB;
        zB = t;
      }
      /* do the first line of Table 4 whose condition qualifies */
      if (4 * d <= 5 * e && ((d + e) % 3) == 0)
      { /* condition 1 */
        r = (2 * d - e) / 3;
        e = (2 * e - d) / 3;
        d = r;
        add3(xT, zT, xA, zA, xB, zB, xC, zC); /* T = f(A,B,C) */
        add3(xT2, zT2, xT, zT, xA, zA, xB, zB); /* T2 = f(T,A,B) */
        add3(xB, zB, xB, zB, xT, zT, xA, zA); /* B = f(B,T,A) */
        t = xA;
        xA = xT2;
        xT2 = t;
        t = zA;
        zA = zT2;
        zT2 = t; /* swap A and T2 */
      }
      else if (4 * d <= 5 * e && (d - e) % 6 == 0)
      { /* condition 2 */
        d = (d - e) / 2;
        add3(xB, zB, xA, zA, xB, zB, xC, zC); /* B = f(A,B,C) */
        duplicate(xA, zA, xA, zA); /* A = 2*A */
      }
      else if (d <= (4 * e))
      { /* condition 3 */
        d -= e;
        add3(xT, zT, xB, zB, xA, zA, xC, zC); /* T = f(B,A,C) */
        t = xB;
        xB = xT;
        xT = xC;
        xC = t;
        t = zB;
        zB = zT;
        zT = zC;
        zC = t; /* circular permutation (B,T,C) */
      }
      else if ((d + e) % 2 == 0)
      { /* condition 4 */
        d = (d - e) / 2;
        add3(xB, zB, xB, zB, xA, zA, xC, zC); /* B = f(B,A,C) */
        duplicate(xA, zA, xA, zA); /* A = 2*A */
      }
      else if (d % 2 == 0)
      { /* condition 5 */
        d /= 2;
        add3(xC, zC, xC, zC, xA, zA, xB, zB); /* C = f(C,A,B) */
        duplicate(xA, zA, xA, zA); /* A = 2*A */
      }
      else if (d % 3 == 0)
      { /* condition 6 */
        d = d / 3 - e;
        duplicate(xT, zT, xA, zA); /* T1 = 2*A */
        add3(xT2, zT2, xA, zA, xB, zB, xC, zC); /* T2 = f(A,B,C) */
        add3(xA, zA, xT, zT, xA, zA, xA, zA); /* A = f(T1,A,A) */
        add3(xT, zT, xT, zT, xT2, zT2, xC, zC); /* T1 = f(T1,T2,C) */
        t = xC;
        xC = xB;
        xB = xT;
        xT = t;
        t = zC;
        zC = zB;
        zB = zT;
        zT = t; /* circular permutation (C,B,T) */
      }
      else if ((d + e) % 3 == 0)
      { /* condition 7 */
        d = (d - 2 * e) / 3;
        add3(xT, zT, xA, zA, xB, zB, xC, zC); /* T1 = f(A,B,C) */
        add3(xB, zB, xT, zT, xA, zA, xB, zB); /* B = f(T1,A,B) */
        duplicate(xT, zT, xA, zA);
        add3(xA, zA, xA, zA, xT, zT, xA, zA); /* A = 3*A */
      }
      else if ((d - e) % 3 == 0)
      { /* condition 8 */
        d = (d - e) / 3;
        add3(xT, zT, xA, zA, xB, zB, xC, zC); /* T1 = f(A,B,C) */
        add3(xC, zC, xC, zC, xA, zA, xB, zB); /* C = f(A,C,B) */
        t = xB;
        xB = xT;
        xT = t;
        t = zB;
        zB = zT;
        zT = t; /* swap B and T */
        duplicate(xT, zT, xA, zA);
        add3(xA, zA, xA, zA, xT, zT, xA, zA); /* A = 3*A */
      }
      else if (e % 2 == 0)
      { /* condition 9 */
        e /= 2;
        add3(xC, zC, xC, zC, xB, zB, xA, zA); /* C = f(C,B,A) */
        duplicate(xB, zB, xB, zB); /* B = 2*B */
      }
    }
    add3(x, z, xA, zA, xB, zB, xC, zC);
  }
}
