
package test;

import BigIntegers.AprtCleInteger;
import static BigIntegers.ECMBigInteger.Moebius;
import static BigIntegers.ECMBigInteger.Totient;
import BigIntegers.LargeInteger;
import Factorzations.Classical;
import Factorzations.Lehman;
import Factorzations.ecm.ECM;
import Factorzations.siqs.PrimeTrialDivisionData;
import Factorzations.siqs.Siqs;

import static Factorzations.siqs.Siqs.primeTrialDivisionData;
import static Factorzations.siqs.StaticFunctions.getIndexFromDivisor;
import static Factorzations.ecm.StaticFunctions.Cos;
import static Factorzations.ecm.StaticFunctions.LongToBigNbr;
import static Factorzations.ecm.StaticFunctions.modPow;
import com.gazman.factor.VectorData;
import com.gazman.factor.matrix.BitMatrix;

import ecm.PrimeTest.AprtCle;
import ecm.BigIntegers.ModInv;
import ecm.Checks.PowerPM1Check;
import ecm.BigIntegers.GCD;
import ecm.Checks.FibonacciCheck;
import ecm.Checks.LucasCheck;
import ecm.Checks.PowerCheck;

import java.util.Random;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;

import junit.framework.TestCase;
import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import org.junit.Test;
import utilities.FFT;
import utilities.NTT;

/**
 *
 * @author X. Wang
 */
public class JUnitTest extends TestCase{
    private final Random rand = new Random();
    
    private int[] toArray(int... vec) {
        int[] result = new int[vec.length];
        System.arraycopy(vec, 0, result, 0, vec.length);
        return result;
    }
    
    private long[] toArray(long... vec){
        long[] result = new long[vec.length];
        System.arraycopy(vec, 0, result, 0, vec.length);
        return result;
    }
    
    private long[] randomVector(int len, long bound) {
        long[] result = new long[len];
        for (int i = 0; i < result.length; i++) {
            result[i] = rand.nextLong(bound);
        }
        return result;
    }
    
    @Test
    public void testAprtCle(){
        System.out.println("* JUnitTest: testAprtCle()");
        AprtCle aprtCle = new AprtCle();
        AprtCleInteger N = new AprtCleInteger("4811081079755727264065584381117");
        assertEquals(N.isProbablePrimeRobin(), aprtCle.isProbablePrime(N));
        N = new AprtCleInteger("102838308636");
        assertEquals(N.isProbablePrimeRobin(), N.isProbablePrime(1));
        N = new AprtCleInteger("1111111111111111111");
        assertEquals(aprtCle.isProbablePrime(N), true);
        N = new AprtCleInteger("1111111111111111111");
        assertEquals(N.isProbablePrimeRobin(), true);
        N = new AprtCleInteger("111111111111111111111111");
        assertEquals(aprtCle.isProbablePrime(N), false);
    }
    
    @Test
    public void testPowerPM1(){
        System.out.println("* JUnitTest: testPowerPM1()");
        BigInteger n = BigInteger.ONE.shiftLeft(19).subtract(BigInteger.ONE);
        assertEquals(n, BigInteger.ONE.shiftRight(-19).subtract(BigInteger.ONE));
        AprtCleInteger N = new AprtCleInteger(n.toString());
        N.addSelf2PD();
        PowerPM1Check check0 = new PowerPM1Check(N);
        assertEquals(check0.Check(), true);
        assertEquals(N.Base, 2);
        assertEquals(N.exp, -19);
        n = BigInteger.ONE.shiftLeft(23).subtract(BigInteger.ONE);
        N = new AprtCleInteger(n.toString());
        N.addSelf2PD();
        check0 = new PowerPM1Check(N);
        assertEquals(check0.Check(), true);
        assertEquals(N.getFactor(0).toString(), "47");
        N = new AprtCleInteger("5778");
        LucasCheck check1 = new LucasCheck(N);
        assertEquals(check1.Check(), 18);
        N = new AprtCleInteger("144");
        FibonacciCheck check2 = new FibonacciCheck(N);
        assertEquals(check2.Check(), 12);
    }
    
