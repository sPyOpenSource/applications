
package Factorzations.siqs;

import static Factorzations.siqs.Relation.matrixB;
import static Factorzations.siqs.Relation.congruencesFound;
import static Factorzations.siqs.Relation.nbrPrimes1;
import static Factorzations.siqs.Relation.CalculateX;

import static Factorzations.siqs.StaticFunctions.modInv;
import static Factorzations.siqs.Maths.primeTrialDivisionData;

import static Factorzations.siqs.Siqs.NbrPolynomials;
import static Factorzations.siqs.Siqs.nbrFactorsA;
import static Factorzations.siqs.Siqs.nbrThreadFinishedPolySet;

import static Factorzations.siqs.Siqs.SieveLimit;
import static Factorzations.siqs.Siqs.ValuesSieved;
import static Factorzations.siqs.Siqs.polynomialsSieved;
import static Factorzations.siqs.Siqs.trialDivisions;
import static Factorzations.siqs.Siqs.largePrimeUpperBound;

import static Factorzations.siqs.Siqs.TestNbr1;
import static Factorzations.siqs.Siqs.factorSiqs;

import static calculator.largemodel.Add.AddBigNbr;
import static calculator.largemodel.ChangeSigns.ChSignBigNbr;
import static calculator.largemodel.Divide.DivBigNbrByLong;
import static calculator.largemodel.Multiply.MultBigNbr;
import static calculator.largemodel.Subtract.SubtractBigNbr;

/**
 *
 * @author xuyi
 */
public class Sieve extends FirstPolynomial {
    private final int threadNumber;
    
    Sieve(int threadNumber) {
        this.threadNumber = threadNumber;
    }
    
    private void SieveLocationHit(
                           int rowMatrixB[], int rowMatrixBbeforeMerge[],
                           int index2,
                           PrimeSieveData primeSieveData[],
                           int rowPartials[],
                           int rowSquares[], int biDividend[],
                           int NumberLength, int biT[],
                           int biR[], int biU[], int biV[],
                           int indexFactorsA[], boolean oddPolynomial)
  {
    trialDivisions++;

    CalculateX(biQuadrCoeff, biLinearCoeff, biLinearDelta, index2, oddPolynomial,
                             biT, NumberLength);

    MultBigNbr(biT, biT, biDividend, NumberLength);       // (Ax+B)^2
                                                          // To factor: (Ax+B)^2-N
    SubtractBigNbr(biDividend, TestNbr1, biDividend, NumberLength);
    
    /* factor biDividend */

    int NumberLengthDivid = NumberLength; /* Number length for dividend */
    boolean positive = true;
    if ((biDividend[NumberLengthDivid - 1] & 0x40000000) != 0)
    { /* Negative */
      positive = false;
      ChSignBigNbr(biDividend, NumberLengthDivid); // Convert to positive
    }
    rowSquares[0] = 1;
    for (int index = 0; index < nbrFactorsA; index++)
    {
      DivBigNbrByLong(biDividend, afact[index], biDividend,
                                NumberLengthDivid);
      if ((biDividend[NumberLengthDivid - 1] == 0
              && biDividend[NumberLengthDivid - 2] < 0x40000000))
        NumberLengthDivid--;
    }
    int nbrColumns = 1;
    if (!positive)
    {                                  // Insert -1 as a factor.
      rowMatrixBbeforeMerge[nbrColumns++] = 0;
    }
    rowMatrixBbeforeMerge[0] = nbrColumns;
    long Divid = PrimeTrialDivisionData.PerformTrialDivision(primeSieveData, rowMatrixBbeforeMerge,
                                 index2, aindex, biDividend,
                                 rowSquares, NumberLengthDivid,
                                 oddPolynomial);
    if (Divid == 1)
    { // Smooth relation found.
      Siqs.SmoothRelationFound(positive, rowMatrixB,
                                   rowMatrixBbeforeMerge,
                                   index2, aindex, biQuadrCoeff, rowSquares, biLinearCoeff, biLinearDelta,
                                   NumberLength, biT, biU, biR,
                                   oddPolynomial);
    } else {
      if (Divid > 0 && Divid < largePrimeUpperBound)
      {
        Siqs.PartialRelationFound(positive, rowMatrixB,
                                 rowMatrixBbeforeMerge,
                                 index2, aindex, Divid, seed, rowPartials, biQuadrCoeff,
                                 rowSquares, biLinearCoeff, biLinearDelta,
                                 NumberLength, biT, biR, biU, biV,
                                 indexFactorsA, oddPolynomial);
      }
    }
  }

