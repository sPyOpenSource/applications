
package Factorzations.siqs;

import static Factorzations.ecm.StaticFunctions.LongToBigNbr;
import static Factorzations.ecm.StaticFunctions.MultBigNbrByLongModN;

import static BigIntegers.StaticFunctions.AddBigNbrModN;
import static BigIntegers.StaticFunctions.MultBigNbrModN;

import static calculator.largemodel.Add.AddBigNbr;
import static calculator.largemodel.ChangeSigns.ChSignBigNbr;
import static calculator.largemodel.Divide.DivBigNbrByLong;
import static calculator.largemodel.Multiply.MultBigNbr;
import static calculator.largemodel.Multiply.MultBigNbrByLong;
import static calculator.largemodel.Subtract.SubtractBigNbr;

import static Factorzations.siqs.Siqs.SieveLimit;
import static Factorzations.siqs.Siqs.TestNbr2;
import static Factorzations.siqs.Siqs.TestNbr1;
import static Factorzations.siqs.Siqs.getFactorsOfA;
import static Factorzations.siqs.Siqs.indexMinFactorA;
import static Factorzations.siqs.Siqs.matrixPartial;
import static Factorzations.siqs.Siqs.matrixPartialHashIndex;
import static Factorzations.siqs.Siqs.nbrFactorsA;
import static Factorzations.siqs.Siqs.vectLeftHandSide;

import com.gazman.factor.VectorData;
import ecm.BigIntegers.ModInv;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JProgressBar;

/**
 * congruence relation: x^2 = y^2 (mod n), x != +-y (mod n)
 * @author xuyi
 */
public class Relation extends Matrix {
    static JProgressBar progressBar;

    public static Map<Integer,Integer> matrixPartialHashIndex = new HashMap<>();
    public static ArrayList<VectorData> bSmoothVectors = new ArrayList<>();
    
    public static int vectLeftHandSide[][], matrixPartial[][], matrixB[][];
    public static int multiplier;
    public static AtomicInteger congruencesFound;
    static int nbrPartials;
    static long smoothsFound, totalPartials, partialsFound;
    public static int nbrPrimes1, nbrPrimes2;
    
    static void SmoothRelationFound(
                           boolean positive,
                           int rowMatrixB[], int rowMatrixBbeforeMerge[],
                           int index2, int aindex[], int biQuadrCoeff[],
                           int rowSquares[], int biLinearCoeff[], int biLinearDelta[][],
                           int NumberLength, int biT[], int biU[],
                           int biR[], boolean oddPolynomial)
  {
    if (congruencesFound.get() == matrixB.length)
      return; // All congruences already found.

    // Add all elements of aindex array to the rowMatrixB array discarding
    // duplicates.
    mergeArrays(aindex, nbrFactorsA, rowMatrixB, rowMatrixBbeforeMerge,
                rowSquares);

    LongToBigNbr(1, biR, NumberLength);
    LongToBigNbr(positive? 1: -1, biT, NumberLength);

    CalculateX(biQuadrCoeff, biLinearCoeff, biLinearDelta, index2, oddPolynomial,
                             biU, NumberLength);

    if (InsertNewRelation(rowMatrixB, rowSquares, biT,
                          biU, biR, NumberLength))
    {
        smoothsFound++;
        //System.out.println("smoo:" + smoothsFound);
    }
  }
    