    @Test
    public void testPowerCheck(){
        System.out.println("* JUnitTest: testPowerCheck()");
        PowerCheck check = new PowerCheck(new AprtCleInteger("33222322321"));
        assertEquals(check.Check(0), 1);
        check = new PowerCheck(new AprtCleInteger("1411871524934571474917760655895199855251278439165992573432696950418064945008237961581572291078632362876307204161"));
        assertEquals(check.Check(0), 2);
        check = new PowerCheck(new AprtCleInteger("23146501155983534523631862959791900676295668658822499906167689"));
        assertEquals(check.Check(0), 2);
    }
    
    @Test
    public void testCos(){
        System.out.println("* JUnitTest: testCos()");
        assertEquals(Cos(0), 1);
        assertEquals(Cos(1), 0);
        assertEquals(Cos(4), -1);
    }
    
    @Test
    public void testLongToBigNbr(){
        System.out.println("* JUnitTest: testLongToBigNbr()");
        int[] x = toArray(0, 0);
        LongToBigNbr(1, x, 2);
        assertEquals(x[0], 1);
    }
    
    @Test
    public void testModPow(){
        System.out.println("* JUnitTest: testModPow()");
        assertEquals(modPow(0, 0, 2), 1);
        assertEquals(modPow(1, 0, 2), 1);
        assertEquals(modPow(0, 1, 2), 0);
        assertEquals(modPow(0, 0, 3), 1);
        assertEquals(modPow(1, 0, 3), 1);
        assertEquals(modPow(0, 1, 3), 0);
        assertEquals(modPow(1, 1, 3), 1);
        assertEquals(modPow(4, 5, 13), 10);
    }
    
    @Test
    public void testClassical(){
        System.out.println("* JUnitTest: testClassical()");
        Classical classical = new Classical(new BigInteger("33697387660717"));
        assertEquals(classical.factor().toString(), "36048893");
    }
    
    @Test
    public void testModInvBigNbr(){
        System.out.println("* JUnitTest: testModInvBigNbr()");
        int[] x = toArray(1, 2, 3);
        int[] y = new int[3];
        int[] z = toArray(1, 2, 3);
        ModInv.ModInvBigNbr(x, y, z, 3);
        assertArrayEquals(y, toArray(0, 0, 0));
        z[1] = 0;
        ModInv.ModInvBigNbr(x, y, z, 3);
        assertArrayEquals(y, toArray(1073741825, 2147483646, 2));
    }
    
    @Test
    public void testGcdBigNbr(){
        System.out.println("* JUnitTest: testGcdBigNbr()");
        int[] x = toArray(1, 2, 3);
        int[] y = toArray(1, 3, 5);
        int[] z = new int[3];
        GCD.GcdBigNbr(x, y, z, 3);
        assertArrayEquals(z, toArray(1, 0, 0));
    }
    
    @Test
    public void testECM(){
        System.out.println("* JUnitTest: testECM()");
        AprtCleInteger N = new AprtCleInteger("3369738766071892021");
        ECM ecm1 = new ECM(N);
        assertEquals(ecm1.factor().toString(), "204518747");
        N = new AprtCleInteger("1411871524934571474917760655895199855251278439165992573432696950418064945008237961581572291078632362876307204161");
        ECM ecm2 = new ECM(N);
        assertEquals(ecm2.factor().toString(), "37574878907783208914842069056982679950480785472350286369");
        N = new AprtCleInteger("23146501155983534523631862959791900676295668658822499906167689");
        ECM ecm3 = new ECM(N);
        assertEquals(ecm3.factor().toString(), "4811081079755727264065584381117");
    }
    
    @Test
    public void testLehman(){
        System.out.println("* JUnitTest: testLehman()");
        assertEquals(new Lehman(new BigInteger("581405072324003"), 39).factor().toString(), "11686859");
        for(int i = 200; i < 333; i++){
            if (i == 322){
                assertEquals(new Lehman(new BigInteger("3369738766071892021"), i).factor().toString(), "16476429743");
            } else {
                assertEquals(new Lehman(new BigInteger("3369738766071892021"), i).factor(), BigInteger.ONE);
            }
        }
    }
        
