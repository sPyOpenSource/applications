
package ecm.PrimeTest;

import static Factorzations.ecm.StaticFunctions.LongToBigNbr;
import static Factorzations.ecm.StaticFunctions.MultBigNbrByLongModN;

import static BigIntegers.StaticFunctions.AddBigNbrModN;
import static BigIntegers.StaticFunctions.BigNbrIsZero;
import static BigIntegers.StaticFunctions.MultBigNbrModN;
import static BigIntegers.MontgomeryInteger.dDosALa31;
import static BigIntegers.MontgomeryInteger.dDosALa62;
import BigIntegers.AprtCleInteger;

/**
 *
 * @author spy
 */
public class AprtCleFinal {
    protected int i, j, G, H, I, J, K, P, Q, T, U, W, X;
    protected int IV, InvX, LEVELnow, NP, PK, PL, PM, SW, VK, TestedQs, TestingQs;
    protected int QQ, T1, T3, U1, U3, V1, V3;
    protected int LengthN, LengthS;
    protected long Mask;
    protected double dS;
    protected AprtCleInteger N;

    boolean Final() {
        LengthN = N.size();
        for (I = 0; I < N.size(); I++){
            N.variable.biN[I] = N.get(I);
            N.set(I, N.variable.biS[I]);
            N.variable.biR[I] = 0;
        }
        while (true){
          if (N.get(N.size() - 1) != 0)
            break;
          N.pop();
        }
        N.dN = (double) N.get(N.size() - 1);
        if (N.size() > 1)
          N.dN += (double) N.getIntArray()[N.size() - 2] / dDosALa31;
        if (N.size() > 2)
          N.dN += (double) N.getIntArray()[N.size() - 3] / dDosALa62;
        LengthS = N.size();
        dS = N.dN;
        N.MontgomeryMultR1[0] = 1;
        for (I = 1; I < N.size(); I++) N.MontgomeryMultR1[I] = 0;

        N.variable.biR[0] = 1;
        N.BigNbrModN(N.variable.biN, LengthN, N.variable.biT); // Compute N mod S
        for (J = 1; J <= T; J++){
          MultBigNbrModN(N.variable.biR, N.variable.biT, N.variable.biTmp, N.getIntArray(), N.size());
          for (i = N.size() - 1; i > 0; i--){
            if (N.variable.biTmp[i] != 0) break;
          }
          if (i == 0 && N.variable.biTmp[0] != 1) return true; // Number is prime
          while (true){
            if (N.variable.biTmp[N.size() - 1] != 0) break;
            N.pop();
          }
          for (I = 0; I < N.size(); I++) N.set(I, N.variable.biTmp[I]);
          N.dN = (double) N.getIntArray()[N.size() - 1];
          if (N.size() > 1)
            N.dN += (double) N.getIntArray()[N.size() - 2] / dDosALa31;
          if (N.size() > 2)
            N.dN += (double) N.getIntArray()[N.size() - 3] / dDosALa62;
          for (i = N.size() - 1; i > 0; i--){
            if (N.getIntArray()[i] != N.variable.biTmp[i]) break;
          }
          if (N.getIntArray()[i] > N.variable.biTmp[i]){
            N.BigNbrModN(N.variable.biN, LengthN, N.variable.biTmp); // Compute N mod R
            if (BigNbrIsZero(N.variable.biTmp, N.size()))
            { // If N is multiple of R..
              return false; // Number is composite
            }
          }
          N.dN = dS;
          N.setSize(LengthS);
          for (I = 0; I < N.size(); I++){
            N.variable.biR[I] = N.getIntArray()[I];
            N.getIntArray()[I] = N.variable.biS[I];
          }
        } /* End for J */
        return true; /* Number is prime */
    }
    
