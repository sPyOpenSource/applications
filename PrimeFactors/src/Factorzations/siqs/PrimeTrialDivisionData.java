
package Factorzations.siqs;

import static Factorzations.siqs.Relation.nbrPrimes1;
import static Factorzations.siqs.Siqs.DosALa31_1;
import static Factorzations.siqs.Siqs.nbrFactorsA;
import static Factorzations.siqs.StaticFunctions.getIndexFromDivisor;
import static Factorzations.siqs.Siqs.primeTrialDivisionData;

/**
 * primes called the factor base
 * @author spy
 */

public class PrimeTrialDivisionData
{
    public int value, exp1, exp2, exp3, exp4, exp5, exp6;
    
    public void set(int currentPrime){
        value = currentPrime;
        long E = (1L << 31) % currentPrime;
        exp1 = (int)E;
        long D = E * E % currentPrime;
        exp2 = (int)D; // (2^31)^2 mod currentPrime
        D = D * E % currentPrime;
        exp3 = (int)D; // (2^31)^3 mod currentPrime
        D = D * E % currentPrime;
        exp4 = (int)D; // (2^31)^4 mod currentPrime
        D = D * E % currentPrime;
        exp5 = (int)D; // (2^31)^5 mod currentPrime
        D = D * E % currentPrime;
        exp6 = (int)D; // (2^31)^6 mod currentPrime
    }
    
    public static int Divid(int[] biR, int NumberLength, int Divisor){
        // Perform division
        long Rem = 0;
        long dQuot, Divid;
        int Dividend;
        switch (NumberLength)
        {
            case 7 : // {biR6 - biR0} <- {biR6 - biR0} / divis
              Dividend = biR[6];
              biR[6] = Dividend / Divisor;
              Rem = Dividend - biR[6] * Divisor;
            case 6 : // {biR5 - biR0} <- {biR5 - biR0} / divis
              Divid = biR[5] + (Rem << 31);
              dQuot = Divid / Divisor;
              Rem = Divid - dQuot * Divisor;
              biR[5] = (int)dQuot;
            case 5 : // {biR4 - biR0} <- {biR4 - biR0} / divis
              Divid = biR[4] + (Rem << 31);
              dQuot = Divid / Divisor;
              Rem = Divid - dQuot * Divisor;
              biR[4] = (int)dQuot;
            case 4 : // {biR3 - biR0} <- {biR3 - biR0} / divis
              Divid = biR[3] + (Rem << 31);
              dQuot = Divid / Divisor;
              Rem = Divid - dQuot * Divisor;
              biR[3] = (int)dQuot;
            case 3 : // {biR2 - biR0} <- {biR2 - biR0} / divis
              Divid = biR[2] + (Rem << 31);
              dQuot = Divid / Divisor;
              Rem = Divid - dQuot * Divisor;
              biR[2] = (int)dQuot;
            case 2:
              Divid = biR[1] + (Rem << 31);
              dQuot = Divid / Divisor;
              Rem = Divid - dQuot * Divisor;
              biR[1] = (int)dQuot;
            case 1:
              Divid = biR[0] + (Rem << 31);
              biR[0] = (int)(Divid / Divisor);
        }
        boolean cond = false;
        switch (NumberLength)
        {
            case 7 :
              cond = (biR[6] == 0) && biR[5] < 0x40000000;
              break;
            case 6 :
              cond = (biR[5] == 0) && biR[4] < 0x40000000;
              break;
            case 5 :
              cond = (biR[4] == 0) && biR[3] < 0x40000000;
              break;
            case 4 :
              cond = (biR[3] == 0) && biR[2] < 0x40000000;
              break;
            case 3 :
              cond = (biR[2] == 0) && biR[1] < 0x40000000;
              break;
            case 2:
              cond = (biR[1] == 0) && biR[0] < 0x40000000;
              break;
        }
        if(cond){
            NumberLength--;
        }
        return NumberLength;
    }
    
    public long getRem(int NumberLength, int[] Dividend){
        long DividLSDW = ((long)Dividend[1] * exp1) + Dividend[0];
        switch (NumberLength)
            {
                case 7 :
                  DividLSDW += (long)Dividend[6] * exp6;
                case 6 :
                  DividLSDW += (long)Dividend[5] * exp5;
                case 5 :
                  DividLSDW += (long)Dividend[4] * exp4;
                case 4 :
                  DividLSDW += (long)Dividend[3] * exp3;
                case 3 :
                  DividLSDW += (long)Dividend[2] * exp2;
            }
        return DividLSDW;
    }
    