    @Test
    public void testSIQS(){
        System.out.println("* JUnitTest: testSIQS()");
        assertEquals(Factorzations.siqs.StaticFunctions.modInv(0, 2), 0);
        assertEquals(Factorzations.siqs.StaticFunctions.modInv(1, 3), 1);
        assertEquals(Factorzations.siqs.StaticFunctions.modInv(1, 1), 1);
        assertEquals(Factorzations.siqs.StaticFunctions.modInv(0, 1), 0);
        assertEquals(Factorzations.siqs.StaticFunctions.modInv(5, 0), -1);
        assertEquals(Factorzations.siqs.StaticFunctions.modInv(0, 5), 0);
        assertEquals(Factorzations.siqs.StaticFunctions.modInv(0, -2), 0);
        boolean[] result = new boolean[5];
        for(int i = 0; i < 5; i ++)
            result[i] = Factorzations.siqs.StaticFunctions.isProbablePrime(i + 2);
        boolean[] expect = {true, true, true, true, false};
        assertEquals(Arrays.toString(result), Arrays.toString(expect));
        assertEquals(Factorzations.siqs.StaticFunctions.isProbablePrime(2), true);
        assertEquals(Factorzations.siqs.StaticFunctions.isProbablePrime(13), true);
        assertEquals(Factorzations.siqs.StaticFunctions.isProbablePrime(22), false);
        assertEquals(Factorzations.siqs.StaticFunctions.isProbablePrime(121), false);
        assertEquals(Factorzations.siqs.StaticFunctions.isProbablePrime(111111111111111111L), false);
        Siqs SIQS = new Siqs(new AprtCleInteger("378773223823"));
        BigInteger NN = SIQS.factor();
        assertEquals(NN, BigInteger.valueOf(288227));
        SIQS = new Siqs(new AprtCleInteger("24195636269526952453313164257425744993821052695269502757059"));
        assertEquals(SIQS.factor().toString(), "3368803233864606281388302137");
    }
    
    @Test
    public void testGetSmallFactors(){
        System.out.println("* JUnitTest: testGetSmallFactors()");
        AprtCleInteger N = new AprtCleInteger("4563553357657676464464466");
        String[] primes = {"2", "7", "19", "31", "397"};
        N.GetSmallFactors(0);
        for (int i = 0; i < 5; i++){
            assertEquals(N.getPD(i).toString(), primes[i]);
        }
    }
    
    @Test
    public void testLargeInteger(){
        System.out.println("* JUnitTest: testLargeInteger()");
        long s = System.nanoTime();
        BigInteger m = BigInteger.ONE.shiftLeft(69755).subtract(BigInteger.ONE);
        long now = System.nanoTime();
        BigInteger M = m.pow(2);
        //System.out.println((System.nanoTime() - now) / 1000);
        
        LargeInteger NTT = new LargeInteger(m.toString());
        now = System.nanoTime();
        NTT = NTT.squareNTT();
        //System.out.println((System.nanoTime() - now) / 1000);
        assertEquals(NTT.toString(), M.toString());

        LargeInteger FFT = new LargeInteger(m.toString());
        now = System.nanoTime();
        FFT = FFT.squareFFT();
        //System.out.println((System.nanoTime() - now) / 1000 / 1000);
        assertEquals(FFT.toString(), M.toString());
        
        assertEquals(LargeInteger.isPrime(11), false);
        assertEquals(LargeInteger.isPrime(3217), true);
        now = System.nanoTime();
        assertEquals(LargeInteger.isPrime(20611), false);
        //System.out.println((System.nanoTime() - now) / 1000 / 1000 / 1000);
    }
    
    @Test
    public void testFFT(){
        long[] test = toArray(4l,0l,2l,0l,3l,0l,1l,0l);
        FFT.transform(test, FFT.roots);
        assertArrayEquals(test, toArray(10l, 24785888312599l, 36785211728549l, 6439346478484l, 2l, 48784535144503l, 33598564834656l, 60757783190832l));
        test = toArray(8l,0l,6l,0l,7l,0l,5l,0l);
        FFT.transform(test, FFT.roots);
        assertArrayEquals(test, toArray(26l, 59197769997571l, 36785211728549l, 34477934375670l, 2l, 20745947247325l, 33598564834656l, 26345901505868l));
        test = toArray(260l, 4l, 6373293787786l, 64010482775415l, 46821398113264l, 42404714915736l, 69130247803723l, 52794968856872l);
        FFT.transform(test, FFT.rootsInv);
        assertArrayEquals(test, toArray(256l, 416l, 488l, 480l, 272l, 128l, 40l, 0l));
    }
    
