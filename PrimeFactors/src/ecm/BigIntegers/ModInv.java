
package ecm.BigIntegers;

import static BigIntegers.FactorInteger.NLen;
import BigIntegers.StaticFunctions;
import static calculator.largemodel.Add.AddBigNbr32;
import static calculator.largemodel.Subtract.SubtractBigNbr32;

/**
 *
 * @author X. Wang
 */
public class ModInv {
    static final long CalcAuxModInvBB[] = new long[NLen], CalcAuxModInvA[] = new long[NLen];
    static final long CalcAuxModInvB[] = new long[NLen], CalcAuxModInvMu[] = new long[NLen];
    static final long CalcAuxModInvGamma[] = new long[NLen];
    
    /***********************************************************************/
    /* NAME: ModInvBigNbr                                                  */
    /*                                                                     */
    /* PURPOSE: Find the inverse multiplicative modulo v.                  */
    /*                                                                     */
    /* The algorithm terminates with u1 = u^(-1) MOD v.                    */
    /**
     * @param a
     * @param inv
     * @param b
     * @param NumberLength
     ***********************************************************************/
  public static void ModInvBigNbr(int[] a, int[] inv, int[] b, int NumberLength){
        int i;
        int Dif, E;
        int st1, st2;
        long Yaa, Yab; // 2^E * A'     = Yaa A + Yab B
        long Yba, Ybb; // 2^E * B'     = Yba A + Ybb B
        long Ygb0;     // 2^E * Mu'    = Yaa Mu + Yab Gamma + Ymb0 B0
        long Ymb0;     // 2^E * Gamma' = Yba Mu + Ybb Gamma + Ygb0 B0
        int Iaa, Iab, Iba, Ibb;
        long Tmp1, Tmp2, Tmp3, Tmp4, Tmp5;
        int B0l;
        int invB0l;
        int Al, Bl, T1, Gl, Ml, P;
        long carry1, carry2, carry3, carry4;
        int Yaah, Yabh, Ybah, Ybbh;
        int Ymb0h, Ygb0h;
        long Pr1, Pr2, Pr3, Pr4, Pr5, Pr6, Pr7;
        long[] B = CalcAuxModInvBB;

        if (NumberLength >= 2 &&
            b[NumberLength-1] == 0 && b[NumberLength-2] < 0x40000000)
        {
          NumberLength--;
        }
        StaticFunctions.Convert31To32Bits(CalcAuxModInvA, a, NumberLength);
        StaticFunctions.Convert31To32Bits(CalcAuxModInvB, b, NumberLength);
        System.arraycopy(CalcAuxModInvB, 0, B, 0, NumberLength);
        B0l = (int)B[0];
        invB0l = B0l; // 2 least significant bits of inverse correct.
        invB0l = invB0l * (2 - B0l * invB0l); // 4 LSB of inverse correct.
        invB0l = invB0l * (2 - B0l * invB0l); // 8 LSB of inverse correct.
        invB0l = invB0l * (2 - B0l * invB0l); // 16 LSB of inverse correct.
        invB0l = invB0l * (2 - B0l * invB0l); // 32 LSB of inverse correct.
        for (i = NumberLength - 1; i >= 0; i--)
        {
          CalcAuxModInvGamma[i] = 0;
          CalcAuxModInvMu[i] = 0;
        }
        CalcAuxModInvMu[0] = 1;
        Dif = 0;
        outer_loop : while (true)
        {
          Iaa = Ibb = 1;
          Iab = Iba = 0;
          Al = (int) CalcAuxModInvA[0];
          Bl = (int) CalcAuxModInvB[0];
          E = 0;
          P = 1;
          if (Bl == 0)
          {
            for (i = NumberLength - 1; i >= 0; i--)
            {
              if (CalcAuxModInvB[i] != 0)
                break;
            }
            if (i < 0)
              break; // Go out of loop if CalcAuxModInvB = 0
          }
          while (true){
            T1 = 0;
            while ((Bl & 1) == 0)
            {
              if (E == 31)
              {
                Yaa = Iaa;
                Yab = Iab;
                Yba = Iba;
                Ybb = Ibb;
                Gl = (int) CalcAuxModInvGamma[0];
                Ml = (int) CalcAuxModInvMu[0];
                Dif++;
                T1++;
                Yaa <<= T1;
                Yab <<= T1;
                Ymb0 = (- (int) Yaa * Ml - (int) Yab * Gl) * invB0l;
                Ygb0 = (-Iba * Ml - Ibb * Gl) * invB0l;
                carry1 = carry2 = carry3 = carry4 = 0;
                Yaah = (int) (Yaa >> 32);
                Yabh = (int) (Yab >> 32);
                Ybah = (int) (Yba >> 32);
                Ybbh = (int) (Ybb >> 32);
                Ymb0h = (int) (Ymb0 >> 32);
                Ygb0h = (int) (Ygb0 >> 32);
                Yaa &= 0xFFFFFFFFL;
                Yab &= 0xFFFFFFFFL;
                Yba &= 0xFFFFFFFFL;
                Ybb &= 0xFFFFFFFFL;
                Ymb0 &= 0xFFFFFFFFL;
                Ygb0 &= 0xFFFFFFFFL;

                st1 = Yaah * 6 + Yabh * 2 + Ymb0h;
                st2 = Ybah * 6 + Ybbh * 2 + Ygb0h;
                for (i = 0; i < NumberLength; i++)
                {
                  Pr1 = Yaa * (Tmp1 = CalcAuxModInvMu[i]);
                  Pr2 = Yab * (Tmp2 = CalcAuxModInvGamma[i]);
                  Pr3 = Ymb0 * (Tmp3 = B[i]);
                  Pr4 =
                    (Pr1 & 0xFFFFFFFFL)
                      + (Pr2 & 0xFFFFFFFFL)
                      + (Pr3 & 0xFFFFFFFFL)
                      + carry3;
                  Pr5 = Yaa * (Tmp4 = CalcAuxModInvA[i]);
                  Pr6 = Yab * (Tmp5 = CalcAuxModInvB[i]);
                  Pr7 = (Pr5 & 0xFFFFFFFFL) + (Pr6 & 0xFFFFFFFFL) + carry1;
                  switch (st1)
                  {
                    case -9 :
                      carry3 = -Tmp1 - Tmp2 - Tmp3;
                      carry1 = -Tmp4 - Tmp5;
                      break;
                    case -8 :
                      carry3 = -Tmp1 - Tmp2;
                      carry1 = -Tmp4 - Tmp5;
                      break;
                    case -7 :
                      carry3 = -Tmp1 - Tmp3;
                      carry1 = -Tmp4;
                      break;
                    case -6 :
                      carry3 = -Tmp1;
                      carry1 = -Tmp4;
                      break;
                    case -5 :
                      carry3 = -Tmp1 + Tmp2 - Tmp3;
                      carry1 = -Tmp4 + Tmp5;
                      break;
                    case -4 :
                      carry3 = -Tmp1 + Tmp2;
                      carry1 = -Tmp4 + Tmp5;
                      break;
                    case -3 :
                      carry3 = -Tmp2 - Tmp3;
                      carry1 = -Tmp5;
                      break;
                    case -2 :
                      carry3 = -Tmp2;
                      carry1 = -Tmp5;
                      break;
                    case -1 :
                      carry3 = -Tmp3;
                      carry1 = 0;
                      break;
                    case 0 :
                      carry3 = 0;
                      carry1 = 0;
                      break;
                    case 1 :
                      carry3 = Tmp2 - Tmp3;
                      carry1 = Tmp5;
                      break;
                    case 2 :
                      carry3 = Tmp2;
                      carry1 = Tmp5;
                      break;
                    case 3 :
                      carry3 = Tmp1 - Tmp2 - Tmp3;
                      carry1 = Tmp4 - Tmp5;
                      break;
                    case 4 :
                      carry3 = Tmp1 - Tmp2;
                      carry1 = Tmp4 - Tmp5;
                      break;
                    case 5 :
                      carry3 = Tmp1 - Tmp3;
                      carry1 = Tmp4;
                      break;
                    case 6 :
                      carry3 = Tmp1;
                      carry1 = Tmp4;
                      break;
                    case 7 :
                      carry3 = Tmp1 + Tmp2 - Tmp3;
                      carry1 = Tmp4 + Tmp5;
                      break;
                    case 8 :
                      carry3 = Tmp1 + Tmp2;
                      carry1 = Tmp4 + Tmp5;
                      break;
                  }
                  carry3 += (Pr1 >>> 32) + (Pr2 >>> 32) + (Pr3 >>> 32) + (Pr4 >> 32);
                  carry1 += (Pr5 >>> 32) + (Pr6 >>> 32) + (Pr7 >> 32);
                  if (i > 0)
                  {
                    CalcAuxModInvMu[i - 1] = Pr4 & 0xFFFFFFFFL;
                    CalcAuxModInvA[i - 1] = Pr7 & 0xFFFFFFFFL;
                  }
                  Pr1 = Yba * Tmp1;
                  Pr2 = Ybb * Tmp2;
                  Pr3 = Ygb0 * Tmp3;
                  Pr4 =
                    (Pr1 & 0xFFFFFFFFL)
                      + (Pr2 & 0xFFFFFFFFL)
                      + (Pr3 & 0xFFFFFFFFL)
                      + carry4;
                  Pr5 = Yba * Tmp4;
                  Pr6 = Ybb * Tmp5;
                  Pr7 = (Pr5 & 0xFFFFFFFFL) + (Pr6 & 0xFFFFFFFFL) + carry2;
                  switch (st2)
                  {
                    case -9 :
                      carry4 = -Tmp1 - Tmp2 - Tmp3;
                      carry2 = -Tmp4 - Tmp5;
                      break;
                    case -8 :
                      carry4 = -Tmp1 - Tmp2;
                      carry2 = -Tmp4 - Tmp5;
                      break;
                    case -7 :
                      carry4 = -Tmp1 - Tmp3;
                      carry2 = -Tmp4;
                      break;
                    case -6 :
                      carry4 = -Tmp1;
                      carry2 = -Tmp4;
                      break;
                    case -5 :
                      carry4 = -Tmp1 + Tmp2 - Tmp3;
                      carry2 = -Tmp4 + Tmp5;
                      break;
                    case -4 :
                      carry4 = -Tmp1 + Tmp2;
                      carry2 = -Tmp4 + Tmp5;
                      break;
                    case -3 :
                      carry4 = -Tmp2 - Tmp3;
                      carry2 = -Tmp5;
                      break;
                    case -2 :
                      carry4 = -Tmp2;
                      carry2 = -Tmp5;
                      break;
                    case -1 :
                      carry4 = -Tmp3;
                      carry2 = 0;
                      break;
                    case 0 :
                      carry4 = 0;
                      carry2 = 0;
                      break;
                    case 1 :
                      carry4 = Tmp2 - Tmp3;
                      carry2 = Tmp5;
                      break;
                    case 2 :
                      carry4 = Tmp2;
                      carry2 = Tmp5;
                      break;
                    case 3 :
                      carry4 = Tmp1 - Tmp2 - Tmp3;
                      carry2 = Tmp4 - Tmp5;
                      break;
                    case 4 :
                      carry4 = Tmp1 - Tmp2;
                      carry2 = Tmp4 - Tmp5;
                      break;
                    case 5 :
                      carry4 = Tmp1 - Tmp3;
                      carry2 = Tmp4;
                      break;
                    case 6 :
                      carry4 = Tmp1;
                      carry2 = Tmp4;
                      break;
                    case 7 :
                      carry4 = Tmp1 + Tmp2 - Tmp3;
                      carry2 = Tmp4 + Tmp5;
                      break;
                    case 8 :
                      carry4 = Tmp1 + Tmp2;
                      carry2 = Tmp4 + Tmp5;
                      break;
                  }
                  carry4 += (Pr1 >>> 32) + (Pr2 >>> 32) + (Pr3 >>> 32) + (Pr4 >> 32);
                  carry2 += (Pr5 >>> 32) + (Pr6 >>> 32) + (Pr7 >> 32);
                  if (i > 0)
                  {
                    CalcAuxModInvGamma[i - 1] = Pr4 & 0xFFFFFFFFL;
                    CalcAuxModInvB[i - 1] = Pr7 & 0xFFFFFFFFL;
                  }
                }

                if ((int) CalcAuxModInvA[i - 1] < 0)
                {
                  carry1 -= Yaa;
                  carry2 -= Yba;
                }
                if ((int) CalcAuxModInvB[i - 1] < 0)
                {
                  carry1 -= Yab;
                  carry2 -= Ybb;
                }
                if ((int) CalcAuxModInvMu[i - 1] < 0)
                {
                  carry3 -= Yaa;
                  carry4 -= Yba;
                }
                if ((int) CalcAuxModInvGamma[i - 1] < 0)
                {
                  carry3 -= Yab;
                  carry4 -= Ybb;
                }
                CalcAuxModInvA[i - 1] = carry1 & 0xFFFFFFFFL;
                CalcAuxModInvB[i - 1] = carry2 & 0xFFFFFFFFL;
                CalcAuxModInvMu[i - 1] = carry3 & 0xFFFFFFFFL;
                CalcAuxModInvGamma[i - 1] = carry4 & 0xFFFFFFFFL;
                continue outer_loop;
              }
              Bl >>= 1;
              Dif++;
              E++;
              P *= 2;
              T1++;
            } /* end while */
            Iaa <<= T1;
            Iab <<= T1;
            if (Dif >= 0)
            {
              Dif = -Dif;
              if (((Al + Bl) & 3) == 0)
              {
                T1 = Iba;
                Iba += Iaa;
                Iaa = T1;
                T1 = Ibb;
                Ibb += Iab;
                Iab = T1;
                T1 = Bl;
                Bl += Al;
                Al = T1;
              } else {
                T1 = Iba;
                Iba -= Iaa;
                Iaa = T1;
                T1 = Ibb;
                Ibb -= Iab;
                Iab = T1;
                T1 = Bl;
                Bl -= Al;
                Al = T1;
              }
            } else {
              if (((Al + Bl) & 3) == 0)
              {
                Iba += Iaa;
                Ibb += Iab;
                Bl += Al;
              } else {
                Iba -= Iaa;
                Ibb -= Iab;
                Bl -= Al;
              }
            }
            Dif--;
          }
        }
        if (CalcAuxModInvA[0] != 1){
          SubtractBigNbr32(B, CalcAuxModInvMu, CalcAuxModInvMu, NumberLength);
        }
        if ((int) CalcAuxModInvMu[i = NumberLength - 1] < 0){
          AddBigNbr32(B, CalcAuxModInvMu, CalcAuxModInvMu, NumberLength);
        }
        for (; i >= 0; i--){
          if (B[i] != CalcAuxModInvMu[i])
            break;
        }
        if (i < 0 || B[i] < CalcAuxModInvMu[i]){ // If B < Mu
          SubtractBigNbr32(CalcAuxModInvMu, B, CalcAuxModInvMu, NumberLength); // Mu <- Mu - B
        }
        StaticFunctions.Convert32To31Bits(CalcAuxModInvMu, inv, NumberLength);
    }
}
