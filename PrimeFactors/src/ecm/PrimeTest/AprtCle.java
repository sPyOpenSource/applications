
package ecm.PrimeTest;

import BigIntegers.AprtCleInteger;
import static Factorzations.ecm.StaticFunctions.MultBigNbrByLongModN;

import static BigIntegers.StaticFunctions.AddBigNbrModN;
import static BigIntegers.StaticFunctions.BigNbrAreEqual;
import static BigIntegers.StaticFunctions.BigNbrIsZero;

import static calculator.largemodel.Divide.DivBigNbrByLong;
import static calculator.largemodel.Multiply.MultBigNbrByLong;
import static ecm.PrimeTest.AprtCleVariable.LEVELmax;
import static ecm.PrimeTest.AprtCleVariable.PWmax;

/**
 *
 * @author X. Wang
 */
public class AprtCle extends AprtCleFinal{
    public static void main(String[] args) {
        AprtCle aprtCle = new AprtCle();
        boolean isPrime = aprtCle.isProbablePrime(new AprtCleInteger("4811081079755727264065584381117"));
        System.out.println(isPrime);
    }
        
  // Prime checking routine
  // Return codes: 0 = Number is prime.
  //               1 = Number is composite.  
  public boolean isProbablePrime(AprtCleInteger N)
  {
    this.N = N;
    j = PK = PL = PM = 0;
    for (I = 0; I < N.size(); I++){
      N.variable.biS[I] = 0;
      for (J = 0; J < PWmax; J++){
        N.variable.aiJX[J][I] = 0;
      }
    }
    GetPrimes2Test : for (i = 0; i < LEVELmax; i++){
      N.variable.biS[0] = 2;
      for (I = 1; I < N.size(); I++){
        N.variable.biS[I] = 0;
      }
      for (j = 0; j < AprtCleVariable.aiNQ[i]; j++){
        Q = AprtCleVariable.aiQ[j];
        U = AprtCleVariable.aiT[i] * Q;
        do {
          U /= Q;
          MultBigNbrByLong(N.variable.biS, Q, N.variable.biS, N.size());
        } while (U % Q == 0);
        if (N.CompareSquare(N.variable.biS, N.variable.biTmp) > 0)
          break GetPrimes2Test;
      } // End for j 
    } // End for i 
    if (i == LEVELmax)
    { // too big 
      return N.isProbablePrimeRobin();
    }
    LEVELnow = i;
    TestingQs = j;
    T = AprtCleVariable.aiT[LEVELnow];
    NP = AprtCleVariable.aiNP[LEVELnow];
    MainStart : while (true){
      for (i = 0; i < NP; i++){
        P = AprtCleVariable.aiP[i];
        SW = TestedQs = 0;
        Q = W = (int) N.BigNbrModLong(P * P);
        for (J = P - 2; J > 0; J--) W = (W * Q) % (P * P);
        if (P > 2 && W != 1){
          SW = 1;
        }
        while (true){
          for (j = TestedQs; j <= TestingQs; j++){
            Q = AprtCleVariable.aiQ[j] - 1;
            G = AprtCleVariable.aiG[j];
            K = 0;
            while (Q % P == 0){
              K++;
              Q /= P;
            }
            Q = AprtCleVariable.aiQ[j];
            if (K == 0) continue;
            //System.out.println("P = " + P + ",  Q = " + Q + "  (" + (i * (TestingQs + 1) + j) * 100 / (NP * (TestingQs + 1)) + "%)");
            PM = 1;
            for (I = 1; I < K; I++) PM = PM * P;
            PL = (P - 1) * PM;
            PK = P * PM;
            J = 1;
            for (I = 1; I < Q; I++){
              J = J * G % Q;
              N.variable.aiIndx[J] = I;
            }
            J = 1;
            for (I = 1; I <= Q - 2; I++){
              J = J * G % Q;
              N.variable.aiF[I] = N.variable.aiIndx[(Q + 1 - J) % Q];
            }
            for (I = 0; I < PK; I++){
              for (J = 0; J < N.size(); J++){
                N.variable.aiJ0[I][J] = N.variable.aiJ1[I][J] = 0;
              }
            }
            if (P > 2){
              N.JacobiSum(1, 1, P, PK, PL, PM, Q);
            } else {
              if (K != 1){
                N.JacobiSum(1, 1, P, PK, PL, PM, Q);
                for (I = 0; I < PK; I++){
                  for (J = 0; J < N.size(); J++){
                    N.variable.aiJW[I][J] = 0;
                  }
                }
                if (K != 2){
                  for (I = 0; I < PM; I++){
                    for (J = 0; J < N.size(); J++){
                      N.variable.aiJW[I][J] = N.variable.aiJ0[I][J];
                    }
                  }
                  N.JacobiSum(2, 1, P, PK, PL, PM, Q);
                  for (I = 0; I < PM; I++){
                    for (J = 0; J < N.size(); J++){
                      N.variable.aiJS[I][J] = N.variable.aiJ0[I][J];
                    }
                  }
                  N.JS_JW(PK, PL, PM, P);
                  N.NormalizeJS(PK, PL, PM, P);
                  for (I = 0; I < PM; I++){
                    for (J = 0; J < N.size(); J++){
                      N.variable.aiJ1[I][J] = N.variable.aiJS[I][J];
                    }
                  }
                  N.JacobiSum(3 << (K - 3), 1 << (K - 3), P, PK, PL, PM, Q);
                  for (J = 0; J < N.size(); J++){
                    for (I = 0; I < PK; I++){
                      N.variable.aiJW[I][J] = 0;
                    }
                    for (I = 0; I < PM; I++){
                      N.variable.aiJS[I][J] = N.variable.aiJ0[I][J];
                    }
                  }
                  N.JS_2(PK, PL, PM, P);
                  N.NormalizeJS(PK, PL, PM, P);
                  for (I = 0; I < PM; I++){
                    for (J = 0; J < N.size(); J++){
                      N.variable.aiJ2[I][J] = N.variable.aiJS[I][J];
                    }
                  }
                }
              }
            }
            for (J = 0; J < N.size(); J++){
              N.variable.aiJ00[0][J] = N.variable.aiJ01[0][J] = N.MontgomeryMultR1[J];
              for (I = 1; I < PK; I++){
                N.variable.aiJ00[I][J] = N.variable.aiJ01[I][J] = 0;
              }
            }
            VK = (int) N.BigNbrModLong(PK);
            for (I = 1; I < PK; I++){
              if (I % P != 0){
                U1 = 1;
                U3 = I;
                V1 = 0;
                V3 = PK;
                while (V3 != 0){
                  QQ = U3 / V3;
                  T1 = U1 - V1 * QQ;
                  T3 = U3 - V3 * QQ;
                  U1 = V1;
                  U3 = V3;
                  V1 = T1;
                  V3 = T3;
                }
                N.variable.aiInv[I] = (U1 + PK) % PK;
              } else {
                N.variable.aiInv[I] = 0;
              }
            }
            check();
            for (I = 0; I < PL; I++){
              for (J = 0; J < N.size(); J++){
                N.variable.aiJS[I][J] = N.variable.aiJ00[I][J];
              }
            }
            for (; I < PK; I++){
              for (J = 0; J < N.size(); J++){
                N.variable.aiJS[I][J] = 0;
              }
            }
            DivBigNbrByLong(N.getIntArray(), PK, N.variable.biExp, N.size());
            N.JS_E(PK, PL, PM, P);
            for (I = 0; I < PK; I++){
              for (J = 0; J < N.size(); J++){
                N.variable.aiJW[I][J] = 0;
              }
            }
            for (I = 0; I < PL; I++){
              for (J = 0; J < PL; J++){
                N.MontgomeryMult(N.variable.aiJS[I], N.variable.aiJ01[J], N.variable.biTmp);
                AddBigNbrModN(N.variable.biTmp, N.variable.aiJW[(I + J) % PK], N.variable.aiJW[(I + J) % PK], N.getIntArray(), N.size());
              }
            }
            N.NormalizeJW(PK, PL, PM, P);
            MatchingRoot : do {
              H = -1;
              W = 0;
              for (I = 0; I < PL; I++){
                if (!BigNbrIsZero(N.variable.aiJW[I], N.size())){
                  if (H == -1 && BigNbrAreEqual(N.variable.aiJW[I], N.MontgomeryMultR1, N.size())){
                    H = I;
                  } else {
                    H = -2;
                    AddBigNbrModN(N.variable.aiJW[I], N.MontgomeryMultR1, N.variable.biTmp, N.getIntArray(), N.size());
                    if (BigNbrIsZero(N.variable.biTmp, N.size())) W++;
                  }
                }
              }
              if (H >= 0) break;

              if (W != P - 1) return false; // Not prime 

              for (I = 0; I < PM; I++){
                AddBigNbrModN(N.variable.aiJW[I], N.MontgomeryMultR1, N.variable.biTmp, N.getIntArray(), N.size());
                if (BigNbrIsZero(N.variable.biTmp, N.size())) break;
              }
              if (I == PM) return false; // Not prime 

              for (J = 1; J <= P - 2; J++){
                AddBigNbrModN(N.variable.aiJW[I + J * PM], N.MontgomeryMultR1, N.variable.biTmp, N.getIntArray(), N.size());
                if (!BigNbrIsZero(N.variable.biTmp, N.size())) return false; // Not prime 
              }

              H = I + PL;
            } while (false);
            if (SW == 1 || H % P == 0) continue;
            if (P != 2){
              SW = 1;
              continue;
            }
            if (K == 1){
              if ((N.get(0) & 3) == 1) SW = 1;
              continue;
            }

            //System.out.println("if (Q^((N-1)/2) mod N != N-1), N is not prime.");

            MultBigNbrByLongModN(N.MontgomeryMultR1, Q, N.variable.biTmp, N.getIntArray(), N.size());
            for (I = 0; I < N.size(); I++) N.variable.biR[I] = N.variable.biTmp[I];
            I = N.size() - 1;
            Mask = 0x40000000L;
            while ((N.get(I) & Mask) == 0){
              Mask /= 2;
              if (Mask == 0){
                I--;
                Mask = 0x40000000L;
              }
            }
            do {
              Mask /= 2;
              if (Mask == 0){
                I--;
                Mask = 0x40000000L;
              }
              N.MontgomeryMult(N.variable.biR, N.variable.biR, N.variable.biT);
              for (J = 0; J < N.size(); J++) N.variable.biR[J] = N.variable.biT[J];
              if ((N.get(I) & Mask) != 0){
                N.MontgomeryMult(N.variable.biR, N.variable.biTmp, N.variable.biT);
                for (J = 0; J < N.size(); J++) N.variable.biR[J] = N.variable.biT[J];
              }
            } while (I > 0 || Mask > 2);
            AddBigNbrModN(N.variable.biR, N.MontgomeryMultR1, N.variable.biTmp, N.getIntArray(), N.size());
            if (!BigNbrIsZero(N.variable.biTmp, N.size())) return false; // Not prime 
            SW = 1;
          } // end for j 
          if (SW == 0){
            TestedQs = TestingQs + 1;
            if (TestingQs < AprtCleVariable.aiNQ[LEVELnow] - 1){
              TestingQs++;
              Q = AprtCleVariable.aiQ[TestingQs];
              U = T * Q;
              do {
                MultBigNbrByLong(N.variable.biS, Q, N.variable.biS, N.size());
                U /= Q;
              } while (U % Q == 0);
              continue; // Retry 
            }
            LEVELnow++;
            if (LEVELnow == LEVELmax) return N.isProbablePrimeRobin(); // Cannot tell 
            T = AprtCleVariable.aiT[LEVELnow];
            NP = AprtCleVariable.aiNP[LEVELnow];
            N.variable.biS[0] = 2;
            for (J = 1; J < N.size(); J++) N.variable.biS[J] = 0;
            for (J = 0; J <= AprtCleVariable.aiNQ[LEVELnow]; J++){
              Q = AprtCleVariable.aiQ[J];
              U = T * Q;
              do {
                MultBigNbrByLong(N.variable.biS, Q, N.variable.biS, N.size());
                U /= Q;
              } while (U % Q == 0);
              if (N.CompareSquare(N.variable.biS, N.variable.biTmp) > 0){
                TestingQs = J;
                continue MainStart; // Retry from the beginning 
              }
            } // end for J 
            return N.isProbablePrimeRobin(); // Program error 
          } // end if 
          break;
        } // end for (;;) 
      } // end for i 

    return Final();
    }
  }
}
