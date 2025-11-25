// <XMP>
// Self-Initializing Quadratic Sieve (SIQS) Factorization Method
//
// Written by Dario Alejandro Alpern (Buenos Aires - Argentina)
// Last updated May 15th, 2011. See http://www.alpertron.com.ar/ECM.HTM
//
// No part of this code can be used for commercial purposes without
// the written consent from the author. Otherwise it can be used freely
// except that you have to write somewhere in the code this header.
// 
package Factorzations.siqs;

import BigIntegers.AprtCleInteger;
import static BigIntegers.FactorInteger.DosALa31;
import static BigIntegers.StaticFunctions.BigIntToBigNbr;
import static BigIntegers.StaticFunctions.BigNbrToBigInt;

import static calculator.largemodel.Multiply.MultBigNbrByLong;
import static calculator.largemodel.Remainder.RemDivBigNbrByLong;

import static Factorzations.ecm.StaticFunctions.modPow;
import static Factorzations.siqs.StaticFunctions.JacobiSymbol;
import static Factorzations.siqs.Relation.congruencesFound;
import static Factorzations.siqs.Relation.matrixB;

import java.awt.Dimension;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

public class Siqs extends Relation
{
    static JComponent newContentPane = new JPanel();
    private final int numberThreads = 1;
    public static int nbrFactorsA, NbrPolynomials, nbrThreadFinishedPolySet;
    public static long trialDivisions, ValuesSieved, polynomialsSieved;
    
    public final static int[] TestNbr1 = new int[20], TestNbr2 = new int[20];
    public static int vectExpParity[];
    
    public static Thread[] threads;
    public static PrimeSieveData primeSieveData[];
    public static byte log2, threshold;
    public static int indexMinFactorA;
    public static int SieveLimit, NumberLength;
    public static int smallPrimeUpperLimit, firstLimit, secondLimit, thirdLimit;
    public static long largePrimeUpperBound;

    static final long DosALa31_1 = DosALa31 - 1;
    //public static AtomicLong newSeed = new AtomicLong();
    public static long newSeed = 0;
    public static BigInteger factorSiqs;

    public static int span;
    private long Power2, SqrRootMod, fact, FactorBase;
    private long D, E, Q, V, W, X, Y, T1, V1, W1, Y1;
    private double dNumberToFactor, Prod, bestadjust;
    static int arrmult[] = {1, 2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97};
    private final double adjustment[] = new double[arrmult.length];

  public Siqs(AprtCleInteger ecm)
  {
    //this.numberThreads = numberThreads;
    this.ecm = ecm;
    NumberLength = BigNbrToBigInt(ecm, TestNbr1);
  }
  
    public static void main(String args[]){
        //Create and set up the window.
        JFrame frame = new JFrame("SIQS");
        frame.setPreferredSize(new Dimension(150, 60));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
 
        //Create and set up the content pane.
        newContentPane.setOpaque(true); //content panes must be opaque
        frame.setContentPane(newContentPane);
 
        //Display the window.
        frame.pack();
        frame.setVisible(true);
        //BigInteger NN = new Siqs(new GCDBigInteger("56848340258081717636675345640784884269942720090962626378129322109017360249", true)).factor();
        BigInteger NN = new Siqs(new AprtCleInteger("28812358742867173186384364969583237304303880236561177111998089096093")).factor();
        //BigInteger NN = new Siqs(new ECM("1192729626363545334371466409576203777052945105817717", true)).factor();
        //BigInteger NN = new Siqs(new ECM("10564552020050085806002346595749373582609207089", true)).factor();
        //BigInteger NN = new Siqs(new ECM("10564552020050085806002346595749373582609207091", true)).factor();
        //BigInteger NN = new Siqs(new ECM("378773223823", true)).factor();
        //BigInteger NN = new Siqs(new ECM("15347", true)).factor();
        System.out.println(NN);
    }