    /* prime(s) outside the factor base */
    static void PartialRelationFound(
                           boolean positive,
                           int rowMatrixB[], int rowMatrixBbeforeMerge[],
                           int index2, int aindex[],
                           long Divid, long oldSeed, int rowPartials[], int biQuadrCoeff[],
                           int rowSquares[], int biLinearCoeff[], int biLinearDelta[][],
                           int NumberLength, int biT[],
                           int biR[], int biU[], int biV[],
                           int indexFactorsA[], boolean oddPolynomial)
  {
    int index, expParity;
    long D, Divisor;
    int nbrFactorsPartial, prev;
    int hashIndex;
    int rowPartial[];
    int newDivid = (int)Divid;    // This number is greater than zero.
    int indexFactorA = 0;
    int oldDivid, NumberLengthDivid;
    int squareRootSize = NumberLength / 2 + 1;
    int nbrColumns;
    PrimeTrialDivisionData rowPrimeTrialDivisionData;

    if (congruencesFound.get() == matrixB.length)
      return;
    // Partial relation found.
    totalPartials++;

    // Check if there is already another relation with the same
    // factor outside the prime base.
    // Calculate hash index
    if(matrixPartialHashIndex.containsKey(newDivid))
        hashIndex = matrixPartialHashIndex.get(newDivid);
    else hashIndex = -1;
    prev = -1;
    while (hashIndex >= 0)
    {
      rowPartial = matrixPartial[hashIndex];
      oldDivid = rowPartial[0];
      if (newDivid == oldDivid || newDivid == -oldDivid)
      {   // Match of partials.

        for (index = 0; index < squareRootSize; index++)
        {
          biV[index] = rowPartial[index + 2];
        }                           // biV = Old positive square root (Ax+B).
        for (; index < NumberLength; index++)
          biV[index] = 0;

        long seed = rowPartial[squareRootSize + 2];
        getFactorsOfA(seed, indexFactorsA);
        LongToBigNbr(newDivid, biR, NumberLength);
        nbrFactorsPartial = 0;

                                                // biT = old (Ax+B)^2.
        MultBigNbr(biV, biV, biT, NumberLength);
                                                // biT = old (Ax+B)^2 - N.
        SubtractBigNbr(biT, TestNbr1, biT, NumberLength);
        
        
        if (oldDivid < 0)
          rowPartials[nbrFactorsPartial++] = 0; // Insert -1 as a factor.
        if ((biT[NumberLength - 1] & 0x40000000) != 0)
          ChSignBigNbr(biT, NumberLength);      // Make it positive.

        NumberLengthDivid = NumberLength;
           // The number is multiple of the big prime, so divide by it.
        DivBigNbrByLong(biT, newDivid, biT, NumberLengthDivid);
        if (biT[NumberLengthDivid - 1] == 0 && biT[NumberLengthDivid - 2] < 0x40000000)
          NumberLengthDivid--;
        for (index = 0; index < nbrFactorsA; index++)
        {
          DivBigNbrByLong(biT,
                 primeTrialDivisionData[indexFactorsA[index]].value, biT,
                 NumberLengthDivid);
          if (biT[NumberLengthDivid - 1] == 0 && biT[NumberLengthDivid - 2] < 0x40000000)
            NumberLengthDivid--;
        }

        for (index = 1; index < nbrPrimes1; index++)
        {
          expParity = 0;
          if (index >= indexMinFactorA && indexFactorA < nbrFactorsA)
          {
            if (index == indexFactorsA[indexFactorA])
            {
              expParity = 1;
              indexFactorA++;
            }
          }
          rowPrimeTrialDivisionData = primeTrialDivisionData[index];
          Divisor = rowPrimeTrialDivisionData.value;
          while (rowPrimeTrialDivisionData.getRem(NumberLengthDivid, biT) % Divisor == 0)
          {
            DivBigNbrByLong(biT, Divisor, biT, NumberLengthDivid);
            expParity = 1 - expParity;
            if (expParity == 0)
              rowSquares[rowSquares[0]++] = (int)Divisor;
            if (NumberLengthDivid <= 2)
            {
              long DividLSDW = ((long)biT[1] << 31) + biT[0];
              if (DividLSDW == 1)
              {               // Division has ended.
                break;
              }
            }
            else if (biT[NumberLengthDivid - 1] == 0 && biT[NumberLengthDivid - 2] < 0x40000000)
            {
              NumberLengthDivid--;
            }
          }
          if (expParity != 0)
            rowPartials[nbrFactorsPartial++] = index;
        }
   
        CalculateX(biQuadrCoeff, biLinearCoeff, biLinearDelta, index2, oddPolynomial,
                             biT, NumberLength);

        // biU = Product of old Ax+B times new Ax+B
        MultBigNbrModN(biV, biT, biU, TestNbr1, NumberLength);

        // Add all elements of aindex array to the rowMatrixB array discarding
        // duplicates.
        mergeArrays(aindex, nbrFactorsA, rowMatrixB, rowMatrixBbeforeMerge,
                    rowSquares);
        rowMatrixBbeforeMerge[0] = nbrColumns = rowMatrixB[0];
        System.arraycopy(rowMatrixB, 1,                      // Source
                         rowMatrixBbeforeMerge, 1,           // Destination
                         nbrColumns);                        // Length
        mergeArrays(rowPartials, nbrFactorsPartial,
                    rowMatrixB, rowMatrixBbeforeMerge,
                    rowSquares);

        if (rowMatrixB[0] > 1 &&
            InsertNewRelation(rowMatrixB, rowSquares, biT,
                                biU, biR, NumberLength))
        {
          partialsFound++;
        }
        return;
      }
      else
      {
        prev = hashIndex;
        hashIndex = rowPartial[1]; // Get next index for same hash.
      }
    } /* end while */

      if (hashIndex == -1 && nbrPartials < matrixPartial.length)
      { // No match and partials table is not full.
        // Add partial to table of partials.
        if (prev >= 0)
        {
          matrixPartial[prev][1] = nbrPartials;
        } else {
          matrixPartialHashIndex.put(newDivid, nbrPartials);
        }
        rowPartial = matrixPartial[nbrPartials];

        // Add all elements of aindex array to the rowMatrixB array discarding
        // duplicates.
        mergeArrays(aindex, nbrFactorsA, rowMatrixB, rowMatrixBbeforeMerge,
                    rowSquares);

        LongToBigNbr(Divid, biR, NumberLength);
        int nbrSquares = rowSquares[0];
        for (index = 1; index < nbrSquares; index++)
        {
          D = rowSquares[index];
          MultBigNbrByLongModN(biR, D, biR, TestNbr1, NumberLength);
          if (D == multiplier)
            DivBigNbrByLong(biU, D, biU, NumberLength);
        }
        rowPartial[0] = (positive? newDivid : -newDivid);
                   // Indicate last index with this hash.
        rowPartial[1] = -1;

        CalculateX(biQuadrCoeff, biLinearCoeff, biLinearDelta, index2, oddPolynomial,
                             biT, NumberLength);

        for (index = 0; index < squareRootSize; index++)
          rowPartial[index + 2] = (int) biT[index];
        rowPartial[squareRootSize + 2] = (int)oldSeed;
        nbrPartials++;
      }
  }

