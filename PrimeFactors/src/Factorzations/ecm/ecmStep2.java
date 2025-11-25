
package Factorzations.ecm;

import BigIntegers.AprtCleInteger;
import static BigIntegers.FactorInteger.NLen;
import static BigIntegers.StaticFunctions.BigNbrAreEqual;
import static BigIntegers.StaticFunctions.BigNbrIsZero;
import ecm.BigIntegers.GCD;
import static ecm.BigIntegers.ModInv.ModInvBigNbr;

/**
 * small prime order
 * @author X. Wang
 */
public class ecmStep2 extends ecmStep1 {
    private final int[][] root = new int[480][NLen];
    private final int[] sieveidx = new int[480];
    private final int[] WX = new int[NLen], WZ = new int[NLen];

    public ecmStep2(AprtCleInteger val) {
        super(val);
    }
    
    /******************************************************
     * Second step (using improved standard continuation) 
     ******************************************************/
    public int step2(){
        int j = 0;
        for (u = 1; u < 2310; u += 2){
          if (u % 3 == 0 || u % 5 == 0 || u % 7 == 0 || u % 11 == 0)
            sieve2310[u / 2] = (byte) 1;
          else
            sieve2310[(sieveidx[j++] = u / 2)] = (byte) 0;
        }
        System.arraycopy(sieve2310, 0, sieve2310, 1155, 1155);
        System.arraycopy(X, 0, Xaux, 0, N.size()); // (X:Z) -> Q (output
        System.arraycopy(Z, 0, Zaux, 0, N.size()); //         from step 1)
        for (int Pass = 0; Pass < 2; Pass++){
          System.arraycopy(
            N.MontgomeryMultR1,
            0,
            GcdAccumulated,
            0,
            N.size());
          System.arraycopy(X, 0, UX, 0, N.size());
          System.arraycopy(Z, 0, UZ, 0, N.size()); // (UX:UZ) -> Q
          ModInvBigNbr(Z, Aux2, N.getIntArray(), N.size());
          N.MontgomeryMult(Aux2, N.MontgomeryMultAfterInv, Aux1);
          N.MontgomeryMult(Aux1, X, root[0]); // root[0] <- X/Z (Q)
          int J = 0;
          StaticFunctions.AddBigNbrModN(X, Z, Aux1, N.getIntArray(), N.size());
          N.MontgomeryMult(Aux1, Aux1, W1);
          StaticFunctions.SubtractBigNbrModN(X, Z, Aux1, N.getIntArray(), N.size());
          N.MontgomeryMult(Aux1, Aux1, W2);
          N.MontgomeryMult(W1, W2, TX);
          StaticFunctions.SubtractBigNbrModN(W1, W2, Aux1, N.getIntArray(), N.size());
          N.MontgomeryMult(Aux1, AA, Aux2);
          StaticFunctions.AddBigNbrModN(Aux2, W2, Aux3, N.getIntArray(), N.size());
          N.MontgomeryMult(Aux1, Aux3, TZ); // (TX:TZ) -> 2Q
          StaticFunctions.SubtractBigNbrModN(X, Z, Aux1, N.getIntArray(), N.size());
          StaticFunctions.AddBigNbrModN(TX, TZ, Aux2, N.getIntArray(), N.size());
          N.MontgomeryMult(Aux1, Aux2, W1);
          StaticFunctions.AddBigNbrModN(X, Z, Aux1, N.getIntArray(), N.size());
          StaticFunctions.SubtractBigNbrModN(TX, TZ, Aux2, N.getIntArray(), N.size());
          N.MontgomeryMult(Aux1, Aux2, W2);
          StaticFunctions.AddBigNbrModN(W1, W2, Aux1, N.getIntArray(), N.size());
          N.MontgomeryMult(Aux1, Aux1, Aux2);
          N.MontgomeryMult(Aux2, UZ, X);
          StaticFunctions.SubtractBigNbrModN(W1, W2, Aux1, N.getIntArray(), N.size());
          N.MontgomeryMult(Aux1, Aux1, Aux2);
          N.MontgomeryMult(Aux2, UX, Z); // (X:Z) -> 3Q
          for (int I = 5; I < 2310; I += 2){
            System.arraycopy(X, 0, WX, 0, N.size());
            System.arraycopy(Z, 0, WZ, 0, N.size());
            StaticFunctions.SubtractBigNbrModN(X, Z, Aux1, N.getIntArray(), N.size());
            StaticFunctions.AddBigNbrModN(TX, TZ, Aux2, N.getIntArray(), N.size());
            N.MontgomeryMult(Aux1, Aux2, W1);
            StaticFunctions.AddBigNbrModN(X, Z, Aux1, N.getIntArray(), N.size());
            StaticFunctions.SubtractBigNbrModN(TX, TZ, Aux2, N.getIntArray(), N.size());
            N.MontgomeryMult(Aux1, Aux2, W2);
            StaticFunctions.AddBigNbrModN(W1, W2, Aux1, N.getIntArray(), N.size());
            N.MontgomeryMult(Aux1, Aux1, Aux2);
            N.MontgomeryMult(Aux2, UZ, X);
            StaticFunctions.SubtractBigNbrModN(W1, W2, Aux1, N.getIntArray(), N.size());
            N.MontgomeryMult(Aux1, Aux1, Aux2);
            N.MontgomeryMult(Aux2, UX, Z); // (X:Z) -> 5Q, 7Q, ...
            if (Pass == 0){
                N.MontgomeryMult(GcdAccumulated, Aux1, Aux2);
                System.arraycopy(Aux2, 0, GcdAccumulated, 0, N.size());
            } else {
                GCD.GcdBigNbr(Aux1, N.getIntArray(), GD, N.size());
                if (!BigNbrAreEqual(GD, BigNbr1, N.size()))
                    return 1;
            }
            if (I == 1155){
              System.arraycopy(X, 0, DX, 0, N.size());
              System.arraycopy(Z, 0, DZ, 0, N.size()); // (DX:DZ) -> 1155Q
            }
            if (I % 3 != 0 && I % 5 != 0 && I % 7 != 0 && I % 11 != 0){
              J++;
              ModInvBigNbr(Z, Aux2, N.getIntArray(), N.size());
              N.MontgomeryMult(Aux2, N.MontgomeryMultAfterInv, Aux1);
              N.MontgomeryMult(Aux1, X, root[J]); // root[J] <- X/Z
            }
            System.arraycopy(WX, 0, UX, 0, N.size()); // (UX:UZ) <-
            System.arraycopy(WZ, 0, UZ, 0, N.size()); // Previous (X:Z)
          } // end for I 
          StaticFunctions.AddBigNbrModN(DX, DZ, Aux1, N.getIntArray(), N.size());
          N.MontgomeryMult(Aux1, Aux1, W1);
          StaticFunctions.SubtractBigNbrModN(DX, DZ, Aux1, N.getIntArray(), N.size());
          N.MontgomeryMult(Aux1, Aux1, W2);
          N.MontgomeryMult(W1, W2, X);
          StaticFunctions.SubtractBigNbrModN(W1, W2, Aux1, N.getIntArray(), N.size());
          N.MontgomeryMult(Aux1, AA, Aux2);
          StaticFunctions.AddBigNbrModN(Aux2, W2, Aux3, N.getIntArray(), N.size());
          N.MontgomeryMult(Aux1, Aux3, Z);
          System.arraycopy(X, 0, UX, 0, N.size());
          System.arraycopy(Z, 0, UZ, 0, N.size()); // (UX:UZ) -> 2310Q
          StaticFunctions.AddBigNbrModN(X, Z, Aux1, N.getIntArray(), N.size());
          N.MontgomeryMult(Aux1, Aux1, W1);
          StaticFunctions.SubtractBigNbrModN(X, Z, Aux1, N.getIntArray(), N.size());
          N.MontgomeryMult(Aux1, Aux1, W2);
          N.MontgomeryMult(W1, W2, TX);
          StaticFunctions.SubtractBigNbrModN(W1, W2, Aux1, N.getIntArray(), N.size());
          N.MontgomeryMult(Aux1, AA, Aux2);
          StaticFunctions.AddBigNbrModN(Aux2, W2, Aux3, N.getIntArray(), N.size());
          N.MontgomeryMult(Aux1, Aux3, TZ); // (TX:TZ) -> 2*2310Q
          StaticFunctions.SubtractBigNbrModN(X, Z, Aux1, N.getIntArray(), N.size());
          StaticFunctions.AddBigNbrModN(TX, TZ, Aux2, N.getIntArray(), N.size());
          N.MontgomeryMult(Aux1, Aux2, W1);
          StaticFunctions.AddBigNbrModN(X, Z, Aux1, N.getIntArray(), N.size());
          StaticFunctions.SubtractBigNbrModN(TX, TZ, Aux2, N.getIntArray(), N.size());
          N.MontgomeryMult(Aux1, Aux2, W2);
          StaticFunctions.AddBigNbrModN(W1, W2, Aux1, N.getIntArray(), N.size());
          N.MontgomeryMult(Aux1, Aux1, Aux2);
          N.MontgomeryMult(Aux2, UZ, X);
          StaticFunctions.SubtractBigNbrModN(W1, W2, Aux1, N.getIntArray(), N.size());
          N.MontgomeryMult(Aux1, Aux1, Aux2);
          N.MontgomeryMult(Aux2, UX, Z); // (X:Z) -> 3*2310Q
          int Qaux = (int) (L1 / 4620);
          int maxIndexM = (int) (L2 / 4620);
          for (indexM = 0; indexM <= maxIndexM; indexM++){
            if (indexM >= Qaux)
            { // If inside step 2 range... 
              if (indexM == 0){
                ModInvBigNbr(UZ, Aux2, N.getIntArray(), N.size());
                N.MontgomeryMult(Aux2, N.MontgomeryMultAfterInv, Aux3);
                N.MontgomeryMult(UX, Aux3, Aux1); // Aux1 <- X/Z (2310Q)
              } else {
                ModInvBigNbr(Z, Aux2, N.getIntArray(), N.size());
                N.MontgomeryMult(Aux2, N.MontgomeryMultAfterInv, Aux3);
                N.MontgomeryMult(X, Aux3, Aux1); // Aux1 <- X/Z (3,5,*
              } //              2310Q)

              // Generate sieve 
              if (indexM % 10 == 0 || indexM == Qaux){
                StaticFunctions.GenerateSieve(
                  indexM / 10 * 46200 + 1,
                  sieve,
                  sieve2310,
                  N.getSmallPrimes());
              }
              // Walk through sieve 
              J = 1155 + (indexM % 10) * 2310;
              for (int i = 0; i < 480; i++){
                j = sieveidx[i]; // 0 < J < 1155
                if (sieve[J + j] != 0 && sieve[J - 1 - j] != 0)
                  continue; // Do not process if both are composite numbers.
                StaticFunctions.SubtractBigNbrModN(Aux1, root[i], M, N.getIntArray(), N.size());
                N.MontgomeryMult(GcdAccumulated, M, Aux2);
                System.arraycopy(Aux2, 0, GcdAccumulated, 0, N.size());
              }
              if (Pass != 0){
                GCD.GcdBigNbr(GcdAccumulated, N.getIntArray(), GD, N.size());
                if (!BigNbrAreEqual(GD, BigNbr1, N.size()))
                  return 1;
              }
            }
            if (indexM != 0)
            { // Update (X:Z)
              System.arraycopy(X, 0, WX, 0, N.size());
              System.arraycopy(Z, 0, WZ, 0, N.size());
              StaticFunctions.SubtractBigNbrModN(X, Z, Aux1, N.getIntArray(), N.size());
              StaticFunctions.AddBigNbrModN(TX, TZ, Aux2, N.getIntArray(), N.size());
              N.MontgomeryMult(Aux1, Aux2, W1);
              StaticFunctions.AddBigNbrModN(X, Z, Aux1, N.getIntArray(), N.size());
              StaticFunctions.SubtractBigNbrModN(TX, TZ, Aux2, N.getIntArray(), N.size());
              N.MontgomeryMult(Aux1, Aux2, W2);
              StaticFunctions.AddBigNbrModN(W1, W2, Aux1, N.getIntArray(), N.size());
              N.MontgomeryMult(Aux1, Aux1, Aux2);
              N.MontgomeryMult(Aux2, UZ, X);
              StaticFunctions.SubtractBigNbrModN(W1, W2, Aux1, N.getIntArray(), N.size());
              N.MontgomeryMult(Aux1, Aux1, Aux2);
              N.MontgomeryMult(Aux2, UX, Z);
              System.arraycopy(WX, 0, UX, 0, N.size());
              System.arraycopy(WZ, 0, UZ, 0, N.size());
            }
          } // end for Q
          if (Pass == 0){
            if (BigNbrIsZero(GcdAccumulated, N.size()))
            { // If GcdAccumulated is
              System.arraycopy(Xaux, 0, X, 0, N.size());
              System.arraycopy(Zaux, 0, Z, 0, N.size());
              continue; // multiple of TestNbr, continue.
            }
            GCD.GcdBigNbr(GcdAccumulated, N.getIntArray(), GD, N.size());
            if (BigNbrAreEqual(GD, N.getIntArray(), N.size()))
              break;
            if (!BigNbrAreEqual(GD, BigNbr1, N.size()))
              return 1;
            break;
          }
        } // end for Pass 
        // End curve calculation 
        return 0;
    }
}