  public static long getFactorsOfA(long seed, int aindex[])
  {
    int index1, index2, i;
    for (index1 = 0; index1 < nbrFactorsA; index1++)
    {
      do
      {
        seed = (1141592621 * seed + 321435) & 0xFFFFFFFFL;
        i = (int) (((seed * span) >> 32) + indexMinFactorA);
        for (index2 = 0; index2 < index1; index2++)
        {
          if (aindex[index2] == i || aindex[index2] == i + 1)
            break;
        }
      }
      while (index2 < index1);
      aindex[index1] = i;
    }
    for (index1 = 0; index1 < nbrFactorsA; index1++) // Sort factors of A.
    {
      for (index2 = index1 + 1; index2 < nbrFactorsA; index2++)
      {
        if (aindex[index1] > aindex[index2])
        {
          int tmp = aindex[index1];
          aindex[index1] = aindex[index2];
          aindex[index2] = tmp;
        }
      }
    }
    return seed;
  }

  /************************************************************************
   * Multithread procedure:                                               
   *                                                                      
   * 1) Main thread generates factor base and other parameters.           
   * 2) Start M threads where the number M is specified by the user in a 
   *    box beneath the applet.                                           
   * 3) For each polynomial:                                              
   *    3a) Main thread generates the data for the set of 2^n polynomials.
   *    3b) Each child thread computes a range of polynomials             
   *        (u*2^n/M to (u+1)*2^n/M exclusive).                           
   * Partial and full relation routines must be synchronized.             
   *
   * @return **************************************************************/
  public BigInteger factor()
  {
      System.out.println(ecm);
    
    ValuesSieved = 0;
    nbrPartials = 0;
    nbrThreadFinishedPolySet = 0;
    congruencesFound = new AtomicInteger();
    
    double Temp = Math.log(ecm.doubleValue());
    nbrPrimes1 = (int) Math.exp(Math.sqrt(Temp * Math.log(Temp)) * 0.363 - 1);
    SieveLimit = (int) Math.exp(8.5 + 0.015 * Temp) & 0xFFFFFFF8;
    nbrFactorsA = (int)(Temp * 0.051) + 1;
    if(nbrFactorsA <= 2) {
        nbrFactorsA = 2;
        nbrPrimes1 = 16;
    }
    span = nbrPrimes1 / (2 * nbrFactorsA * nbrFactorsA);
    if (nbrPrimes1 < 500)
      span *= 2;
    /*if(span == 0){
        span = 8;
    }*/
    NbrPolynomials = (1 << (nbrFactorsA - 1)) - 1;

    factorSiqs = null;
    threads = new Thread[numberThreads];
    primeSieveData = new PrimeSieveData[nbrPrimes1 + 3];
    primeTrialDivisionData = new PrimeTrialDivisionData[nbrPrimes1 + 3];
    
    TestNbr1[NumberLength++] = 0;
    System.arraycopy(TestNbr1, 0,         // Source
                     TestNbr2, 0,         // Destination
                     NumberLength);       // Length
    
    Setup();
    
      /*********************************************/
      /* Generate sieve threads                    */
      /*********************************************/
    for (int threadNumber = 0; threadNumber < numberThreads; threadNumber++)
    {
      new Thread(new Sieve(threadNumber)).start(); // Start new thread.
    }
    synchronized(matrixB)
    {
        try
        {
          matrixB.wait();
              matrixBlength = Relation.matrixB.length;
              int[] biT = new int[20];
              while (!LinearAlgebraPhase(biT, NumberLength));
              factorSiqs = BigIntToBigNbr(biT, NumberLength);  // Factor found.
        } catch(InterruptedException ie) {
            Logger.getLogger(Siqs.class.getName()).log(Level.SEVERE, null, ie);
        }
    }
    if (factorSiqs.signum() == 0)
      throw new ArithmeticException();
    for (int threadNumber = 0; threadNumber < numberThreads; threadNumber++)
    {                 // Wake up all sieve threads so they can terminate.
      if (threads[threadNumber].isAlive())
      {
        try
        {
          threads[threadNumber].interrupt();
        } catch (Exception e) {
            Logger.getLogger(Siqs.class.getName()).log(Level.SEVERE, null, e);
        }
      }
    }
    System.out.println("poly: " + polynomialsSieved + ", " + "trial: " + trialDivisions + ", " +
                                   "smooth: " + smoothsFound + ", " + "total: " + totalPartials + ", " +
                                   "partial: " + partialsFound + ", " + "values: " + ValuesSieved);
    return factorSiqs;
  }

