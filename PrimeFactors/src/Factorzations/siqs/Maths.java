
package Factorzations.siqs;

import BigIntegers.AprtCleInteger;
import static Factorzations.ecm.StaticFunctions.LongToBigNbr;
import static Factorzations.ecm.StaticFunctions.MultBigNbrByLongModN;
import static Factorzations.ecm.StaticFunctions.SubtractBigNbrModN;

import static Factorzations.siqs.Matrix.BlockLanczos;
import static Factorzations.siqs.Siqs.vectExpParity;
import static Factorzations.siqs.Siqs.vectLeftHandSide;
import static Factorzations.siqs.Siqs.TestNbr2;
import static Factorzations.siqs.Siqs.TestNbr1;

import static Factorzations.siqs.Relation.matrixB;
import static Factorzations.siqs.Relation.nbrPrimes1;
import static calculator.largemodel.Subtract.SubtractBigNbr;
import static BigIntegers.StaticFunctions.MultBigNbrModN;
import static ecm.BigIntegers.GCD.GcdBigNbr;

/**
 *
 * @author xuyi
 */
public class Maths {
    AprtCleInteger ecm;
    int matrixBlength;
    public static PrimeTrialDivisionData[] primeTrialDivisionData;
    private final int biU[] = new int[20], biR[] = new int[20];

    /************************/
    /* Linear algebra phase */
    /************************/
  boolean LinearAlgebraPhase(int biT[], int NumberLength)
  {
    int row, j;
    int[] rowMatrixB;
    int primeIndex;
    int NumberLengthBak;

    // Get new number of rows after erasing singletons.
    matrixBlength = EraseSingletons(nbrPrimes1);

    System.out.print(matrixBlength + "x");
    System.out.println(primeTrialDivisionData[0].exp2);
    
    primeTrialDivisionData[0].exp2 = 0;         // Restore correct value.
    long seed = 123456789;
    int[] matrixV = BlockLanczos(matrixBlength, seed);
      // The rows of matrixV indicate which rows must be multiplied so no
      // primes are multiplied an odd number of times.
    for (int mask = 1; mask != 0; mask *= 2)
    {
      LongToBigNbr(1, biT, NumberLength + 1);
      LongToBigNbr(1, biR, NumberLength + 1);
      
      for (row = matrixBlength - 1; row >= 0; row--)
        vectExpParity[row] = 0;
      NumberLengthBak = NumberLength;
      if (TestNbr1[NumberLength - 1] == 0 && TestNbr1[NumberLength - 2] < 0x40000000)
        NumberLength--;
      
      for (row = matrixBlength - 1; row >= 0; row--)
      {
        if ((matrixV[row] & mask) != 0)
        {
          MultBigNbrModN(vectLeftHandSide[row], biR, biU, TestNbr1,
                         NumberLength);
          for (j = 0; j <= NumberLength; j++)
            biR[j] = biU[j];
          
          rowMatrixB = matrixB[row];
          for (j = rowMatrixB.length - 1; j >= 0; j--)
          {
            primeIndex = rowMatrixB[j];
            vectExpParity[primeIndex] ^= 1;
            if (vectExpParity[primeIndex] == 0)
            {
              if (primeIndex == 0)
              {
                SubtractBigNbr(TestNbr1, biT, biT, NumberLength); // Multiply biT by -1.
              } else {
                MultBigNbrByLongModN(biT,
                        primeTrialDivisionData[primeIndex].value, biT,
                        TestNbr1, NumberLength);
              }
            }
          }
        }
      }
      NumberLength = NumberLengthBak;

      SubtractBigNbrModN(biR, biT, biR, TestNbr1, NumberLength);

      GcdBigNbr(biR, TestNbr2, biT, NumberLength);
      int index;
        for (index = 1; index < NumberLength; index++)
        {
          if (biT[index] != 0)
            break;
        }
      if (index < NumberLength || biT[0] > 1)
      { /* GCD is not 1 */
        for (index = 0; index < NumberLength; index++)
        {
          if (biT[index] != TestNbr2[index])
            break;
        }
        if (index < NumberLength)
        { /* GCD is not 1 */
          return true;
        }
      }
    }
    return false;
  }
  
  public static void mergeArrays(int aindex[], int nbrFactorsA,
                           int rowMatrixB[], int rowMatrixBeforeMerge[],
                           int rowSquares[])
  {
    int indexAindex = 0;
    int indexRMBBM = 1;
    int indexRMB = 1;
    int nbrColumns = rowMatrixBeforeMerge[0];

    while (indexAindex < nbrFactorsA && indexRMBBM < nbrColumns)
    {
      if (aindex[indexAindex] < rowMatrixBeforeMerge[indexRMBBM])
      {
        rowMatrixB[indexRMB++] = aindex[indexAindex++];
      }
      else if (aindex[indexAindex] > rowMatrixBeforeMerge[indexRMBBM])
      {
        rowMatrixB[indexRMB++] = rowMatrixBeforeMerge[indexRMBBM++];
      } else {
        rowSquares[rowSquares[0]++] =
                   primeTrialDivisionData[aindex[indexAindex++]].value;
        indexRMBBM++;
      }
    }
    while (indexAindex < nbrFactorsA)
      rowMatrixB[indexRMB++] = aindex[indexAindex++];
    while (indexRMBBM < nbrColumns)
      rowMatrixB[indexRMB++] = rowMatrixBeforeMerge[indexRMBBM++];
    rowMatrixB[0] = indexRMB;
  }
  