    @Test
    public void testReciprocal(){
        for(int i = 0; i < 16; i++){
            assertEquals(NTT.reciprocal((long)Math.pow(2, i), FFT.mod), FFT.lengthInv[i]);
            assertEquals(NTT.reciprocal(FFT.roots[i], FFT.mod), FFT.rootsInv[i]);
        }
    }
    
    @Test
    public void testForwardTransform() {
        long[] actual = NTT.transform(
               toArray(6l, 0l, 10l, 7l, 2l), 3, 11);
        long[] expect = toArray(3l, 7l, 0l, 5l, 4l);
        assertArrayEquals(expect, actual);
        long[] A = toArray(4l, 3l, 2l, 1l, 0l, 0l, 0l, 0l);
        long[] actualA = NTT.transform(
                A, FFT.roots[3], FFT.mod);
        expect = toArray(10l, 24785888312599l, 36785211728549l, 6439346478484l, 2l, 48784535144503l, 33598564834656l, 60757783190832l);
        assertArrayEquals(expect, actualA);
        long[] B = toArray(8l, 7l, 6l, 5l, 0l, 0l, 0l, 0l);
        long[] actualB = NTT.transform(
               B, FFT.roots[3], FFT.mod);
        expect = toArray(26l, 59197769997571l, 36785211728549l, 34477934375670l, 2l, 20745947247325l, 33598564834656l, 26345901505868l);
        assertArrayEquals(expect, actualB);
        for(int i = 0; i < actualA.length; i++)
            actualA[i] = FFT.modmult(actualA[i], actualB[i], FFT.mod);
        assertArrayEquals(actualA, toArray(260l, 46821398113264l, 6373293787786l, 69130247803723l, 4l, 42404714915736l, 64010482775415l, 52794968856872l));
        expect = NTT.inverseTransform(actualA, FFT.roots[3], FFT.mod);
        assertArrayEquals(expect, toArray(32l, 52l, 61l, 60l, 34l, 16l, 5l, 0l));
    }
	
    @Test
    public void testisPrimitiveRoot(){
        assertEquals(NTT.findModulus(5, 5), 11);
        assertEquals(NTT.findModulus(8, 649), 673);
        assertEquals(NTT.findPrimitiveRoot(5, 5, 11), 3);
        assertEquals(NTT.findPrimitiveRoot(8, 520, 521), 315);
        assertEquals(NTT.findPrimitiveRoot(8, 336, 337), 85);
        assertEquals(NTT.isPrimitiveRoot(64, 8, 673), true);
        assertEquals(NTT.isPrimitiveRoot(347, 8, 673), true);
        assertEquals(NTT.isPrimitiveRoot(609, 8, 673), true);
        assertEquals(NTT.isPrimitiveRoot(326, 8, 673), true);
        assertEquals(NTT.isPrimitiveRoot(85, 8, 337), true);
        assertEquals(NTT.isPrimitiveRoot(315, 8, 521), true);
        for(int i = 0; i < 16; i++)
            assertEquals(NTT.isPrimitiveRoot(FFT.roots[i], (long)Math.pow(2, i), FFT.mod), true);
    }
    
