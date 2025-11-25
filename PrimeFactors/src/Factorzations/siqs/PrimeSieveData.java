
package Factorzations.siqs;

import static Factorzations.siqs.Relation.multiplier;
import static Factorzations.siqs.Relation.nbrPrimes1;
import static Factorzations.siqs.Relation.nbrPrimes2;

import static Factorzations.siqs.Siqs.SieveLimit;
import static Factorzations.siqs.Siqs.firstLimit;
import static Factorzations.siqs.Siqs.log2;
import static Factorzations.siqs.Siqs.secondLimit;
import static Factorzations.siqs.Siqs.smallPrimeUpperLimit;
import static Factorzations.siqs.Siqs.thirdLimit;
import static Factorzations.siqs.Siqs.threshold;

import static calculator.largemodel.Add.AddBigNbr;
import static calculator.largemodel.Subtract.SubtractBigNbr;

/**
 *
 * @author spy
 */
public class PrimeSieveData
{
    int value, modsqrt;
    int Bainv2[];
    int Bainv2_0, soln1, difsoln;

   public static void PerformSiqsSieveStage(PrimeSieveData primeSieveData[],
                             short SieveArray[], int biLinearCoeff[], int biLinearDelta[][],
                             int PolynomialIndex, int NumberLength)
  {
    short logPrimeEvenPoly, logPrimeOddPoly;
    int currentPrime, F1, F2, F3, F4, X1, X2;
    int index1, index2, indexFactorA;
    int mask;
    boolean polyadd;
    int S1, G0, G1, G2, G3;
    int H0, H1, H2, H3, I0, I1, I2, I3;
    PrimeSieveData rowPrimeSieveData;

    F1 = PolynomialIndex;
    indexFactorA = 0;
    while ((F1 & 1) == 0)
    {
      F1 >>= 1;
      indexFactorA++;
    }
    if (polyadd = ((F1 & 2) != 0))   // Adjust value of B as appropriate
    {                                // according to the Gray code.
      AddBigNbr(biLinearCoeff, biLinearDelta[indexFactorA], biLinearCoeff,
                    NumberLength);
      AddBigNbr(biLinearCoeff, biLinearDelta[indexFactorA], biLinearCoeff,
                    NumberLength);
    }
    else
    {
      SubtractBigNbr(biLinearCoeff, biLinearDelta[indexFactorA],
                         biLinearCoeff, NumberLength);
      SubtractBigNbr(biLinearCoeff, biLinearDelta[indexFactorA],
                         biLinearCoeff, NumberLength);
    }
    indexFactorA--;
    X1 = SieveLimit << 1;
    rowPrimeSieveData = primeSieveData[1];
    F1 = polyadd ? -rowPrimeSieveData.Bainv2[indexFactorA]:
                    rowPrimeSieveData.Bainv2[indexFactorA];
    if (((rowPrimeSieveData.soln1 += F1) & 1) == 0)
    {
      SieveArray[0] = (short) (log2 - threshold);
      SieveArray[1] = (short) (-threshold);
    } else {
      SieveArray[0] = (short) (-threshold);
      SieveArray[1] = (short) (log2 - threshold);
    }
    if (((rowPrimeSieveData.soln1 + rowPrimeSieveData.Bainv2_0) & 1) == 0)
    {
      SieveArray[0] += (short) ((log2 - threshold) << 8);
      SieveArray[1] += (short) ((-threshold) << 8);
    } else {
      SieveArray[0] += (short) ((-threshold) << 8);
      SieveArray[1] += (short) ((log2 - threshold) << 8);
    }
    F2 = 2;
    index1 = 2;
    while (true)
    {
      rowPrimeSieveData = primeSieveData[index1];
      currentPrime = rowPrimeSieveData.value;
      F3 = F2 * currentPrime;
      if (X1 + 1 < F3)
      {
        F3 = X1 + 1;
      }
      F4 = F2;
      while (F4 * 2 <= F3)
      {
        System.arraycopy(SieveArray, 0, SieveArray, F4, F4);
        F4 *= 2;
      }
      System.arraycopy(SieveArray, 0, SieveArray, F4, F3 - F4);
      if (F3 == X1 + 1)
        break;
      F1 = currentPrime;
      logPrimeEvenPoly = 1;
      while (F1 >= 5)
      {
        F1 /= 3;
        logPrimeEvenPoly++;
      }
      logPrimeOddPoly = (short)(logPrimeEvenPoly << 8);
      F1 = polyadd ? -rowPrimeSieveData.Bainv2[indexFactorA]:
                      rowPrimeSieveData.Bainv2[indexFactorA];
      index2 = (rowPrimeSieveData.soln1 + F1) % currentPrime;
      rowPrimeSieveData.soln1 = index2 += currentPrime & (index2 >> 31);
      for (; index2 < F3; index2 += currentPrime)
        SieveArray[index2] += logPrimeEvenPoly;
      for (index2 = (rowPrimeSieveData.soln1 + currentPrime -
                      rowPrimeSieveData.Bainv2_0) % currentPrime;
           index2 < F3;
           index2 += currentPrime)
        SieveArray[index2] += logPrimeOddPoly;
      if (currentPrime != multiplier)
      {
        for (F1 = index2 = (rowPrimeSieveData.soln1 + currentPrime -
                        rowPrimeSieveData.difsoln) % currentPrime;
             index2 < F3;
             index2 += currentPrime)
          SieveArray[index2] += logPrimeEvenPoly;
        for (index2 = (F1 + currentPrime -
                        rowPrimeSieveData.Bainv2_0) % currentPrime;
             index2 < F3;
             index2 += currentPrime)
          SieveArray[index2] += logPrimeOddPoly;
      }
      index1++;
      F2 *= currentPrime;
    }

    F1 = primeSieveData[smallPrimeUpperLimit].value;
    logPrimeEvenPoly = 1;
    logPrimeOddPoly = 0x100;
    mask = 5;
    while (F1 >= 5)
    {
      F1 /= 3;
      logPrimeEvenPoly++;
      logPrimeOddPoly += 0x100;
      mask *= 3;
    }
    if (polyadd)
    {
      for (; index1 < smallPrimeUpperLimit; index1++)
      {
        rowPrimeSieveData = primeSieveData[index1];
        currentPrime = rowPrimeSieveData.value;
        if ((S1 = rowPrimeSieveData.soln1 -
                  rowPrimeSieveData.Bainv2[indexFactorA]) < 0)
          S1 += currentPrime;
        rowPrimeSieveData.soln1 = S1;
      }

      for (index1 = smallPrimeUpperLimit; index1 < firstLimit; index1++)
      {
        rowPrimeSieveData = primeSieveData[index1];
        currentPrime = rowPrimeSieveData.value;
        if (currentPrime >= mask)
        {
          mask *= 3;
          logPrimeEvenPoly++;
          logPrimeOddPoly += 0x100;
        }
        F2 = currentPrime + currentPrime;
        F3 = F2 + currentPrime;
        F4 = F3 + currentPrime;
        S1 = rowPrimeSieveData.soln1 -
                  rowPrimeSieveData.Bainv2[indexFactorA];
        rowPrimeSieveData.soln1 = S1 += (S1 >> 31) & currentPrime;
        index2 = X1 / F4 * F4 + S1;
        G0 = -rowPrimeSieveData.difsoln;
        if (S1 + G0 < 0)
          G0 += currentPrime;
        G1 = G0 + currentPrime;
        G2 = G1 + currentPrime;
        G3 = G2 + currentPrime;
        H0 = -rowPrimeSieveData.Bainv2_0;
        if (S1 + H0 < 0)
          H0 += currentPrime;
        H1 = H0 + currentPrime;
        H2 = H1 + currentPrime;
        H3 = H2 + currentPrime;
        I0 = H0 - rowPrimeSieveData.difsoln;
        if (S1 + I0 < 0)
          I0 += currentPrime;
        I1 = I0 + currentPrime;
        I2 = I1 + currentPrime;
        I3 = I2 + currentPrime;
        do
        {
          SieveArray[index2] += logPrimeEvenPoly;
          SieveArray[index2 + currentPrime] += logPrimeEvenPoly;
          SieveArray[index2 + F2] += logPrimeEvenPoly;
          SieveArray[index2 + F3] += logPrimeEvenPoly;
          SieveArray[index2 + G0] += logPrimeEvenPoly;
          SieveArray[index2 + G1] += logPrimeEvenPoly;
          SieveArray[index2 + G2] += logPrimeEvenPoly;
          SieveArray[index2 + G3] += logPrimeEvenPoly;
          SieveArray[index2 + H0] += logPrimeOddPoly;
          SieveArray[index2 + H1] += logPrimeOddPoly;
          SieveArray[index2 + H2] += logPrimeOddPoly;
          SieveArray[index2 + H3] += logPrimeOddPoly;
          SieveArray[index2 + I0] += logPrimeOddPoly;
          SieveArray[index2 + I1] += logPrimeOddPoly;
          SieveArray[index2 + I2] += logPrimeOddPoly;
          SieveArray[index2 + I3] += logPrimeOddPoly;
        }
        while ((index2 -= F4) >= 0);
      }

      for (; index1 < secondLimit; index1++)
      {
        rowPrimeSieveData = primeSieveData[index1];
        currentPrime = rowPrimeSieveData.value;
        F2 = currentPrime + currentPrime;
        F3 = F2 + currentPrime;
        F4 = F2 + F2;
        X2 = X1 - F4;
        if (currentPrime >= mask)
        {
          mask *= 3;
          logPrimeEvenPoly++;
          logPrimeOddPoly += 0x100;
        }
        if (rowPrimeSieveData.difsoln >= 0)
        {
          F1 = rowPrimeSieveData.soln1 -
                    rowPrimeSieveData.Bainv2[indexFactorA];
          F1 += (F1 >> 31) & currentPrime;
          index2 = (rowPrimeSieveData.soln1 = F1);
          do
          {
            SieveArray[index2] += logPrimeEvenPoly;
            SieveArray[index2 + currentPrime] += logPrimeEvenPoly;
            SieveArray[index2 + F2] += logPrimeEvenPoly;
            SieveArray[index2 + F3] += logPrimeEvenPoly;
          } while ((index2 += F4) <= X2);
          for (; index2 <= X1; index2 += currentPrime)
            SieveArray[index2] += logPrimeEvenPoly;
          index2 = F1 - rowPrimeSieveData.Bainv2_0;
          index2 += (index2 >> 31) & currentPrime;
          do
          {
            SieveArray[index2] += logPrimeOddPoly;
            SieveArray[index2 + currentPrime] += logPrimeOddPoly;
            SieveArray[index2 + F2] += logPrimeOddPoly;
            SieveArray[index2 + F3] += logPrimeOddPoly;
          } while ((index2 += F4) <= X2);
          for (; index2 <= X1; index2 += currentPrime)
            SieveArray[index2] += logPrimeOddPoly;
          F1 -= rowPrimeSieveData.difsoln;
          F1 += (F1 >> 31) & currentPrime;
          index2 = F1;
          do
          {
            SieveArray[index2] += logPrimeEvenPoly;
            SieveArray[index2 + currentPrime] += logPrimeEvenPoly;
            SieveArray[index2 + F2] += logPrimeEvenPoly;
            SieveArray[index2 + F3] += logPrimeEvenPoly;
          } while ((index2 += F4) <= X2);
          for (; index2 <= X1; index2 += currentPrime)
            SieveArray[index2] += logPrimeEvenPoly;
          index2 = F1 - rowPrimeSieveData.Bainv2_0;
          index2 += (index2 >> 31) & currentPrime;
          do
          {
            SieveArray[index2] += logPrimeOddPoly;
            SieveArray[index2 + currentPrime] += logPrimeOddPoly;
            SieveArray[index2 + F2] += logPrimeOddPoly;
            SieveArray[index2 + F3] += logPrimeOddPoly;
          } while ((index2 += F4) <= X2);
          for (; index2 <= X1; index2 += currentPrime)
            SieveArray[index2] += logPrimeOddPoly;
        }
      }

      for (; index1 < thirdLimit; index1++)
      {
        rowPrimeSieveData = primeSieveData[index1];
        currentPrime = rowPrimeSieveData.value;
        if (currentPrime >= mask)
        {
          mask *= 3;
          logPrimeEvenPoly++;
          logPrimeOddPoly += 0x100;
        }
        F2 = rowPrimeSieveData.soln1 - rowPrimeSieveData.Bainv2[indexFactorA];
        F2 += currentPrime & (F2 >> 31);
        index2 = (rowPrimeSieveData.soln1 = F2);
        do
        {
          SieveArray[index2] += logPrimeEvenPoly;
        } while ((index2 += currentPrime) <= X1);
        F1 = F2 - (F3=rowPrimeSieveData.Bainv2_0);
        F1 += currentPrime & (F1 >> 31);
        do
        {
          SieveArray[F1] += logPrimeOddPoly;
        } while ((F1 += currentPrime) <= X1);
        F2 -= rowPrimeSieveData.difsoln;
        index2 = F2 += currentPrime & (F2 >> 31);
        do
        {
          SieveArray[index2] += logPrimeEvenPoly;
        } while ((index2 += currentPrime) <= X1);
        F2 += (currentPrime & ((F2-F3) >> 31))-F3;
        do
        {
          SieveArray[F2] += logPrimeOddPoly;
        } while ((F2 += currentPrime) <= X1);
      }

      for (; index1 < nbrPrimes2; index1++)
      {
        rowPrimeSieveData = primeSieveData[index1];
        currentPrime = rowPrimeSieveData.value;
        if (currentPrime >= mask)
        {
          mask *= 3;
          logPrimeEvenPoly++;
          logPrimeOddPoly += 0x100;
        }
        F2 = rowPrimeSieveData.soln1 - rowPrimeSieveData.Bainv2[indexFactorA];
        if ((rowPrimeSieveData.soln1=(F2 += currentPrime & (F2 >> 31))) < X1)
          SieveArray[F2] += logPrimeEvenPoly;
        F1 = F2 - (F3=rowPrimeSieveData.Bainv2_0);
        if ((F1 += currentPrime & (F1 >> 31)) < X1)
          SieveArray[F1] += logPrimeOddPoly;
        F2 -= rowPrimeSieveData.difsoln;
        if ((F2 += currentPrime & (F2 >> 31)) < X1)
          SieveArray[F2] += logPrimeEvenPoly;
        if ((F2 += (currentPrime & ((F2-F3) >> 31))-F3) < X1)
          SieveArray[F2] += logPrimeOddPoly;
        rowPrimeSieveData = primeSieveData[++index1];
        currentPrime = rowPrimeSieveData.value;
        F2 = rowPrimeSieveData.soln1 - rowPrimeSieveData.Bainv2[indexFactorA];
        if ((rowPrimeSieveData.soln1=(F2 += currentPrime & (F2 >> 31))) < X1)
          SieveArray[F2] += logPrimeEvenPoly;
        F1 = F2 - (F3=rowPrimeSieveData.Bainv2_0);
        if ((F1 += currentPrime & (F1 >> 31)) < X1)
          SieveArray[F1] += logPrimeOddPoly;
        F2 -= rowPrimeSieveData.difsoln;
        if ((F2 += currentPrime & (F2 >> 31)) < X1)
          SieveArray[F2] += logPrimeEvenPoly;
        if ((F2 += (currentPrime & ((F2-F3) >> 31))-F3) < X1)
          SieveArray[F2] += logPrimeOddPoly;
        rowPrimeSieveData = primeSieveData[++index1];
        currentPrime = rowPrimeSieveData.value;
        F2 = rowPrimeSieveData.soln1 - rowPrimeSieveData.Bainv2[indexFactorA];
        if ((rowPrimeSieveData.soln1=(F2 += currentPrime & (F2 >> 31))) < X1)
          SieveArray[F2] += logPrimeEvenPoly;
        F1 = F2 - (F3=rowPrimeSieveData.Bainv2_0);
        if ((F1 += currentPrime & (F1 >> 31)) < X1)
          SieveArray[F1] += logPrimeOddPoly;
        F2 -= rowPrimeSieveData.difsoln;
        if ((F2 += currentPrime & (F2 >> 31)) < X1)
          SieveArray[F2] += logPrimeEvenPoly;
        F2 -= F3;
        if ((F2 += currentPrime & (F2 >> 31)) < X1)
          SieveArray[F2] += logPrimeOddPoly;
        rowPrimeSieveData = primeSieveData[++index1];
        currentPrime = rowPrimeSieveData.value;
        F2 = rowPrimeSieveData.soln1 - rowPrimeSieveData.Bainv2[indexFactorA];
        if ((rowPrimeSieveData.soln1=(F2 += currentPrime & (F2 >> 31))) < X1)
          SieveArray[F2] += logPrimeEvenPoly;
        F1 = F2 - (F3=rowPrimeSieveData.Bainv2_0);
        if ((F1 += currentPrime & (F1 >> 31)) < X1)
          SieveArray[F1] += logPrimeOddPoly;
        F2 -= rowPrimeSieveData.difsoln;
        if ((F2 += currentPrime & (F2 >> 31)) < X1)
          SieveArray[F2] += logPrimeEvenPoly;
        F2 -= F3;
        if ((F2 += currentPrime & (F2 >> 31)) < X1)
          SieveArray[F2] += logPrimeOddPoly;
      }

      for (; index1 < nbrPrimes1; index1++)
      {
        rowPrimeSieveData = primeSieveData[index1];
        currentPrime = rowPrimeSieveData.value;
        if (currentPrime >= mask)
        {
          mask *= 3;
          logPrimeEvenPoly++;
          logPrimeOddPoly += 0x100;
        }
        F2 = rowPrimeSieveData.soln1 - rowPrimeSieveData.Bainv2[indexFactorA];
        if ((rowPrimeSieveData.soln1=(F2 += currentPrime & (F2 >> 31))) < X1)
          SieveArray[F2] += logPrimeEvenPoly;
        F1 = F2 - (F3=rowPrimeSieveData.Bainv2_0);
        if ((F1 += currentPrime & (F1 >> 31)) < X1)
          SieveArray[F1] += logPrimeOddPoly;
        F2 -= rowPrimeSieveData.difsoln;
        if ((F2 += currentPrime & (F2 >> 31)) < X1)
          SieveArray[F2] += logPrimeEvenPoly;
        F2 -= F3;
        if ((F2 += currentPrime & (F2 >> 31)) < X1)
          SieveArray[F2] += logPrimeOddPoly;
      }

    } else {
      for (; index1 < smallPrimeUpperLimit; index1++)
      {
        rowPrimeSieveData = primeSieveData[index1];
        currentPrime = rowPrimeSieveData.value;
        S1 = rowPrimeSieveData.soln1 +
                  rowPrimeSieveData.Bainv2[indexFactorA] - currentPrime;
        S1 += currentPrime & (S1 >> 31);
        rowPrimeSieveData.soln1 = S1;
      }

      for (index1 = smallPrimeUpperLimit; index1 < firstLimit; index1++)
      {
        rowPrimeSieveData = primeSieveData[index1];
        currentPrime = rowPrimeSieveData.value;
        if (currentPrime >= mask)
        {
          mask *= 3;
          logPrimeEvenPoly++;
          logPrimeOddPoly += 0x100;
        }
        F2 = currentPrime + currentPrime;
        F3 = F2 + currentPrime;
        F4 = F3 + currentPrime;
        S1 = rowPrimeSieveData.soln1 +
                  rowPrimeSieveData.Bainv2[indexFactorA] - currentPrime;
        rowPrimeSieveData.soln1 = S1 += (S1 >> 31) & currentPrime;
        index2 = X1 / F4 * F4 + S1;
        G0 = -rowPrimeSieveData.difsoln;
        if (S1 + G0 < 0)
          G0 += currentPrime;
        G1 = G0 + currentPrime;
        G2 = G1 + currentPrime;
        G3 = G2 + currentPrime;
        H0 = -rowPrimeSieveData.Bainv2_0;
        if (S1 + H0 < 0)
          H0 += currentPrime;
        H1 = H0 + currentPrime;
        H2 = H1 + currentPrime;
        H3 = H2 + currentPrime;
        I0 = H0 - rowPrimeSieveData.difsoln;
        if (S1 + I0 < 0)
          I0 += currentPrime;
        I1 = I0 + currentPrime;
        I2 = I1 + currentPrime;
        I3 = I2 + currentPrime;
        do
        {
          SieveArray[index2] += logPrimeEvenPoly;
          SieveArray[index2 + currentPrime] += logPrimeEvenPoly;
          SieveArray[index2 + F2] += logPrimeEvenPoly;
          SieveArray[index2 + F3] += logPrimeEvenPoly;
          SieveArray[index2 + G0] += logPrimeEvenPoly;
          SieveArray[index2 + G1] += logPrimeEvenPoly;
          SieveArray[index2 + G2] += logPrimeEvenPoly;
          SieveArray[index2 + G3] += logPrimeEvenPoly;
          SieveArray[index2 + H0] += logPrimeOddPoly;
          SieveArray[index2 + H1] += logPrimeOddPoly;
          SieveArray[index2 + H2] += logPrimeOddPoly;
          SieveArray[index2 + H3] += logPrimeOddPoly;
          SieveArray[index2 + I0] += logPrimeOddPoly;
          SieveArray[index2 + I1] += logPrimeOddPoly;
          SieveArray[index2 + I2] += logPrimeOddPoly;
          SieveArray[index2 + I3] += logPrimeOddPoly;
        }
        while ((index2 -= F4) >= 0);
      }

      for (; index1 < secondLimit; index1++)
      {
        rowPrimeSieveData = primeSieveData[index1];
        currentPrime = rowPrimeSieveData.value;
        F2 = currentPrime + currentPrime;
        F3 = F2 + currentPrime;
        F4 = F2 + F2;
        X2 = X1 - F4;
        if (currentPrime >= mask)
        {
          mask *= 3;
          logPrimeEvenPoly++;
          logPrimeOddPoly += 0x100;
        }
        if (rowPrimeSieveData.difsoln >= 0)
        {
          F1 = rowPrimeSieveData.soln1 +
                    rowPrimeSieveData.Bainv2[indexFactorA] - currentPrime;
          F1 += currentPrime & (F1 >> 31);
          index2 = (rowPrimeSieveData.soln1 = F1);
          do
          {
            SieveArray[index2] += logPrimeEvenPoly;
            SieveArray[index2 + currentPrime] += logPrimeEvenPoly;
            SieveArray[index2 + F2] += logPrimeEvenPoly;
            SieveArray[index2 + F3] += logPrimeEvenPoly;
          } while ((index2 += F4) <= X2);
          for (; index2 <= X1; index2 += currentPrime)
            SieveArray[index2] += logPrimeEvenPoly;
          index2 = F1 - rowPrimeSieveData.Bainv2_0;
          index2 += (index2 >> 31) & currentPrime;
          do
          {
            SieveArray[index2] += logPrimeOddPoly;
            SieveArray[index2 + currentPrime] += logPrimeOddPoly;
            SieveArray[index2 + F2] += logPrimeOddPoly;
            SieveArray[index2 + F3] += logPrimeOddPoly;
          } while ((index2 += F4) <= X2);
          for (; index2 <= X1; index2 += currentPrime)
            SieveArray[index2] += logPrimeOddPoly;
          F1 -= rowPrimeSieveData.difsoln;
          F1 += (F1 >> 31) & currentPrime;
          index2 = F1;
          do
          {
            SieveArray[index2] += logPrimeEvenPoly;
            SieveArray[index2 + currentPrime] += logPrimeEvenPoly;
            SieveArray[index2 + F2] += logPrimeEvenPoly;
            SieveArray[index2 + F3] += logPrimeEvenPoly;
          } while ((index2 += F4) <= X2);
          for (; index2 <= X1; index2 += currentPrime)
            SieveArray[index2] += logPrimeEvenPoly;
          index2 = F1 - rowPrimeSieveData.Bainv2_0;
          index2 += (index2 >> 31) & currentPrime;
          do
          {
            SieveArray[index2] += logPrimeOddPoly;
            SieveArray[index2 + currentPrime] += logPrimeOddPoly;
            SieveArray[index2 + F2] += logPrimeOddPoly;
            SieveArray[index2 + F3] += logPrimeOddPoly;
          } while ((index2 += F4) <= X2);
          for (; index2 <= X1; index2 += currentPrime)
            SieveArray[index2] += logPrimeOddPoly;
        }
      }

      for (; index1 < thirdLimit; index1++)
      {
        rowPrimeSieveData = primeSieveData[index1];
        currentPrime = rowPrimeSieveData.value;
        if (currentPrime >= mask)
        {
          mask *= 3;
          logPrimeEvenPoly++;
          logPrimeOddPoly += 0x100;
        }
        F2 = rowPrimeSieveData.soln1 +
                  rowPrimeSieveData.Bainv2[indexFactorA] - currentPrime;
        index2 = (rowPrimeSieveData.soln1 = (F2 += currentPrime & (F2 >> 31)));
        do
        {
          SieveArray[index2] += logPrimeEvenPoly;
        } while ((index2 += currentPrime) <= X1);
        F1 = F2 - (F3=rowPrimeSieveData.Bainv2_0);
        F1 += currentPrime & (F1 >> 31);
        do
        {
          SieveArray[F1] += logPrimeOddPoly;
        } while ((F1 += currentPrime) <= X1);
        F2 -= rowPrimeSieveData.difsoln;
        F1 = F2 += currentPrime & (F2 >> 31);
        do
        {
          SieveArray[F2] += logPrimeEvenPoly;
        } while ((F2 += currentPrime) <= X1);
        F1 -= F3;
        F1 += currentPrime & (F1 >> 31);
        do
        {
          SieveArray[F1] += logPrimeOddPoly;
        } while ((F1 += currentPrime) <= X1);
      }

      for (; index1 < nbrPrimes2; index1++)
      {
        rowPrimeSieveData = primeSieveData[index1];
        currentPrime = rowPrimeSieveData.value;
        if (currentPrime >= mask)
        {
          mask *= 3;
          logPrimeEvenPoly++;
          logPrimeOddPoly += 0x100;
        }
        F2 = rowPrimeSieveData.soln1 +
                  rowPrimeSieveData.Bainv2[indexFactorA] - currentPrime;
        if ((rowPrimeSieveData.soln1=(F2 += currentPrime & (F2 >> 31))) < X1)
          SieveArray[F2] += logPrimeEvenPoly;
        F1 = F2 - (F3=rowPrimeSieveData.Bainv2_0);
        if ((F1 += currentPrime & (F1 >> 31)) < X1)
          SieveArray[F1] += logPrimeOddPoly;
        F2 -= rowPrimeSieveData.difsoln;
        if ((F2 += currentPrime & (F2 >> 31)) < X1)
          SieveArray[F2] += logPrimeEvenPoly;
        F2 -= F3;
        if ((F2 += currentPrime & (F2 >> 31)) < X1)
          SieveArray[F2] += logPrimeOddPoly;
        rowPrimeSieveData = primeSieveData[++index1];
        currentPrime = rowPrimeSieveData.value;
        F2 = rowPrimeSieveData.soln1 +
                  rowPrimeSieveData.Bainv2[indexFactorA] - currentPrime;
        if ((rowPrimeSieveData.soln1=(F2 += currentPrime & (F2 >> 31))) < X1)
          SieveArray[F2] += logPrimeEvenPoly;
        F1 = F2 - (F3=rowPrimeSieveData.Bainv2_0);
        if ((F1 += currentPrime & (F1 >> 31)) < X1)
          SieveArray[F1] += logPrimeOddPoly;
        F2 -= rowPrimeSieveData.difsoln;
        if ((F2 += currentPrime & (F2 >> 31)) < X1)
          SieveArray[F2] += logPrimeEvenPoly;
        F2 -= F3;
        if ((F2 += currentPrime & (F2 >> 31)) < X1)
          SieveArray[F2] += logPrimeOddPoly;
        rowPrimeSieveData = primeSieveData[++index1];
        currentPrime = rowPrimeSieveData.value;
        F2 = rowPrimeSieveData.soln1 +
                  rowPrimeSieveData.Bainv2[indexFactorA] - currentPrime;
        if ((rowPrimeSieveData.soln1=(F2 += currentPrime & (F2 >> 31))) < X1)
          SieveArray[F2] += logPrimeEvenPoly;
        F1 = F2 - (F3=rowPrimeSieveData.Bainv2_0);
        if ((F1 += currentPrime & (F1 >> 31)) < X1)
          SieveArray[F1] += logPrimeOddPoly;
        F2 -= rowPrimeSieveData.difsoln;
        if ((F2 += currentPrime & (F2 >> 31)) < X1)
          SieveArray[F2] += logPrimeEvenPoly;
        F2 -= F3;
        if ((F2 += currentPrime & (F2 >> 31)) < X1)
          SieveArray[F2] += logPrimeOddPoly;
        rowPrimeSieveData = primeSieveData[++index1];
        currentPrime = rowPrimeSieveData.value;
        F2 = rowPrimeSieveData.soln1 +
                  rowPrimeSieveData.Bainv2[indexFactorA] - currentPrime;
        if ((rowPrimeSieveData.soln1=(F2 += currentPrime & (F2 >> 31))) < X1)
          SieveArray[F2] += logPrimeEvenPoly;
        F1 = F2 - (F3=rowPrimeSieveData.Bainv2_0);
        if ((F1 += currentPrime & (F1 >> 31)) < X1)
          SieveArray[F1] += logPrimeOddPoly;
        F2 -= rowPrimeSieveData.difsoln;
        if ((F2 += currentPrime & (F2 >> 31)) < X1)
          SieveArray[F2] += logPrimeEvenPoly;
        F2 -= F3;
        if ((F2 += currentPrime & (F2 >> 31)) < X1)
          SieveArray[F2] += logPrimeOddPoly;
      }

      for (; index1 < nbrPrimes1; index1++)
      {
        rowPrimeSieveData = primeSieveData[index1];
        currentPrime = rowPrimeSieveData.value;
        if (currentPrime >= mask)
        {
          mask *= 3;
          logPrimeEvenPoly++;
          logPrimeOddPoly += 0x100;
        }
        F2 = rowPrimeSieveData.soln1 +
                  rowPrimeSieveData.Bainv2[indexFactorA] - currentPrime;
        if ((rowPrimeSieveData.soln1=(F2 += currentPrime & (F2 >> 31))) < X1)
          SieveArray[F2] += logPrimeEvenPoly;
        F1 = F2 - (F3=rowPrimeSieveData.Bainv2_0);
        if ((F1 += currentPrime & (F1 >> 31)) < X1)
          SieveArray[F1] += logPrimeOddPoly;
        F2 -= rowPrimeSieveData.difsoln;
        if ((F2 += currentPrime & (F2 >> 31)) < X1)
          SieveArray[F2] += logPrimeEvenPoly;
        F2 -= F3;
        if ((F2 += currentPrime & (F2 >> 31)) < X1)
          SieveArray[F2] += logPrimeOddPoly;
      }
    }
  }
}