  /* Multiply binary matrices of length m x 32 by 32 x 32 */
  /* The product matrix has size m x 32. Then add it to a m x 32 matrix. */
  static void MatrixMultAdd(int[] LeftMatr, int[] RightMatr, int[] ProdMatr)
  {
    int leftMatr;
    int matrLength = LeftMatr.length;
    int prodMatr;
    int row, col;
    for (row = 0; row < matrLength; row++)
    {
      prodMatr = ProdMatr[row];
      leftMatr = LeftMatr[row];
      col = 0;
      while (leftMatr != 0)
      {
        if (leftMatr < 0)
          prodMatr ^= RightMatr[col];
        leftMatr *= 2;
        col++;
      }
      ProdMatr[row] = prodMatr;
    }
  }
  
  /* Multiply binary matrices of length m x 32 by 32 x 32 */
  /* The product matrix has size m x 32 */
  static void MatrixMultiplication(int[] LeftMatr, int[] RightMatr, int[] ProdMatr)
  {
    int leftMatr;
    int matrLength = LeftMatr.length;
    int prodMatr;
    int row, col;
    for (row = 0; row < matrLength; row++)
    {
      prodMatr = 0;
      leftMatr = LeftMatr[row];
      col = 0;
      while (leftMatr != 0)
      {
        if (leftMatr < 0)
          prodMatr ^= RightMatr[col];
        leftMatr *= 2;
        col++;
      }
      ProdMatr[row] = prodMatr;
    }
  }

  /* Multiply the transpose of a binary matrix of length n x 32 by */
  /* another binary matrix of length n x 32 */
  /* The product matrix has size 32 x 32 */
  static void MatrTranspMult(int[] LeftMatr, int[] RightMatr, int[] ProdMatr)
  {
    int prodMatr;
    int matrLength = LeftMatr.length;
    int row, col;
    int iMask = 1;
    for (col = 31; col >= 0; col--)
    {
      prodMatr = 0;
      for (row = 0; row < matrLength; row++)
      {
        if ((LeftMatr[row] & iMask) != 0)
          prodMatr ^= RightMatr[row];
      }
      ProdMatr[col] = prodMatr;
      iMask *= 2;
    }
  }

  static void MatrixAddition(int[] leftMatr, int[] rightMatr, int[] sumMatr)
  {
    for (int row = leftMatr.length - 1; row >= 0; row--)
      sumMatr[row] = leftMatr[row] ^ rightMatr[row];
  }

  static void MatrMultBySSt(int[] Matr, int diagS, int[] Prod)
  {
    for (int row = Matr.length - 1; row >= 0; row--)
      Prod[row] = diagS & Matr[row];
  }

  /* Compute Bt * B * input matrix where B is the matrix that holds the */
  /* factorization relations */
  static void MultiplyAByMatrix(
    int[] Matr,
    int[] TempMatr,
    int[] ProdMatr,
    int matrixBlength)
  {
    int index;
    int prodMatr;
    int row;
    int[] rowMatrixB;

    /* Compute TempMatr = B * Matr */
    for (row = matrixBlength - 1; row >= 0; row--)
      TempMatr[row] = 0;
    for (row = matrixBlength - 1; row >= 0; row--)
    {
      rowMatrixB = matrixB[row];
      for (index = rowMatrixB.length - 1; index >= 0; index--)
        TempMatr[rowMatrixB[index]] ^= Matr[row];
    }

    /* Compute ProdMatr = Bt * TempMatr */
    for (row = matrixBlength - 1; row >= 0; row--)
    {
      prodMatr = 0;
      rowMatrixB = matrixB[row];
      for (index = rowMatrixB.length - 1; index >= 0; index--)
        prodMatr ^= TempMatr[rowMatrixB[index]];
      ProdMatr[row] = prodMatr;
    }
  }
  