    /****************/
  /* Sieve thread */
  /****************/
  @Override
  public void run()
  {
    int biT[] = new int[20], biU[] = new int[20], biV[] = new int[20], biR[] = new int[20];
    PrimeSieveData primeSieveData[];
    short [] SieveArray = new short[2 * SieveLimit + 5000];
    int rowPartials[] = new int[200];
    int biDividend[] = new int[20];
    int biAbsLinearCoeff[];
    int indexFactorsA[] = new int[50];
    int rowSquares[] = new int[200];
    int firstPolynomial = 0;
    int lastPolynomial = (NbrPolynomials - 1) & 0xFFFFFFFE;
    firstPolynomial |= 1;
    int grayCode = firstPolynomial ^ (firstPolynomial >> 1);
    firstPolynomial++;
    int i, PolynomialIndex;
    int rowMatrixBbeforeMerge[] = new int[200];
    int rowMatrixB[] = new int[200];
    boolean positive;
    int NumberLengthB;
    int NumberLength = Siqs.NumberLength;
        primeSieveData = new PrimeSieveData[nbrPrimes1 + 3];
        for (i = 0; i < nbrPrimes1; i++)
        {
          primeSieveData[i] = rowPrimeSieveData = new PrimeSieveData();
          PrimeSieveData rowPrimeSieveData0 = Siqs.primeSieveData[i];
          rowPrimeSieveData.value = rowPrimeSieveData0.value;
          rowPrimeSieveData.modsqrt = rowPrimeSieveData0.modsqrt;
          rowPrimeSieveData.Bainv2 = rowPrimeSieveData0.Bainv2;
        }
        Siqs.threads[threadNumber] = Thread.currentThread();

    try
    {
nextpoly:
      for (int polySet = 1;;polySet++)
      { // For each polynomial set...
          nbrThreadFinishedPolySet++;

          if (congruencesFound.get() >= matrixB.length)
          {
              synchronized(matrixB)
              {
                matrixB.notify();
              }
            return;
          }
          
            init(NumberLength, biDividend, primeSieveData);

        if (factorSiqs != null || congruencesFound.get() >= matrixB.length)
        {
            nbrThreadFinishedPolySet++;
            return;
        }

        PolynomialIndex = firstPolynomial;
                      // Compute first polynomial parameters.
        for (i = 0; i < NumberLength; i++)
        {
          biLinearCoeff[i] = biLinearDelta[0][i];
        }
        for (i = 1; i < nbrFactorsA; i++)
        {
          if ((grayCode & (1 << i)) == 0)
          {
            AddBigNbr(biLinearCoeff, biLinearDelta[i], biLinearCoeff,
                          NumberLength);
          } else {
            SubtractBigNbr(biLinearCoeff, biLinearDelta[i], biLinearCoeff,
                               NumberLength);
          }
        }

        for (NumberLengthA = NumberLength; NumberLengthA >= 2; NumberLengthA--)
        {
          if (biQuadrCoeff[NumberLengthA - 1] != 0 ||
              biQuadrCoeff[NumberLengthA - 2] >= 0x40000000)
          {                 // Go out if significant limb.
            break;
          }
        }

        if ((biLinearCoeff[NumberLength - 1] & 0x40000000) != 0)
        {                                        // Number is negative.
          positive = false;
          System.arraycopy(biLinearCoeff, 0,     // Source
                           biT, 0,               // Destination
                           NumberLength);        // Number of elements.
          ChSignBigNbr(biT, NumberLength);       // Make it positive.
          biAbsLinearCoeff = biT;
        } else {
          positive = true;                       // B is positive.
          biAbsLinearCoeff = biLinearCoeff;      // Get B mod current prime.
        }
        for (NumberLengthB = NumberLength; NumberLengthB >= 2; NumberLengthB--)
        {
          if (biAbsLinearCoeff[NumberLengthB - 1] != 0 ||
              biAbsLinearCoeff[NumberLengthB - 2] >= 0x40000000)
          {                 // Go out if significant limb.
            break;
          }
        }
 
        for (i = nbrPrimes1 - 1; i > 0; i--)
        { 
          rowPrimeSieveData = primeSieveData[i];
          rowPrimeTrialDivisionData = primeTrialDivisionData[i];
          currentPrime = rowPrimeTrialDivisionData.value;   // Get current prime.
          Dividend = biQuadrCoeff;                          // Get A mod current prime.
          Rem = rowPrimeTrialDivisionData.getRem(NumberLengthA, Dividend);
          
                                                            // Get its inverse
          inverseA = modInv((int)(Rem % currentPrime), currentPrime);
          
          Rem = rowPrimeTrialDivisionData.getRem(NumberLengthB, biAbsLinearCoeff);
          long RemB = Rem % currentPrime;
          if (positive)
            RemB = currentPrime - RemB;
          rowPrimeSieveData.soln1 = (int)((SieveLimit + (long)inverseA *
                  (rowPrimeSieveData.modsqrt + RemB)) % currentPrime);
        }

        do
        {                       // For each polynomial...
          if (congruencesFound.get() >= matrixB.length || factorSiqs != null)
          {
              nbrThreadFinishedPolySet++;
            return;             // Another thread finished factoring.
          }
          
          polynomialsSieved += 2;
          
          /***************/
          /* Sieve stage */
          /***************/
          PrimeSieveData.PerformSiqsSieveStage(primeSieveData, SieveArray, biLinearCoeff, biLinearDelta,
                                PolynomialIndex, NumberLength);
          ValuesSieved += 2 * SieveLimit;

          /************************/
          /* Trial division stage */
          /************************/
          int index = 2 * SieveLimit - 1;
          do
          {
            if (((SieveArray[index--] | SieveArray[index--] |
                  SieveArray[index--] | SieveArray[index--] |
                  SieveArray[index--] | SieveArray[index--] |
                  SieveArray[index--] | SieveArray[index--] |
                  SieveArray[index--] | SieveArray[index--] |
                  SieveArray[index--] | SieveArray[index--] |
                  SieveArray[index--] | SieveArray[index--] |
                  SieveArray[index--] | SieveArray[index--]) & 0x8080) != 0)
            {
              for (i = 16; i > 0; i--)
              {
                if ((SieveArray[index + i] & 0x80) != 0)
                {
                  if (congruencesFound.get() >= matrixB.length)
                  {       // All congruences were found: stop sieving.
                    index = 0;
                    break;
                  }
                  SieveLocationHit(rowMatrixB,
                                   rowMatrixBbeforeMerge,
                                   index + i, primeSieveData,
                                   rowPartials,
                                   rowSquares,
                                   biDividend, NumberLength, biT,
                                   biR, biU, biV,
                                   indexFactorsA, false);
                  if (congruencesFound.get() >= matrixB.length)
                  {      // All congruences were found: stop sieving.
                    index = 0;
                    break;
                  }
                }
                if (SieveArray[index + i] < 0)
                {
                  if (congruencesFound.get() >= matrixB.length)
                  {       // All congruences were found: stop sieving.
                    index = 0;
                    break;
                  }
                  SieveLocationHit(rowMatrixB,
                                   rowMatrixBbeforeMerge,
                                   index + i, primeSieveData,
                                   rowPartials,
                                   rowSquares,
                                   biDividend, NumberLength, biT,
                                   biR, biU, biV,
                                   indexFactorsA, true);
                  if (congruencesFound.get() >= matrixB.length)
                  {       // All congruences were found: stop sieving.
                    index = 0;
                    break;
                  }
                }
              }
            }
          }
          while (index > 0);

          /*******************/
          /* Next polynomial */
          /*******************/
          PolynomialIndex += 2;
        }
        while (PolynomialIndex <= lastPolynomial &&
               congruencesFound.get() < matrixB.length);
      }
    }
    catch (ArithmeticException ae)
    {
      synchronized(matrixB)
      {
        factorSiqs = null;
        matrixB.notify();
      }
    }
  }
}
