
package Factorzations.siqs;

import static Factorzations.ecm.StaticFunctions.LongToBigNbr;
import static Factorzations.siqs.Relation.nbrPrimes1;

import static Factorzations.siqs.Siqs.nbrFactorsA;
import static Factorzations.siqs.Siqs.newSeed;
import static Factorzations.siqs.Siqs.TestNbr1;

import static Factorzations.siqs.StaticFunctions.modInv;
import static calculator.largemodel.Divide.DivBigNbrByLong;
import static calculator.largemodel.Multiply.MultBigNbrByLong;
import static calculator.largemodel.Remainder.RemDivBigNbrByLong;
import static Factorzations.siqs.Siqs.primeTrialDivisionData;

/**
 *
 * @author xuyi
 */
public abstract class FirstPolynomial implements Runnable {
    int currentPrime, NumberLengthA, inverseA, twiceInverseA;
    long Rem, Q, remE, D, seed;
    PrimeTrialDivisionData rowPrimeTrialDivisionData;
    PrimeSieveData rowPrimeSieveData;
    int[] Dividend;
    final int aindex[] = new int[nbrFactorsA], tmodqq[] = new int[nbrFactorsA], 
            afact[] = new int[nbrFactorsA], amodq[] = new int[nbrFactorsA];
    final int biQuadrCoeff[] = new int[20], biLinearCoeff[] = new int[20];
    final int biLinearDelta[][] = new int[20][20];