    public static void CalculateX(int[] biQuadrCoeff, int[] biLinearCoeff, 
            int[][] biLinearDelta, int index2, boolean oddPolynomial, int[] biT, 
            int NumberLength) {
        MultBigNbrByLong(biQuadrCoeff, index2 - SieveLimit, biT,
                NumberLength);
        AddBigNbr(biT, biLinearCoeff, biT, NumberLength);     // biT = Ax+B
        if (oddPolynomial)
        {                                                     // Ax+B (odd)
            SubtractBigNbr(biT, biLinearDelta[0], biT, NumberLength);
            SubtractBigNbr(biT, biLinearDelta[0], biT, NumberLength);
        }
        if ((biT[NumberLength - 1] & 0x40000000) != 0)
        {                                        // If number is negative
            ChSignBigNbr(biT, NumberLength);     // make it positive.
        }
    }
  
    static boolean InsertNewRelation(
    int[] rowMatrixB, int[] rowSquares,
    int biT[], int biU[], int biR[],
    int NumberLength)
  {
      for (int index=1; index < rowSquares[0]; index++)
    {
      long D = rowSquares[index];
      if (D == multiplier)
      {
          AddBigNbr(biU, TestNbr1, biU, NumberLength);
        DivBigNbrByLong(biU, D, biU, NumberLength);
      }
      else
      {
        MultBigNbrByLong(biR, D, biR, NumberLength);
      }
    }
      
    int i, k;
    int nbrColumns = rowMatrixB[0] - 1;
    int curRowMatrixB[];
    // Insert it only if it is different from previous relations.
    if (congruencesFound.get() >= matrixB.length)
    {                   // Discard excess congruences.
      return true;
    }
    for (i = 0; i < congruencesFound.get(); i++)
    {
      curRowMatrixB = matrixB[i];
      if (nbrColumns > curRowMatrixB.length)
        continue;
      if (nbrColumns == curRowMatrixB.length)
      {
        for (k = 1; k <= nbrColumns; k++)
        {
          if (rowMatrixB[k] != curRowMatrixB[k - 1])
            break;
        }
        if (k > nbrColumns)
          return false; // Do not insert same relation.
        if (rowMatrixB[k] > curRowMatrixB[k - 1])
          continue;
      }
      for (k = congruencesFound.get() - 1; k >= i; k--)
      {
        matrixB[k + 1] = matrixB[k];
        vectLeftHandSide[k + 1] = vectLeftHandSide[k];
      }
      break;
    }

    /* Convert negative numbers to the range 0 <= n < TestNbr */
    if ((TestNbr1[0] & 1) == 0)
    {
      DivBigNbrByLong(TestNbr1, 2, TestNbr2, NumberLength);
             // If biR >= TestNbr perform biR = biR - TestNbr.
      for (k = 0; k < NumberLength; k++)
        biT[k] = 0;
      AddBigNbrModN(biR, biT, biR, TestNbr2, NumberLength);
      ModInv.ModInvBigNbr(biR, biT, TestNbr2, NumberLength);
    } else {
      ModInv.ModInvBigNbr(biR, biT, TestNbr1, NumberLength);
    }
    if ((biU[NumberLength - 1] & 0x40000000) != 0)
      AddBigNbr(biU, TestNbr1, biU, NumberLength);

    // Compute biU / biR  (mod TestNbr)

    MultBigNbrModN(biU, biT, biR, TestNbr1, NumberLength);
    matrixB[i] = new int[nbrColumns];        // Add relation to matrix B.
    System.arraycopy(rowMatrixB, 1,          // Source
                     matrixB[i], 0,          // Destination
                     nbrColumns);            // Number of elements to copy.
    vectLeftHandSide[i] = new int[NumberLength];
    System.arraycopy(biR, 0,                 // Source
                     vectLeftHandSide[i], 0, // Destination
                     NumberLength);          // Number of elements to copy.
    congruencesFound.incrementAndGet();
    progressBar.setValue(congruencesFound.get());
    return true;
  }
}