  // Exchange columns col1 and col2 of firstHi:firstLo and
  // the same columns of secondHi:secondLo
  // col1 and col2 ranges from 0 to 63.
  // Bit zero is the most significant bit.
  static void colexchange(int[] XmY, int[] V, int[] V1, int[] V2,
                           int col1, int col2)
  {
    int row;
    int mask1, mask2;
    int[] matr1, matr2;

    if (col1 == col2)
    {          // Cannot exchange the same column.
      return;
    }          // Exchange columns col1 and col2 of V1:V2
    mask1 = 0x80000000 >>> (col1 & 31);
    mask2 = 0x80000000 >>> (col2 & 31);
    matr1 = (col1 >= 32 ? V1 : V2);
    matr2 = (col2 >= 32 ? V1 : V2);
    for (row = V.length - 1; row >= 0; row--)
    {             // If both bits are different toggle them.
      if (((matr1[row] & mask1) == 0) != ((matr2[row] & mask2) == 0))
      {           // If both bits are different toggle them.
        matr1[row] ^= mask1;
        matr2[row] ^= mask2;
      }
    }
    // Exchange columns col1 and col2 of XmY:V
    matr1 = (col1 >= 32 ? XmY : V);
    matr2 = (col2 >= 32 ? XmY : V);
    for (row = V.length - 1; row >= 0; row--)
    {             // If both bits are different toggle them.
      if (((matr1[row] & mask1) == 0) != ((matr2[row] & mask2) == 0))
      {
        matr1[row] ^= mask1;
        matr2[row] ^= mask2;
      }
    }
  }

  // Add column col1 to col2 of firstHi:firstLo and
  // the same columns of secondHi:secondLo
  // Adding bits are done by performing XOR in them.
  // col1 and col2 ranges from 0 to 63.
  // Bit zero is the most significant bit.
  static void coladd(int[] XmY, int[] V, int[] V1, int[] V2,
                      int col1, int col2)
  {
    int row;
    int mask1, mask2;
    int[] matr1, matr2;

    if (col1 == col2)
    {
      return;
    }               // Add column col1 to column col2 of V1:V2
    mask1 = 0x80000000 >>> (col1 & 31);
    mask2 = 0x80000000 >>> (col2 & 31);
    matr1 = (col1 >= 32 ? V1 : V2);
    matr2 = (col2 >= 32 ? V1 : V2);
    for (row = V.length - 1; row >= 0; row--)
    {              // If bit to add is '1'...
      if ((matr1[row] & mask1) != 0)
      {            // Toggle bit in destination.
        matr2[row] ^= mask2;
      }
    }
                   // Add column col1 to column col2 of XmY:V
    matr1 = (col1 >= 32 ? XmY : V);
    matr2 = (col2 >= 32 ? XmY : V);
    for (row = V.length - 1; row >= 0; row--)
    {              // If bit to add is '1'...
      if ((matr1[row] & mask1) != 0)
      {            // Toggle bit in destination.
        matr2[row] ^= mask2;
      }
    }
  }
  
  int EraseSingletons(int nbrPrimes)
  {
    int row, column, delta;
    int[] rowMatrixB;
    //int matrixBlength = matrixB.length;
    int[] newColumns = new int[matrixBlength];
    // Find singletons in matrixB storing in array vectExpParity the number
    // of primes in each column.
    do
    {   // The singleton removal phase must run until there are no more
        // singletons to erase.
      for (column = nbrPrimes - 1; column >= 0; column--)
      {                  // Initialize number of primes per column to zero.
        vectExpParity[column] = 0;
      }
      for (row = matrixBlength - 1; row >= 0; row--)
      {                  // Traverse all rows of the matrix.
        rowMatrixB = matrixB[row];
        for (column = rowMatrixB.length - 1; column >= 0; column--)
        {                // A prime appeared in that column.
          vectExpParity[rowMatrixB[column]]++;
        }
      }
      row = 0;
      for (column = 0; column < nbrPrimes; column++)
      {
        if (vectExpParity[column] > 1)
        {                // Useful column found with at least 2 primes.
          newColumns[column] = row;
          primeTrialDivisionData[row++].value =
                              primeTrialDivisionData[column].value;
        }
      }
      nbrPrimes = row;
      delta = 0;
      // Erase singletons from matrixB. The rows to be erased are those where the
      // the corresponding element of the array vectExpParity equals 1.
      for (row = 0; row < matrixBlength; row++)
      {                  // Traverse all rows of the matrix.
        rowMatrixB = matrixB[row];
        for (column = rowMatrixB.length - 1; column >= 0; column--)
        {                // Traverse all columns.
          if (vectExpParity[rowMatrixB[column]] == 1)
          {              // Singleton found: erase this row.
            delta++;
            break;
          }
        }
        if (column < 0 && delta != 0)
        {                // Singleton not found: move row upwards.
          matrixB[row - delta] = matrixB[row];
          vectLeftHandSide[row - delta] = vectLeftHandSide[row];
        }
      }
      matrixBlength -= delta;      // Update number of rows of the matrix.
      for (row = 0; row < matrixBlength; row++)
      {                  // Traverse all rows of the matrix.
        rowMatrixB = matrixB[row];
        for (column = rowMatrixB.length - 1; column >= 0; column--)
        {                // Change all column indexes in this row.
          rowMatrixB[column] = newColumns[rowMatrixB[column]];
        }
      }
    } while (delta > 0);           // End loop if number of rows did not
                                   // change.
    primeTrialDivisionData[0].exp2 = nbrPrimes;
    return matrixBlength;
  }
}
