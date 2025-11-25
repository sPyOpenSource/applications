
package BigIntegers;

import static Factorzations.ecm.StaticFunctions.Cos;
import static Factorzations.siqs.StaticFunctions.JacobiSymbol;
import java.math.BigInteger;

/**
 *
 * @author X. Wang
 */
public class ECMBigInteger extends FactorInteger {
    public int digitsInGroup;
    private int DegreeAurif;
    private final long Gamma[] = new long[386], Delta[] = new long[386], AurifQ[] = new long[386];
    public BigInteger[] Quad = new BigInteger[4];
              
    public ECMBigInteger(String val) {
        super(val);
        digits = new int[NLen];
        base = 1 << 31;
    }
    
    public BigInteger Sum(){
        BigInteger N1 = BigInteger.ONE;
          for (int i = 0; i < NbrFactors; i++)
          {
            N1 =
              N1.multiply(PD[i].pow(Exp[i] + 1).subtract(BigInteger.ONE)).divide(
                PD[i].subtract(BigInteger.ONE));
          }
          return N1;
    }
    
    public BigInteger Totient(){
        BigInteger N1 = this;
          for (int i = 0; i < NbrFactors; i++)
          {
            N1 = N1.multiply(PD[i].subtract(BigInteger.ONE)).divide(PD[i]);
          }
          return N1;
    }
    
    public static int Totient(int N){
            int totient, q;
            totient = q = N;
            if (q % 2 == 0){
                totient /= 2;
                do {
                    q /= 2;
                } while (q % 2 == 0);
            }
            if (q % 3 == 0){
                totient = totient * 2 / 3;
                do {
                    q /= 3;
                } while (q % 3 == 0);
            }
            int k = 5;
            while (k * k <= q){
                if (k % 3 != 0 && q % k == 0){
                    totient = totient * (k - 1) / k;
                    do {
                        q /= k;
                    } while (q % k == 0);
                }
                k += 2;
            }
            if (q > 1)
                totient = totient * (q - 1) / q;
            return totient;
    }
    
    public int Moebius(){
        int j = 1; // Compute Moebius
          for (int i = 0; i < NbrFactors; i++)
          {
            if (Exp[i] == 1)
            {
              j = -j;
            }
            else
            {
              j = 0;
            }
          }
          return j;
    }

    public static int Moebius(int N){
            int moebius = 1;
            int q       = N;
            if (q % 2 == 0){
                moebius = -moebius;
                q /= 2;
                if (q % 2 == 0)
                    return 0;
            }
            if (q % 3 == 0){
                moebius = -moebius;
                q /= 3;
                if (q % 3 == 0)
                    return 0;
            }
            int k = 5;
            while (k * k <= q){
                if (k % 3 != 0){
                    while (q % k == 0){
                        moebius = -moebius;
                        q /= k;
                        if (q % k == 0)
                            return 0;
                    }
                }
                k += 2;
            }
            if (q > 1)
                moebius = -moebius;
            return moebius;
    }
    
    public void InsertAurifFactors(BigInteger BigBase, int Expon, int Incre){
        int t1, t2, t3, N, N1, q, L, j, k, Base;
        if (BigBase.compareTo(BigInteger.valueOf(386)) <= 0){
            Base = BigBase.intValue();
            if (Expon % 2 == 0 && Incre == -1){
                do {
                    Expon /= 2;
            } while (Expon % 2 == 0);
            Incre = Base % 4 - 2;
        }
        if (Expon % Base == 0
          && Expon / Base % 2 != 0
          && ((Base % 4 != 1 && Incre == 1) || (Base % 4 == 1 && Incre == -1)))
        {
            N = Base;
            if (N % 4 == 1)
                N1 = N;
            else
                N1 = 2 * N;
            DegreeAurif = Totient(N1) / 2;
            for (k = 1; k <= DegreeAurif; k += 2)
                AurifQ[k] = JacobiSymbol(N, k);
            for (k = 2; k <= DegreeAurif; k += 2){
                t1 = k; // Calculate t2 = gcd(k, N1)
                t2 = N1;
                while (t1 != 0){
                    t3 = t2 % t1;
                    t2 = t1;
                    t1 = t3;
                }
                AurifQ[k] = Moebius(N1 / t2) * Totient(t2) * Cos((N - 1) * k);
            }
            Gamma[0] = Delta[0] = 1;
            for (k = 1; k <= DegreeAurif / 2; k++){
                Gamma[k] = Delta[k] = 0;
                for (j = 0; j < k; j++){
                    Gamma[k] =
                    Gamma[k]
                      + N * AurifQ[2 * k
                      - 2 * j
                      - 1] * Delta[j]
                      - AurifQ[2 * k
                      - 2 * j] * Gamma[j];
                    Delta[k] =
                    Delta[k]
                      + AurifQ[2 * k
                      + 1
                      - 2 * j] * Gamma[j]
                      - AurifQ[2 * k
                      - 2 * j] * Delta[j];
                }
                Gamma[k] /= 2 * k;
                Delta[k] = (Delta[k] + Gamma[k]) / (2 * k + 1);
            }
        for (k = DegreeAurif / 2 + 1; k <= DegreeAurif; k++)
          Gamma[k] = Gamma[DegreeAurif - k];
        for (k = (DegreeAurif + 1) / 2; k < DegreeAurif; k++)
          Delta[k] = Delta[DegreeAurif - k - 1];
        q = Expon / Base;
        L = 1;
        while (L * L <= q){
          if (q % L == 0){
            GetAurifeuilleFactor(L, BigBase);
            if (q != L * L)
              GetAurifeuilleFactor(q / L, BigBase);
          }
          L += 2;
        }
      }
    }
  }
  