    @Test
    public void testInverseTransform() {
        int mod = 337;
        int root = 85;
        long[] A = toArray(4l, 3l, 2l, 1l, 0l, 0l, 0l, 0l);
        long[] actualA = NTT.transform(
                A, root, mod);
        long[] expect = toArray(10l, 329l, 298l, 126l, 2l, 271l, 43l, 301l);
        assertArrayEquals(expect, actualA);
        long[] B = toArray(8l, 7l, 6l, 5l, 0l, 0l, 0l, 0l);
        long[] actualB = NTT.transform(
               B, root, mod);
        expect = toArray(26l, 24l, 298l, 322l, 2l, 83l, 43l, 277l);
        assertArrayEquals(expect, actualB);
        for(int i = 0; i < actualA.length; i++)
            actualA[i] = actualA[i] * actualB[i] % mod;
        long[] actualC = NTT.inverseTransform(
                actualA, root, mod);
        expect = toArray(32l, 52l, 61l, 60l, 34l, 16l, 5l, 0l);
        assertArrayEquals(expect, actualC);
        actualC = NTT.circularConvolve(A, B);
        assertArrayEquals(actualC, expect);
        long sum = 0;
        for(int i = 0; i < actualA.length; i++){
            sum += actualC[i] * Math.pow(10, i);
        }
        long a = 0;
        for(int i = 0; i < A.length; i++){
            a += A[i] * Math.pow(10, i);
        }
        long b = 0;
        for(int i = 0; i < B.length; i++){
            b += B[i] * Math.pow(10, i);
        }
        assertEquals(sum, a * b);
        BigInteger AA = BigInteger.ZERO;
        for(int i = 0; i < 4; i++){
            BigInteger tmp = BigInteger.valueOf(A[i] * (long)Math.pow(100, i));
            AA = AA.add(tmp);
        }
        BigInteger BB = BigInteger.ZERO;
        for(int i = 0; i < 4; i++){
            BigInteger tmp = BigInteger.valueOf(B[i] * (long)Math.pow(100, i));
            BB = BB.add(tmp);
        }
        LargeInteger AAA = new LargeInteger(AA.toString());
        LargeInteger BBB = new LargeInteger(BB.toString());
        assertEquals(AAA.multiply(BBB).toString(), AA.multiply(BB).toString());
    }

    @Test
    public void testSimpleConvolution() {
        int mod = 673;
        int root = 326;
        long[] vec0 = NTT.transform(
                toArray(4l, 1l, 4l, 2l, 1l, 3l, 5l, 6l), root, mod);
        long[] vec1 = NTT.transform(
                toArray(6l, 1l, 8l, 0l, 3l, 3l, 9l, 8l), root, mod);
        long[] vec2 = new long[vec0.length];
        for (int i = 0; i < vec0.length; i++)
                vec2[i] = vec0[i] * (vec1[i]) % mod;
        long[] actual = NTT.inverseTransform(vec2, root, mod);
        long[] expect = toArray(123l, 120l, 106l, 92l, 139l, 144l, 140l, 124l);
        assertArrayEquals(expect, actual);
    }

    @Test
    public void testRoundtripRandomly() {
        final int trials = 300;
        for (int i = 0; i < trials; i++) {
            int vecLen = rand.nextInt(100) + 1;
            int maxVal = rand.nextInt(100) + 1;
            long[] vec = randomVector(vecLen, maxVal + 1);
            long mod = NTT.findModulus(vecLen, maxVal + 1);
            long root = NTT.findPrimitiveRoot(vecLen, mod - 1, mod);
            long[] temp = NTT.transform(vec, root, mod);
            long[] inv = NTT.inverseTransform(temp, root, mod);
            assertArrayEquals(vec, inv);
        }
    }

    @Test
    public void testLinearityRandomly() {
        final int trials = 300;
        for (int i = 0; i < trials; i++) {
            int vecLen = rand.nextInt(100) + 1;
            int maxVal = rand.nextInt(100) + 1;
            long[] vec0 = randomVector(vecLen, maxVal + 1);
            long[] vec1 = randomVector(vecLen, maxVal + 1);
            long mod = NTT.findModulus(vecLen, maxVal + 1);
            long root = NTT.findPrimitiveRoot(vecLen, mod - 1, mod);

            long[] out0 = NTT.transform(vec0, root, mod);
            long[] out1 = NTT.transform(vec1, root, mod);
            long[] out01 = new long[out0.length];
            for (int j = 0; j < out0.length; j++)
                    out01[j] = (out0[j] + out1[j]) % mod;

            long[] vec2 = new long[vec0.length];
            for (int j = 0; j < vec0.length; j++)
                    vec2[j] = (vec0[j] + vec1[j]) % mod;
            long[] out2 = NTT.transform(vec2, root, mod);
            assertArrayEquals(out2, out01);
        }
    }