    void check() {
        if (P != 2){
            for (IV = 0; IV <= 1; IV++){
                for (X = 1; X < PK; X++){
                  for (I = 0; I < PK; I++){
                    for (J = 0; J < N.size(); J++) N.variable.aiJS[I][J] = N.variable.aiJ0[I][J];
                  }
                  if (X % P == 0) continue;
                  if (IV == 0){
                    LongToBigNbr(X, N.variable.biExp, N.size());
                  } else {
                    LongToBigNbr(VK * X / PK, N.variable.biExp, N.size());
                    if (VK * X / PK == 0) continue;
                  }
                  N.JS_E(PK, PL, PM, P);
                  for (I = 0; I < PK; I++){
                    for (J = 0; J < N.size(); J++)
                      N.variable.aiJW[I][J] = 0;
                  }
                  InvX = N.variable.aiInv[X];
                  for (I = 0; I < PK; I++){
                    J = I * InvX % PK;
                    AddBigNbrModN(N.variable.aiJW[J], N.variable.aiJS[I], N.variable.aiJW[J], N.getIntArray(), N.size());
                  }
                  N.NormalizeJW(PK, PL, PM, P);
                  if (IV == 0){
                    for (I = 0; I < PK; I++) {
                      for (J = 0; J < N.size(); J++) N.variable.aiJS[I][J] = N.variable.aiJ00[I][J];
                    }
                  } else {
                    for (I = 0; I < PK; I++) {
                      for (J = 0; J < N.size(); J++)
                        N.variable.aiJS[I][J] = N.variable.aiJ01[I][J];
                    }
                  }
                  N.JS_JW(PK, PL, PM, P);
                  if (IV == 0){
                    for (I = 0; I < PK; I++){
                      for (J = 0; J < N.size(); J++) N.variable.aiJ00[I][J] = N.variable.aiJS[I][J];
                    }
                  } else {
                    for (I = 0; I < PK; I++){
                      for (J = 0; J < N.size(); J++) N.variable.aiJ01[I][J] = N.variable.aiJS[I][J];
                    }
                  }
                } // end for X 
            } // end for IV 
        } else {
            if (K == 1){
                MultBigNbrByLongModN(N.MontgomeryMultR1, Q, N.variable.aiJ00[0], N.getIntArray(), N.size());
                for (J = 0; J < N.size(); J++) N.variable.aiJ01[0][J] = N.MontgomeryMultR1[J];
            } else {
                if (K == 2){
                  if (VK == 1){
                    for (J = 0; J < N.size(); J++) N.variable.aiJ01[0][J] = N.MontgomeryMultR1[J];
                  }
                  for (J = 0; J < N.size(); J++){
                    N.variable.aiJS[0][J] = N.variable.aiJ0[0][J];
                    N.variable.aiJS[1][J] = N.variable.aiJ0[1][J];
                  }
                  N.JS_2(PK, PL, PM, P);
                  if (VK == 3){
                    for (J = 0; J < N.size(); J++){
                      N.variable.aiJ01[0][J] = N.variable.aiJS[0][J];
                      N.variable.aiJ01[1][J] = N.variable.aiJS[1][J];
                    }
                  }
                  MultBigNbrByLongModN(N.variable.aiJS[0], Q, N.variable.aiJ00[0], N.getIntArray(), N.size());
                  MultBigNbrByLongModN(N.variable.aiJS[1], Q, N.variable.aiJ00[1], N.getIntArray(), N.size());
                } else {
                  for (IV = 0; IV <= 1; IV++){
                    for (X = 1; X < PK; X += 2){
                      for (I = 0; I <= PM; I++){
                        for (J = 0; J < N.size(); J++) N.variable.aiJS[I][J] = N.variable.aiJ1[I][J];
                      }
                      if (X % 8 == 5 || X % 8 == 7) continue;
                      if (IV == 0){
                        LongToBigNbr(X, N.variable.biExp, N.size());
                      } else {
                        LongToBigNbr(VK * X / PK, N.variable.biExp, N.size());
                        if (VK * X / PK == 0) continue;
                      }
                      N.JS_E(PK, PL, PM, P);
                      for (I = 0; I < PK; I++){
                        for (J = 0; J < N.size(); J++) N.variable.aiJW[I][J] = 0;
                      }
                      InvX = N.variable.aiInv[X];
                      for (I = 0; I < PK; I++){
                        J = I * InvX % PK;
                        AddBigNbrModN(N.variable.aiJW[J], N.variable.aiJS[I], N.variable.aiJW[J], N.getIntArray(), N.size());
                      }
                      N.NormalizeJW(PK, PL, PM, P);
                      if (IV == 0){
                        for (I = 0; I < PK; I++){
                          for (J = 0; J < N.size(); J++) N.variable.aiJS[I][J] = N.variable.aiJ00[I][J];
                        }
                      } else {
                        for (I = 0; I < PK; I++){
                          for (J = 0; J < N.size(); J++) N.variable.aiJS[I][J] = N.variable.aiJ01[I][J];
                        }
                      }
                      N.NormalizeJS(PK, PL, PM, P);
                      N.JS_JW(PK, PL, PM, P);
                      if (IV == 0){
                        for (I = 0; I < PK; I++){
                          for (J = 0; J < N.size(); J++) N.variable.aiJ00[I][J] = N.variable.aiJS[I][J];
                        }
                      } else {
                        for (I = 0; I < PK; I++){
                          for (J = 0; J < N.size(); J++) N.variable.aiJ01[I][J] = N.variable.aiJS[I][J];
                        }
                      }
                    } // end for X 
                    if (IV == 0 || VK % 8 == 1 || VK % 8 == 3) continue;
                    for (I = 0; I < PM; I++){
                      for (J = 0; J < N.size(); J++){
                        N.variable.aiJW[I][J] = N.variable.aiJ2[I][J];
                        N.variable.aiJS[I][J] = N.variable.aiJ01[I][J];
                      }
                    }
                    for (; I < PK; I++){
                      for (J = 0; J < N.size(); J++) N.variable.aiJW[I][J] = N.variable.aiJS[I][J] = 0;
                    }
                    N.JS_JW(PK, PL, PM, P);
                    for (I = 0; I < PM; I++){
                      for (J = 0; J < N.size(); J++) N.variable.aiJ01[I][J] = N.variable.aiJS[I][J];
                    }
                  } // end for IV 
                }
            }
        }
    }
}