    /******************************************/
    /* Initialization of the first polynomial */
    /******************************************/
    void init(int NumberLength, int biDividend[], PrimeSieveData primeSieveData[]){
        seed = newSeed;
        newSeed = Siqs.getFactorsOfA(seed, aindex);
        for (int index = 0; index < nbrFactorsA; index++)
        { // Get the values of the factors of A.
          afact[index] = primeSieveData[aindex[index]].value;
        }
        // Compute the leading coefficient in biQuadrCoeff.
        LongToBigNbr(afact[0], biQuadrCoeff, NumberLength);
        for (int index = 1; index < nbrFactorsA; index++)
        {
          MultBigNbrByLong(biQuadrCoeff, afact[index], biQuadrCoeff,
                               NumberLength);
        }
        for (NumberLengthA = NumberLength; NumberLengthA >= 2; NumberLengthA--)
        {
          if (biQuadrCoeff[NumberLengthA - 1] != 0 ||
              biQuadrCoeff[NumberLengthA - 2] >= 0x40000000)
            break;
        }

        for (int index1 = 0; index1 < nbrFactorsA; index1++)
        {
          currentPrime = (int)afact[index1];
          D = RemDivBigNbrByLong(biQuadrCoeff,
                   currentPrime * currentPrime, NumberLengthA) / currentPrime;
          Q = (long)primeSieveData[aindex[index1]].modsqrt *
              modInv((int)D, currentPrime) % currentPrime;
          amodq[index1] = (int)D << 1;
          tmodqq[index1] = (int)RemDivBigNbrByLong(TestNbr1,
                   currentPrime * currentPrime, NumberLength);
          if (Q + Q > currentPrime)
            Q = currentPrime - Q;
          DivBigNbrByLong(biQuadrCoeff, currentPrime, biDividend,
                              NumberLengthA);
          MultBigNbrByLong(biDividend, Q, biLinearDelta[index1],
                               NumberLengthA);
          for (int index2 = NumberLengthA; index2 < NumberLength; index2++)
            biLinearDelta[index1][index2] = 0;
        }
        for (int index1 = 1; index1 < nbrPrimes1; index1++)
        {
          rowPrimeTrialDivisionData = primeTrialDivisionData[index1];
          rowPrimeSieveData = primeSieveData[index1];
                                            // Get current prime.
          currentPrime = rowPrimeTrialDivisionData.value; 
          Dividend = biQuadrCoeff;          // Get A mod current prime.
          Rem = rowPrimeTrialDivisionData.getRem(NumberLengthA, Dividend);

                                            // Get its inverse
          inverseA = modInv((int)(Rem % currentPrime), currentPrime);
          twiceInverseA = inverseA << 1;    // and twice this value.

          rowPrimeSieveData.difsoln = (int) ((long)twiceInverseA *
                   rowPrimeSieveData.modsqrt % currentPrime);
          switch (NumberLengthA)
          {
            case 7:
              for (int index2 = nbrFactorsA - 1; index2 > 0; index2--)
              {
                Dividend = biLinearDelta[index2];
                remE = rowPrimeTrialDivisionData.getRem(7, Dividend) %
                        currentPrime;
                rowPrimeSieveData.Bainv2[index2 - 1] =
                        (int) (remE * twiceInverseA % currentPrime);
              }
              Dividend = biLinearDelta[0];
              remE = rowPrimeTrialDivisionData.getRem(7, Dividend) %
                      currentPrime;
              break;
            case 6 :
              for (int index2 = nbrFactorsA - 1; index2 > 0; index2--)
              {
                Dividend = biLinearDelta[index2];
                remE = rowPrimeTrialDivisionData.getRem(6, Dividend) %
                        currentPrime;
                rowPrimeSieveData.Bainv2[index2 - 1] =
                        (int) (remE * twiceInverseA % currentPrime);
              }
              Dividend = biLinearDelta[0];
              remE = rowPrimeTrialDivisionData.getRem(6, Dividend) %
                      currentPrime;
              break;
            case 5 :
              for (int index2 = nbrFactorsA - 1; index2 > 0; index2--)
              {
                Dividend = biLinearDelta[index2];
                remE = rowPrimeTrialDivisionData.getRem(5, Dividend) %
                        currentPrime;
                rowPrimeSieveData.Bainv2[index2 - 1] =
                        (int) (remE * twiceInverseA % currentPrime);
              }
              Dividend = biLinearDelta[0];
              remE = rowPrimeTrialDivisionData.getRem(5, Dividend) %
                      currentPrime;
              break;
            case 4 :
              for (int index2 = nbrFactorsA - 1; index2 > 0; index2--)
              {
                Dividend = biLinearDelta[index2];
                remE = rowPrimeTrialDivisionData.getRem(4, Dividend) %
                        currentPrime;
                rowPrimeSieveData.Bainv2[index2 - 1] =
                        (int) (remE * twiceInverseA % currentPrime);
              }
              Dividend = biLinearDelta[0];
              remE = rowPrimeTrialDivisionData.getRem(4, Dividend) %
                      currentPrime;
              break;
            case 3 :
              for (int index2 = nbrFactorsA - 1; index2 > 0; index2--)
              {
                Dividend = biLinearDelta[index2];
                remE = rowPrimeTrialDivisionData.getRem(3, Dividend) %
                        currentPrime;
                rowPrimeSieveData.Bainv2[index2 - 1] =
                        (int) (remE * twiceInverseA % currentPrime);
              }
              Dividend = biLinearDelta[0];
              remE = rowPrimeTrialDivisionData.getRem(3, Dividend) %
                      currentPrime;
              break;
            default:
                for (int index2 = nbrFactorsA - 1; index2 > 0; index2--)
                {
                    Dividend = biLinearDelta[index2];
                    remE = rowPrimeTrialDivisionData.getRem(2, Dividend) % currentPrime;
                    rowPrimeSieveData.Bainv2[index2 - 1] =
                            (int) (remE * twiceInverseA % currentPrime);
                }
                Dividend = biLinearDelta[0];
                remE = rowPrimeTrialDivisionData.getRem(2, Dividend) % currentPrime;
                break;
          }
          rowPrimeSieveData.Bainv2_0 =
                      (int) (remE * twiceInverseA % currentPrime);
          if (rowPrimeSieveData.Bainv2_0 != 0)
          {
            rowPrimeSieveData.Bainv2_0 =
                           currentPrime - rowPrimeSieveData.Bainv2_0;
          }
        }
        for (int index = 0; index < nbrFactorsA; index++)
          primeSieveData[aindex[index]].difsoln = -1; // Do not sieve.
    } // End initializing first polynomial
}