    @Test
    public void testConvolutionRandomly() {
        final int trials = 300;
        for (int i = 0; i < trials; i++) {
            int vecLen = rand.nextInt(100) + 1;
            int maxVal = rand.nextInt(100) + 1;
            long[] vec0 = randomVector(vecLen, maxVal + 1);
            long[] vec1 = randomVector(vecLen, maxVal + 1);
            long[] actual = NTT.circularConvolve(vec0, vec1);
            long[] expect = NTT.circularConvolve(vec0, vec1);
            assertArrayEquals(expect, actual);
        }
    }
    
    @Test
    public void testAutomaticConvolution() {
        long[] actual = NTT.circularConvolve(
                toArray(4l, 1l, 4l, 2l, 1l, 3l, 5l, 6l),
                toArray(6l, 1l, 8l, 0l, 3l, 3l, 9l, 8l));
        long[] expect = toArray(123l, 120l, 106l, 92l, 139l, 144l, 140l, 124l);
        assertArrayEquals(expect, actual);
    }
    
    @Test
    public void testGetIndexFromDivisor() {
        System.out.println("* JUnitTest: testGetIndexFromDivisor()");
        primeTrialDivisionData = new PrimeTrialDivisionData[3 + 3];
        for(int i = 0; i < 6; i++){
            primeTrialDivisionData[i] = new PrimeTrialDivisionData();
            primeTrialDivisionData[i].value = i;
        }
        assertEquals(getIndexFromDivisor(2l, 3), 2);
    }
    
    @Test
    public void testSolve(){
        System.out.println("* JUnitTest: testSolve()");
        ArrayList<VectorData> list = new ArrayList<>();
        VectorData vector0 = new VectorData(new BitSet(), 0);
        vector0.vector.set(3);
        list.add(vector0);
        VectorData vector1 = new VectorData(new BitSet(), 26);
        vector1.vector.set(0);
        vector1.vector.set(1);
        vector1.vector.set(2);
        list.add(vector1);
        VectorData vector2 = new VectorData(new BitSet(), 71);
        vector2.vector.set(0);
        vector2.vector.set(1);
        vector2.vector.set(2);
        vector2.vector.set(3);
        list.add(vector2);
        VectorData vector3 = new VectorData(new BitSet(), 3);
        vector3.vector.set(0);
        vector3.vector.set(1);
        vector3.vector.set(2);
        list.add(vector3);
        BitMatrix bitMatrix = new BitMatrix();
        ArrayList<ArrayList<VectorData>> solutions = bitMatrix.solve(list);
        assertEquals(solutions.size(), 2);
    }
    
    @Test
    public void testFactorA(){
        Siqs.nbrFactorsA = 4;
        Siqs.span = 8;
        Siqs.indexMinFactorA = 0;
        int aindex[] = new int[4];
        Siqs.getFactorsOfA(0, aindex);
        assertArrayEquals(aindex, toArray(0,3,5,7));
    }
    
    @Test
    public void testTotient(){
        assertEquals(Totient(20), 8);
        AprtCleInteger N = new AprtCleInteger("111111111111111");
        N.GetSmallFactors(0);
        BigInteger t = N.Totient();
        assertEquals("67794900480000", t.toString());
    }
    
    @Test
    public void testMoebius(){
        assertEquals(Moebius(8), 0);
        AprtCleInteger N = new AprtCleInteger("111111111111111");
        N.GetSmallFactors(0);
        assertEquals(1, N.Moebius());
    }
    
    @Test
    public void test4Squares(){
        System.out.println("* JUnitTest: test4Squares()");
        AprtCleInteger N = new AprtCleInteger("111111111111111");
        N.GetSmallFactors(0);
        assertEquals(N.ComputeFourSquares(), true);
        System.out.println(Arrays.toString(N.Quad));
        BigInteger sum = BigInteger.ZERO;
        for (BigInteger Quad : N.Quad) {
            sum = sum.add(Quad.pow(2));
        }
        assertEquals(sum.toString(), "111111111111111");
    }
    
    @Test
    public void testSum(){
        AprtCleInteger N = new AprtCleInteger("111111111111111");
        N.GetSmallFactors(0);
        assertEquals(N.Sum().toString(), "161484774162432");
    }
}