   public static long PerformTrialDivision(PrimeSieveData primeSieveData[],
                            int rowMatrixBbeforeMerge[],
                            int index2, int aindex[],
                            int biDividend[], int rowSquares[],
                            int NumberLength,
                            boolean oddPolynomial)
  {
    int[] biR = new int[7];
    int nbrSquares = rowSquares[0];
    int Divisor, iRem;
    int nbrColumns = rowMatrixBbeforeMerge[0];
    PrimeSieveData rowPrimeSieveData;
    PrimeTrialDivisionData rowPrimeTrialDivisionData;
    switch (NumberLength)
    {
      case 7 :
        biR[6] = biDividend[6];
      case 6 :
        biR[5] = biDividend[5];
      case 5 :
        biR[4] = biDividend[4];
      case 4 :
        biR[3] = biDividend[3];
      case 3 :
        biR[2] = biDividend[2];
      case 1 :
      case 2 :
        biR[1] = biDividend[1];
        biR[0] = biDividend[0];
    }
    
    int expParity = 0;
    int indexFactorA = 0;
    int newFactorAIndex = aindex[0];
    for (int index = 1; index < nbrPrimes1; index++)
    {
        rowPrimeTrialDivisionData = primeTrialDivisionData[index];
        if(index < 3 || index == newFactorAIndex || NumberLength <= 2){
            if(index == newFactorAIndex){
                if(indexFactorA < nbrFactorsA){
                    newFactorAIndex = aindex[indexFactorA++];
                }
            }
        } else {
            rowPrimeSieveData = primeSieveData[index];
            Divisor = rowPrimeSieveData.value;
            if (oddPolynomial)
            {
                iRem = index2 - rowPrimeSieveData.soln1 +
                    rowPrimeSieveData.Bainv2_0;
            }
            else
            {
                iRem = index2 - rowPrimeSieveData.soln1;
            }
            iRem += ((iRem >> 31) & Divisor) - Divisor;
            iRem +=  (iRem >> 31) & Divisor;
            if (iRem >= Divisor)
            {
                iRem %= Divisor;
            }
            if((iRem != 0) && (iRem != (Divisor - rowPrimeSieveData.difsoln))){
                if (expParity != 0)
                {
                    rowMatrixBbeforeMerge[nbrColumns++] = index;
                    expParity = 0;
                }
                continue;
            }
        }
        Divisor = rowPrimeTrialDivisionData.value;
        while (rowPrimeTrialDivisionData.getRem(NumberLength, biR) % Divisor == 0)
        {   // Number is a multiple of prime.
            NumberLength = Divid(biR, NumberLength, Divisor);
            expParity = 1 - expParity;
            if (expParity == 0)
                rowSquares[nbrSquares++] = Divisor;
        }
        if (expParity != 0)
        {
            rowMatrixBbeforeMerge[nbrColumns++] = index;
            expParity = 0;
        }
        if(NumberLength <= 2 && biR[1] < 0x40000000) {
            long Divid = ((long)biR[1] << 31) + biR[0];
            int sqrtDivid = (int)Math.sqrt(Divid);
            if(Divisor > sqrtDivid){
                if(Divid > DosALa31_1) return 0;
                rowSquares[0] = nbrSquares;
                if(Divid <= primeTrialDivisionData[nbrPrimes1 - 1].value && Divid > 1){
                    index = getIndexFromDivisor(Divid, nbrPrimes1);
                    if(index == 0){
                        rowMatrixBbeforeMerge[0] = nbrColumns;
                        return 0;
                    }
                    rowMatrixBbeforeMerge[nbrColumns++] = index;
                    rowMatrixBbeforeMerge[0] = nbrColumns;
                    return 1;
                }
                rowMatrixBbeforeMerge[0] = nbrColumns;
                return Divid;
            }
        }
    }           /* end for */
  
    rowSquares[0] = nbrSquares;
    rowMatrixBbeforeMerge[0] = nbrColumns;
    if (NumberLength > 1)
    {
      return 0; // Very large quotient.
    }
    return biR[0];
  }
}