    /************************
     * Compute startup data *
     ************************/
    private void Setup(){
    int i, j;
    for (int index = primeSieveData.length - 1; index >= 0; index--){
      primeSieveData[index] = new PrimeSieveData();
      primeSieveData[index].Bainv2 = new int[nbrFactorsA - 1];
    }
    for (int index = primeTrialDivisionData.length - 1; index >= 0; index--)
      primeTrialDivisionData[index] = new PrimeTrialDivisionData();
    
    /* search for best Knuth-Schroeppel multiplier */
    bestadjust = -10.0e0;
    primeSieveData[0].value = 1;
    primeTrialDivisionData[0].value = 1;
    PrimeSieveData rowPrimeSieveData = primeSieveData[1];
    PrimeTrialDivisionData rowPrimeTrialDivisionData = primeTrialDivisionData[1];
    rowPrimeSieveData.value = 2;
    rowPrimeTrialDivisionData.value = 2;
       // (2^31)^(j+1) mod 2
    rowPrimeTrialDivisionData.exp1 = rowPrimeTrialDivisionData.exp2 =
        rowPrimeTrialDivisionData.exp3 = rowPrimeTrialDivisionData.exp4 =
        rowPrimeTrialDivisionData.exp5 = rowPrimeTrialDivisionData.exp6 = 0;

    int NbrMod = ecm.and(BigInteger.valueOf(7)).intValue();
    for (j = 0; j < arrmult.length; j++)
    {
      int mod = (NbrMod * arrmult[j]) & 7;
      adjustment[j] = 0.34657359; /*  (ln 2)/2  */
      if (mod == 1)
        adjustment[j] *= (4.0e0);
      if (mod == 5)
        adjustment[j] *= (2.0e0);
      adjustment[j] -= Math.log((double) arrmult[j]) / (2.0e0);
    }
    
    int currentPrime = 3;
    while (currentPrime < 10000)
    {
      NbrMod = (int) RemDivBigNbrByLong(TestNbr1, currentPrime,
                              NumberLength);
      int jacobi = JacobiSymbol(NbrMod, currentPrime);
      double dp = (double) currentPrime;
      double logp = Math.log(dp) / dp;
      for (j = 0; j < arrmult.length; j++)
      {
        if (arrmult[j] == currentPrime)
        {
          adjustment[j] += logp;
        }
        else if (jacobi == JacobiSymbol(arrmult[j], currentPrime))
        {
          adjustment[j] += 2 * logp;
        }
      }
     calculate_new_prime1 :
      while (true)
      {
        currentPrime += 2;
        for (Q = 3; Q * Q <= currentPrime; Q += 2)
        { /* Check if currentPrime is prime */
          if (currentPrime % Q == 0)
            continue calculate_new_prime1;
        }
        break; /* Prime found */
      }
    }  /* end while */
    
    multiplier = 1;
    for (j = 0; j < arrmult.length; j++)
    {
      if (adjustment[j] > bestadjust)
      { /* find biggest adjustment */
        bestadjust = adjustment[j];
        multiplier = arrmult[j];
      }
    } /* end while */
    
    MultBigNbrByLong(TestNbr2, multiplier, TestNbr1, NumberLength);
    matrixPartial = new int[nbrPrimes1 * 8][NumberLength / 2 + 4];
    int length = nbrPrimes1 * 33 / 32;
    if(length < nbrPrimes1 + 50){
        length = nbrPrimes1 + 50;
    }
    progressBar = new JProgressBar(0, length);
    newContentPane.add(progressBar);
    progressBar.setStringPainted(true);
    progressBar.setValue(0);
    matrixB = new int[length][];
    vectLeftHandSide = new int[length][];
    vectExpParity = new int[length];
    
    rowPrimeSieveData.modsqrt = ecm.testBit(0) ? 1 : 0;
    switch ((int)TestNbr1[0] & 0x07)
    {
      case 1:
        log2 = 3;
        break;
      case 5:
        log2 = 1;
        break;
      default:
        log2 = 1;
        break;
    }
    if (multiplier != 1 && multiplier != 2)
    {
      rowPrimeSieveData = primeSieveData[2];
      primeTrialDivisionData[2].set(multiplier);
      rowPrimeSieveData.value = multiplier;
      rowPrimeSieveData.modsqrt = 0;
      j = 3;
    } else {
      j = 2;
    }
    
    currentPrime = 3;
    while (j < nbrPrimes1)
    { /* select small primes */
      NbrMod = (int) RemDivBigNbrByLong(TestNbr1, currentPrime,
                                NumberLength);
      if (currentPrime != multiplier &&
          JacobiSymbol(NbrMod, currentPrime) == 1)
      {
        /* use only if Jacobi symbol = 0 or 1 */
        rowPrimeSieveData = primeSieveData[j];
        primeTrialDivisionData[j].set(currentPrime);
        rowPrimeSieveData.value = currentPrime;
        if ((currentPrime & 3) == 3)
        {
          SqrRootMod = modPow(NbrMod, (currentPrime + 1) / 4, currentPrime);
        } else {
          if ((currentPrime & 7) == 5)    // currentPrime = 5 (mod 8)
          {
            SqrRootMod =
              modPow(NbrMod * 2, (currentPrime - 5) / 8, currentPrime);
            SqrRootMod =
              ((((2 * NbrMod * SqrRootMod % currentPrime) 
                * SqrRootMod - 1)
                % currentPrime)
                * NbrMod
                % currentPrime)
                * SqrRootMod
                % currentPrime;
          } else { /* p = 1 (mod 8) */
            Q = currentPrime - 1;
            E = 0;
            Power2 = 1;
            do {
              E++;
              Q /= 2;
              Power2 *= 2;
            } while ((Q & 1) == 0); /* E >= 3 */
            Power2 /= 2;
            X = 1;
            do {
              X++;
              Y = modPow(X, Q, currentPrime);
            } while (modPow(Y, Power2, currentPrime) == 1);
            X = modPow(NbrMod, (Q - 1) / 2, currentPrime);
            V = NbrMod * X % currentPrime;
            W = V * X % currentPrime;
            while (W != 1) {
              T1 = 0;
              D = W;
              while (D != 1) {
                D = D * D % currentPrime;
                T1++;
              }
              D = modPow(Y, 1 << (E - T1 - 1), currentPrime);
              Y1 = D * D % currentPrime;
              E = T1;
              V1 = V * D % currentPrime;
              W1 = W * Y1 % currentPrime;
              Y = Y1;
              V = V1;
              W = W1;
            } /* end while */
            SqrRootMod = V;
          } /* end if */
        } /* end if */
        rowPrimeSieveData.modsqrt = (int)SqrRootMod;
        j++;
      } /* end while */
      calculate_new_prime2 : while (true)
      {
        currentPrime += 2;
        for (Q = 3; Q * Q <= currentPrime; Q += 2)
        { /* Check if currentPrime is prime */
          if (currentPrime % Q == 0)
            continue calculate_new_prime2;
        }
        break; /* Prime found */
      }
    } /* End while */

    FactorBase = currentPrime;
    largePrimeUpperBound = 100 * FactorBase;
    dNumberToFactor = ecm.doubleValue();
    firstLimit = 2;
    for (j = 2; j < nbrPrimes1; j++)
    {
      firstLimit *= (int) (primeSieveData[j].value);
      if (firstLimit > 2 * SieveLimit)
        break;
    }
    dNumberToFactor *= multiplier;
    smallPrimeUpperLimit = j + 1;
    threshold =
      (byte) (Math
        .log(
          Math.sqrt(dNumberToFactor) * SieveLimit /
            (FactorBase * 64) /
            primeSieveData[j + 1].value)
        / Math.log(3) + 0x80);
    firstLimit = (int) (Math.log(dNumberToFactor) / 3);
    for (secondLimit = firstLimit; secondLimit < nbrPrimes1; secondLimit++)
    {
      if (primeSieveData[secondLimit].value * 2 > SieveLimit)
        break;
    }
    for (thirdLimit = secondLimit; thirdLimit < nbrPrimes1; thirdLimit++)
    {
      if (primeSieveData[thirdLimit].value > 2 * SieveLimit)
        break;
    }
    nbrPrimes2 = nbrPrimes1 - 4;

    Prod = Math.sqrt(2 * dNumberToFactor) / (double) SieveLimit;
    fact = (long) Math.pow(Prod, 1 / (double) nbrFactorsA);
    for (i = 2;; i++)
    {
      if (primeSieveData[i].value > fact)
        break;
    }
    indexMinFactorA = i - span / 2;
    if(indexMinFactorA <= 0) indexMinFactorA = 1;
    }
}