  void GetAurifeuilleFactor(int L, BigInteger BigBase){
    BigInteger X, Csal, Dsal, Nro1;

      X = BigBase.pow(L);
      Csal = Dsal = BigInteger.ONE;
      for (int k = 1; k < DegreeAurif; k++){
        Csal = Csal.multiply(X).add(BigInteger.valueOf(Gamma[k]));
        Dsal = Dsal.multiply(X).add(BigInteger.valueOf(Delta[k]));
      }
      Csal = Csal.multiply(X).add(BigInteger.valueOf(Gamma[DegreeAurif]));
      Nro1 = Dsal.multiply(BigBase.pow((L + 1) / 2));
      InsertFactor(Csal.add(Nro1));
      InsertFactor(Csal.subtract(Nro1));
  }
  
  public boolean ComputeFourSquares()
  {
      int indexPrimes;
      BigInteger p, q, K;
      BigInteger[] Tmp = new BigInteger[4], M = new BigInteger[4], Mult = new BigInteger[4];

      Quad[0] = BigInteger.ONE; /* 1 = 1^2 + 0^2 + 0^2 + 0^2 */
      Quad[1] = BigInteger.ZERO;
      Quad[2] = BigInteger.ZERO;
      Quad[3] = BigInteger.ZERO;
      for (indexPrimes = NbrFactors - 1; indexPrimes >= 0; indexPrimes--)
      {
        if (Exp[indexPrimes] % 2 == 0)
        {
          continue;
        }
        p = PD[indexPrimes];
        q = p.subtract(BigInteger.ONE); /* q = p-1 */
        if (p.equals(BigInteger.TWO))
        {
          Mult[0] = BigInteger.ONE; /* 2 = 1^2 + 1^2 + 0^2 + 0^2 */
          Mult[1] = BigInteger.ONE;
          Mult[2] = BigInteger.ZERO;
          Mult[3] = BigInteger.ZERO;
        }
        else
        { /* Prime not 2 */
          if (!p.testBit(1))
          { /* if p = 1 (mod 4) */
            K = BigInteger.ONE;
            do
            { // Compute Mult1 = sqrt(-1) mod p
              K = K.add(BigInteger.ONE);
              Mult[0] = K.modPow(q.shiftRight(2), p);
            }
            while (Mult[0].equals(BigInteger.ONE) || Mult[0].equals(q));
            if (!Mult[0].multiply(Mult[0]).mod(p).equals(q))
            {
              return false; /* The number is not prime */
            }
            Mult[1] = BigInteger.ONE;
            while (true)
            {
              K = Mult[0].multiply(Mult[0]).add(Mult[1].multiply(Mult[1])).divide(p);
              if (K.equals(BigInteger.ONE))
              {
                Mult[2] = BigInteger.ZERO;
                Mult[3] = BigInteger.ZERO;
                break;
              }
              if (p.mod(K).signum() == 0)
              {
                return false; /* The number is not prime */
              }
              M[0] = Mult[0].mod(K);
              M[1] = Mult[1].mod(K);
              if (M[0].compareTo(K.shiftRight(1)) > 0)
              {
                M[0] = M[0].subtract(K);
              }
              if (M[1].compareTo(K.shiftRight(1)) > 0)
              {
                M[1] = M[1].subtract(K);
              }
              Tmp[0]  = Mult[0].multiply(M[0]).add(Mult[1].multiply(M[1])).divide(K);
              Mult[1] = Mult[0].multiply(M[1]).subtract(Mult[1].multiply(M[0])).divide(K);
              Mult[0] = Tmp[0];
            } /* end while */
          } /* end p = 1 (mod 4) */
          else
          { /* if p = 3 (mod 4) */
            // Compute Mult1 and Mult2 so Mult1^2 + Mult2^2 = -1 (mod p)
            Mult[0] = BigInteger.ZERO;
            do
            {
              Mult[0] = Mult[0].add(BigInteger.ONE);
            }
            while (BigInteger.ONE
              .negate()
              .subtract(Mult[0].multiply(Mult[0]))
              .modPow(q.shiftRight(1), p)
              .compareTo(BigInteger.ONE)
              > 0);
            Mult[1] =
              BigInteger.ONE.negate().subtract(Mult[0].multiply(Mult[0])).modPow(
                p.add(BigInteger.ONE).shiftRight(2),
                p);
            Mult[2] = BigInteger.ONE;
            Mult[3] = BigInteger.ZERO;
            while (true)
            {
              K =
                Mult[0]
                  .multiply(Mult[0])
                  .add(Mult[1].multiply(Mult[1]))
                  .add(Mult[2].multiply(Mult[2]))
                  .add(Mult[3].multiply(Mult[3]))
                  .divide(p);
              if (K.equals(BigInteger.ONE))
              {
                break;
              }
              if (!K.testBit(0))
              { // If K is even ...
                if (Mult[0].add(Mult[1]).testBit(0))
                {
                  if (!Mult[0].add(Mult[2]).testBit(0))
                  {
                    Tmp[0]  = Mult[1];
                    Mult[1] = Mult[2];
                    Mult[2] = Tmp[0];
                  }
                  else
                  {
                    Tmp[0]  = Mult[1];
                    Mult[1] = Mult[3];
                    Mult[3] = Tmp[0];
                  }
                } // At this moment Mult1+Mult2 = even, Mult3+Mult4 = even
                Tmp[1]  = Mult[0].add(Mult[1]).shiftRight(1);
                Tmp[2]  = Mult[0].subtract(Mult[1]).shiftRight(1);
                Tmp[3]  = Mult[2].add(Mult[3]).shiftRight(1);
                Mult[3] = Mult[2].subtract(Mult[3]).shiftRight(1);
                Mult[2] = Tmp[3];
                Mult[1] = Tmp[2];
                Mult[0] = Tmp[1];
                continue;
              } /* end if k is even */
              for(int i = 0; i < M.length; i++){
                M[i] = Mult[i].mod(K);
                if (M[i].compareTo(K.shiftRight(1)) > 0)
                {
                  M[i] = M[i].subtract(K);
                }
              }
              Mult = Calculate(Mult, M, K);
            } /* end while */
          } /* end if p = 3 (mod 4) */
        } /* end prime not 2 */
        Quad = Calculate(Mult, Quad, BigInteger.ONE);
      } /* end for indexPrimes */
      for (indexPrimes = 0; indexPrimes < NbrFactors; indexPrimes++)
      {
        p = PD[indexPrimes].pow(Exp[indexPrimes] / 2);
        for(int i = 0; i < Quad.length; i++)
            Quad[i] = Quad[i].multiply(p);
      }
      for(int i = 0; i < Quad.length; i++)
        Quad[i] = Quad[i].abs();
      // Sort squares
      for(int i = 1; i < Quad.length; i++){
        if (Quad[0].compareTo(Quad[i]) < 0)
        {
          Tmp[0] = Quad[0];
          Quad[0] = Quad[i];
          Quad[i] = Tmp[0];
        }
      }
      for(int i = 2; i < Quad.length; i++){
        if (Quad[1].compareTo(Quad[i]) < 0)
        {
          Tmp[0] = Quad[1];
          Quad[1] = Quad[i];
          Quad[i] = Tmp[0];
        }
      }
      if (Quad[2].compareTo(Quad[3]) < 0)
      {
        Tmp[0] = Quad[2];
        Quad[2] = Quad[3];
        Quad[3] = Tmp[0];
      }
    return true;
  }

    private BigInteger[] Calculate(BigInteger[] Mult, BigInteger[] M, BigInteger K) {
        BigInteger[] Tmp = new BigInteger[4];
        Tmp[0] =
                Mult[0].multiply(M[0])
                        .add(Mult[1].multiply(M[1]))
                        .add(Mult[2].multiply(M[2]))
                        .add(Mult[3].multiply(M[3]))
                        .divide(K);
        Tmp[1] =
                Mult[0].multiply(M[1])
                        .subtract(Mult[1].multiply(M[0]))
                        .add(Mult[2].multiply(M[3]))
                        .subtract(Mult[3].multiply(M[2]))
                        .divide(K);
        Tmp[2] =
                Mult[0].multiply(M[2])
                        .subtract(Mult[2].multiply(M[0]))
                        .subtract(Mult[1].multiply(M[3]))
                        .add(Mult[3].multiply(M[1]))
                        .divide(K);
        Tmp[3] =
                Mult[0].multiply(M[3])
                        .subtract(Mult[3].multiply(M[0]))
                        .add(Mult[1].multiply(M[2]))
                        .subtract(Mult[2].multiply(M[1]))
                        .divide(K);
        return Tmp;
    }
}
