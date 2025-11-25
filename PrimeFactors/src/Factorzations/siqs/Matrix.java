
package Factorzations.siqs;

import static Factorzations.siqs.Relation.matrixB;
import static Factorzations.siqs.Siqs.DosALa31_1;

/**
 *
 * @author xuyi
 */
public class Matrix extends Maths {
    /* finding the nullspace of a matrix over a finite field */
    public static int[] BlockLanczos(int matrixBlength, long seed)
  {
    int i, j, k;
    int oldDiagonalSSt, newDiagonalSSt;
    int index, indexC, mask;
    final int[] matrixD = new int[32];
    final int[] matrixE = new int[32];
    final int[] matrixF = new int[32];
    int[] matrixWinv0 = new int[32];
    int[] matrixWinv1 = new int[32];
    int[] matrixWinv2 = new int[32];
    int[] matrixVtV0 = new int[32];
    int[] matrixVt1V0 = new int[32];
    int[] matrixVt2V0 = new int[32];
    int[] matrixVtAV = new int[32];
    int[] matrixVt1AV1 = new int[32];
    int[] matrixCalcParenD = new int[32];
    int[] vectorIndex = new int[64];
    final int[] matrixAV = new int[matrixBlength];
    int[] matrixV0 = new int[matrixBlength];
    int[] matrixV1 = new int[matrixBlength];
    int[] matrixV2 = new int[matrixBlength];
    int[] matrixXmY = new int[matrixBlength];
    
                                     // Matrix that holds temporary data
    int[] matrixCalc3 = new int[matrixBlength];
    int[] matrixTemp;
    int[] matrixCalc1 = new int[32]; // Matrix that holds temporary data
    int[] matrixCalc2 = new int[32]; // Matrix that holds temporary data
    
    int[] matr;
    int rowMatrixV;
    int rowMatrixXmY;
    int Temp0, Temp1;
    int stepNbr = 0;
    int currentOrder, currentMask;
    int row, col;
    int leftCol, rightCol;
    int minind, min, minanswer;
    int[] rowMatrixB;

    newDiagonalSSt = oldDiagonalSSt = -1;
    
    long mult = 62089911;
    long add = 54325442;
    /* Initialize matrix X-Y and matrix V_0 with random data */
    for (i = matrixXmY.length - 1; i >= 0; i--)
    {
      matrixXmY[i] = (int)seed;
      seed = (seed * mult + add) % DosALa31_1;
      matrixXmY[i] += (int)(seed * 6543265);
      seed = (seed * mult + add) % DosALa31_1;
      matrixV0[i] = (int)seed;
      seed = (seed * mult + add) % DosALa31_1;
      matrixV0[i] += (int)(seed * 6543265);
      seed = (seed * mult + add) % DosALa31_1;
    }
    // Compute matrix Vt(0) * V(0)
    MatrTranspMult(matrixV0, matrixV0, matrixVtV0);
    while (true)
    {
      if ((stepNbr * 3200 / matrixBlength) > 105)
      {
          System.out.println("here");
        return null;
      }
      oldDiagonalSSt = newDiagonalSSt;
      stepNbr++;
      // Compute matrix A * V(i)
      MultiplyAByMatrix(matrixV0, matrixCalc3, matrixAV, matrixBlength);
      // Compute matrix Vt(i) * A * V(i)
      MatrTranspMult(matrixV0, matrixAV, matrixVtAV);

      /* If Vt(i) * A * V(i) = 0, end of loop */
      for (i = matrixVtAV.length - 1; i >= 0; i--)
      {
        if (matrixVtAV[i] != 0)
          break;
      }
      if (i < 0)
      {
        break;
      } /* End X-Y calculation loop */

      /* Selection of S(i) and W(i) */

      matrixTemp  = matrixWinv2;
      matrixWinv2 = matrixWinv1;
      matrixWinv1 = matrixWinv0;
      matrixWinv0 = matrixTemp;

      mask = 1;
      for (j = 31; j >= 0; j--)
      {
        matrixD[j] = matrixVtAV[j]; /*  D = VtAV    */
        matrixWinv0[j] = mask;      /*  Winv = I    */
        mask *= 2;
      }

      index = indexC = 31;
      for (mask = 1; indexC >= 0; mask *= 2)
      {
        if ((oldDiagonalSSt & mask) != 0)
        {
          matrixE[index] = indexC;
          matrixF[index] = mask;
          index--;
        }
        indexC--;
      }
      indexC = 31;
      for (mask = 1; indexC >= 0; mask *= 2)
      {
        if ((oldDiagonalSSt & mask) == 0)
        {
          matrixE[index] = indexC;
          matrixF[index] = mask;
          index--;
        }
        indexC--;
      }
      newDiagonalSSt = 0;
      for (j = 0; j < 32; j++)
      {
        currentOrder = matrixE[j];
        currentMask = matrixF[j];
        for (k = j; k < 32; k++)
        {
          if ((matrixD[matrixE[k]] & currentMask) != 0)
            break;
        }
        if (k < 32)
        {
          i = matrixE[k];
          Temp0 = matrixWinv0[i];
          matrixWinv0[i] = matrixWinv0[currentOrder];
          matrixWinv0[currentOrder] = Temp0;
          Temp1 = matrixD[i];
          matrixD[i] = matrixD[currentOrder];
          matrixD[currentOrder] = Temp1;
          newDiagonalSSt |= currentMask;
          for (k = 31; k >= 0; k--)
          {
            if (k != currentOrder && ((matrixD[k] & currentMask) != 0))
            {
              matrixWinv0[k] ^= Temp0;
              matrixD[k] ^= Temp1;
            }
          } /* end for k */
        } else {
          for (k = j; k < 32; k++)
          {
            if ((matrixWinv0[matrixE[k]] & currentMask) != 0)
              break;
          }
          i = matrixE[k];
          Temp0 = matrixWinv0[i];
          matrixWinv0[i] = matrixWinv0[currentOrder];
          matrixWinv0[currentOrder] = Temp0;
          Temp1 = matrixD[i];
          matrixD[i] = matrixD[currentOrder];
          matrixD[currentOrder] = Temp1;
          for (k = 31; k >= 0; k--)
          {
            if ((matrixWinv0[k] & currentMask) != 0)
            {
              matrixWinv0[k] ^= Temp0;
              matrixD[k] ^= Temp1;
            }
          } /* end for k */
        } /* end if */
      } /* end for j */

      /* Compute D(i), E(i) and F(i) */
      if (stepNbr >= 3)
      {
        // F = -Winv(i-2) * (I - Vt(i-1)*A*V(i-1)*Winv(i-1)) * ParenD * S*St
        MatrixMultiplication(matrixVt1AV1, matrixWinv1, matrixCalc2);
        index = 31; /* Add identity matrix */
        for (mask = 1; mask != 0; mask *= 2)
        {
          matrixCalc2[index] ^= mask;
          index--;
        }
        MatrixMultiplication(matrixWinv2, matrixCalc2, matrixCalc1);
        MatrixMultiplication(matrixCalc1, matrixCalcParenD, matrixF);
        MatrMultBySSt(matrixF, newDiagonalSSt, matrixF);
      }

      // E = -Winv(i-1) * Vt(i)*A*V(i) * S*St
      if (stepNbr >= 2)
      {
        MatrixMultiplication(matrixWinv1, matrixVtAV, matrixE);
        MatrMultBySSt(matrixE, newDiagonalSSt, matrixE);
      }
      // ParenD = Vt(i)*A*A*V(i) * S*St + Vt(i)*A*V(i)
      // D = I - Winv(i) * ParenD
      MatrTranspMult(matrixAV, matrixAV, matrixCalc1); // Vt(i)*A*A*V(i)
      MatrMultBySSt(matrixCalc1, newDiagonalSSt, matrixCalc1);
      MatrixAddition(matrixCalc1, matrixVtAV, matrixCalcParenD);
      MatrixMultiplication(matrixWinv0, matrixCalcParenD, matrixD);
      index = 31; /* Add identity matrix */
      for (mask = 1; index >= 0; mask *= 2)
      {
        matrixD[index] ^= mask;
        index--;
      }

      /* Update value of X - Y */
      MatrixMultiplication(matrixWinv0, matrixVtV0, matrixCalc1);
      MatrixMultAdd(matrixV0, matrixCalc1, matrixXmY);

      /* Compute value of new matrix V(i) */
      // V(i+1) = A * V(i) * S * St + V(i) * D + V(i-1) * E + V(i-2) * F
      MatrMultBySSt(matrixAV, newDiagonalSSt, matrixCalc3);
      MatrixMultAdd(matrixV0, matrixD, matrixCalc3);
      if (stepNbr >= 2)
      {
        MatrixMultAdd(matrixV1, matrixE, matrixCalc3);
        if (stepNbr >= 3)
          MatrixMultAdd(matrixV2, matrixF, matrixCalc3);
      }
      /* Compute value of new matrix Vt(i)V0 */
        // Vt(i+1)V(0) = Dt * Vt(i)V(0) + Et * Vt(i-1)V(0) + Ft * Vt(i-2)V(0)
      MatrTranspMult(matrixD, matrixVtV0, matrixCalc2);
      if (stepNbr >= 2)
      {
        MatrTranspMult(matrixE, matrixVt1V0, matrixCalc1);
        MatrixAddition(matrixCalc1, matrixCalc2, matrixCalc2);
        if (stepNbr >= 3)
        {
          MatrTranspMult(matrixF, matrixVt2V0, matrixCalc1);
          MatrixAddition(matrixCalc1, matrixCalc2, matrixCalc2);
        }
      }
      matrixTemp   = matrixV2;
      matrixV2     = matrixV1;
      matrixV1     = matrixV0;
      matrixV0     = matrixCalc3;
      matrixCalc3  = matrixTemp;
      
      matrixTemp   = matrixVt2V0;
      matrixVt2V0  = matrixVt1V0;
      matrixVt1V0  = matrixVtV0;
      matrixVtV0   = matrixCalc2;
      matrixCalc2  = matrixTemp;
      
      matrixTemp   = matrixVt1AV1;
      matrixVt1AV1 = matrixVtAV;
      matrixVtAV   = matrixTemp;
    } /* end while */

    /* Find matrix V1:V2 = B * (X-Y:V) */
    for (row = matrixBlength - 1; row >= 0; row--)
      matrixV1[row] = matrixV2[row] = 0;
    for (row = matrixBlength - 1; row >= 0; row--)
    {
      rowMatrixB = matrixB[row];
      rowMatrixXmY = matrixXmY[row];
      rowMatrixV = matrixV0[row];
       // The vector rowMatrixB includes the indexes of the columns set to '1'.
      for (index = rowMatrixB.length - 1; index >= 0; index--)
      {
        col = rowMatrixB[index];
        matrixV1[col] ^= rowMatrixXmY;
        matrixV2[col] ^= rowMatrixV;
      }
    }
    rightCol = 64;
    leftCol = 0;
    while (leftCol < rightCol)
    {
      for (col = leftCol; col < rightCol; col++)
      {       // For each column find the first row which has a '1'.
              // Columns outside this range must have '0' in all rows.
        matr = (col >= 32 ? matrixV1 : matrixV2);
        mask = 0x80000000 >>> (col & 31);
        vectorIndex[col] = -1;    // indicate all rows in zero in advance.
        for (row = 0; row < matr.length; row++)
        {
          if ((matr[row] & mask) != 0)
          {               // First row for this mask is found. Store it.
            vectorIndex[col] = row;
            break;
          }
        }
      }
      for (col = leftCol; col < rightCol; col++)
      {
        if (vectorIndex[col] < 0)
        {  // If all zeros in col 'col', exchange it with first column with
           // data different from zero (leftCol).
          colexchange(matrixXmY, matrixV0, matrixV1, matrixV2, leftCol, col);
          vectorIndex[col] = vectorIndex[leftCol];
          vectorIndex[leftCol] = -1;  // This column now has zeros.
          leftCol++;                  // Update leftCol to exclude that column.
        }
      }
      if (leftCol == rightCol)
        break;
        // At this moment all columns from leftCol to rightCol are non-zero.
        // Get the first row that includes a '1'.
      min = vectorIndex[leftCol];
      minind = leftCol;
      for (col = leftCol + 1; col < rightCol; col++)
      {
        if (vectorIndex[col] < min)
        {
          min = vectorIndex[col];
          minind = col;
        }
      }
      minanswer = 0;
      for (col = leftCol; col < rightCol; col++)
      {
        if (vectorIndex[col] == min)
          minanswer++;
      }
      if (minanswer > 1)
      {            // Two columns with the same first row to '1'.
        for (col = minind + 1; col < rightCol; col++)
        {
          if (vectorIndex[col] == min)
          {        // Add first column which has '1' in the same row to
                   // the other columns so they have '0' in this row after
                   // this operation.
            coladd(matrixXmY, matrixV0, matrixV1, matrixV2, minind, col);
          }
        }
      } else {
        rightCol--;
        colexchange(matrixXmY, matrixV0, matrixV1, matrixV2, minind, rightCol);
      }
    }

    leftCol = 0; /* find linear independent solutions */
    while (leftCol < rightCol)
    {
      for (col = leftCol; col < rightCol; col++)
      {         // For each column find the first row which has a '1'.
        matr = (col >= 32 ? matrixXmY : matrixV0);
        mask = 0x80000000 >>> (col & 31);
        vectorIndex[col] = -1;    // indicate all rows in zero in advance.
        for (row = 0; row < matrixV1.length; row++)
        {
          if ((matr[row] & mask) != 0)
          {         // First row for this mask is found. Store it.
            vectorIndex[col] = row;
            break;
          }
        }
      }
      for (col = leftCol; col < rightCol; col++)
      {  // If all zeros in col 'col', exchange it with last column with
         // data different from zero (rightCol).
        if (vectorIndex[col] < 0)
        {
          rightCol--;                 // Update rightCol to exclude that column.
          colexchange(matrixXmY, matrixV0, matrixV1, matrixV2, rightCol, col);
          vectorIndex[col] = vectorIndex[rightCol];
          vectorIndex[rightCol] = -1; // This column now has zeros.
        }
      }
      if (leftCol == rightCol)
        break;
        // At this moment all columns from leftCol to rightCol are non-zero.
        // Get the first row that includes a '1'.
      min = vectorIndex[leftCol];
      minind = leftCol;
      for (col = leftCol + 1; col < rightCol; col++)
      {
        if (vectorIndex[col] < min)
        {
          min = vectorIndex[col];
          minind = col;
        }
      }
      minanswer = 0;
      for (col = leftCol; col < rightCol; col++)
      {
        if (vectorIndex[col] == min)
          minanswer++;
      }
      if (minanswer > 1)
      {            // At least two columns with the same first row to '1'.
        for (col = minind + 1; col < rightCol; col++)
        {
          if (vectorIndex[col] == min)
          {        // Add first column which has '1' in the same row to
                   // the other columns so they have '0' in this row after
                   // this operation.
            coladd(matrixXmY, matrixV0, matrixV1, matrixV2, minind, col);
          }
        }
      } else {
        colexchange(matrixXmY, matrixV0, matrixV1, matrixV2, minind, leftCol);
        leftCol++;
      }
    }
    return matrixV0;
  }
}
