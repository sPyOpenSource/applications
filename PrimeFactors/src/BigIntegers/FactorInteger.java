
package BigIntegers;

import static BigIntegers.LargeInteger.getEnd;
import static BigIntegers.StaticFunctions.BigIntToBigNbr;
import static calculator.largemodel.Divide.DivBigNbrByLong;
import static calculator.largemodel.Remainder.RemDivBigNbrByLong;
import java.math.BigInteger;

/**
 *
 * @author X. Wang
 */
public class FactorInteger extends LargeInteger {
    public static final int TYP_AURIF = 100000000, TYP_TABLE  = 150000000;
    public static final int TYP_SIQS  = 200000000, TYP_LEHMAN = 250000000;
    public static final int TYP_RABIN = 300000000, TYP_EC     = 350000000;
    public static final BigInteger BigInt3 = BigInteger.valueOf(3L);
    public static final int NLen = 1200;
    public static final long MaxUInt = 0x7FFFFFFFL;
    public static final long DosALa31 = (long) 1 << 31;
    public int Base, exp;
    public int NbrFactors, NroFact;
    public boolean isPrime, isComplete;
    private final int SmallPrime[] = new int[670]; /* Primes < 5000 */
    int nbrPrimes;

    protected final BigInteger PD[] = new BigInteger[4000], Factores[] = new BigInteger[200];
    protected final int Exp[] = new int[4000], Typ[] = new int[4000];
    
    public BigInteger getPD(int index){
        return PD[index];
    }
    
    public void setPD(int index, BigInteger y){
        PD[index] = y;
    }
    
    public int getType(int index){
        return Typ[index];
    }
    
    public void setType(int index, int y){
        Typ[index] = y;
    }
    
    public void setExp(int index, int y){
        Exp[index] = y;
    }
    
    public int getExp(int index){
        return Exp[index];
    }
    
    public void setSize(int NumberLength){
        this.length = NumberLength;
    }
    
    public void pop(){
        length--;
    }
    
    public int size(){
        return length;
    }
    
    public int[] getIntArray(){
        return digits;
    }
    
    public long GetSmallFactors(final int Type)
    {
        long Div, TestComp;
        int i;
        boolean checkExpParity = false;

        //length = StaticFunctions.BigNbrToBigInt(this, digits);
        NbrFactors = 0;
        for (i = 0; i < 400; i++)
            Exp[i] = Typ[i] = 0;
        while ((digits[0] & 1) == 0)
        { /* N even */
            if (Exp[NbrFactors] == 0)
                PD[NbrFactors] = BigInteger.TWO;
            Exp[NbrFactors]++;
            DivBigNbrByLong(digits, 2, digits, length);
        }
        if (Exp[NbrFactors] != 0)
            NbrFactors++;
        while (RemDivBigNbrByLong(digits, 3, length) == 0)
        {
            if (Type == 1)
                checkExpParity ^= true;
            if (Exp[NbrFactors] == 0)
                PD[NbrFactors] = BigInt3;
            Exp[NbrFactors]++;
            DivBigNbrByLong(digits, 3, digits, length);
        }
        if (checkExpParity)
            return -1; /* Discard it */
        if (Exp[NbrFactors] != 0)
            NbrFactors++;
        Div = 5;
        TestComp = (long)digits[0] + ((long)digits[1] << 31);
        if (TestComp < 0)
        {
            TestComp = 10000 * DosALa31;
        } else {
            for (i = 2; i < length; i++)
            {
                if (digits[i] != 0)
                {
                    TestComp = 10000 * DosALa31;
                    break;
                }
            }
        }
        while (Div < 131072)
        {
            if (Div % 3 != 0)
            {
                while (RemDivBigNbrByLong(digits, Div, length) == 0)
                {
                    if (Type == 1 && Div % 4 == 3)
                        checkExpParity ^= true;
                    if (Exp[NbrFactors] == 0)
                        PD[NbrFactors] = BigInteger.valueOf(Div);
                    Exp[NbrFactors]++;
                    DivBigNbrByLong(digits, Div, digits, length);
                    TestComp = (long)digits[0] + ((long)digits[1] << 31);
                    if (TestComp < 0)
                    {
                        TestComp = 10000 * DosALa31;
                    } else {
                        for (i = 2; i < length; i++)
                        {
                            if (digits[i] != 0)
                            {
                                TestComp = 10000 * DosALa31;
                                break;
                            }
                        }
                    } /* end while */
                }
                if (checkExpParity)
                    return -1; /* Discard it */
                if (Exp[NbrFactors] != 0)
                    NbrFactors++;
            }
            Div += 2;
            if (TestComp < Div * Div && TestComp != 1)
            {
                if (Type == 1 && TestComp % 4 == 3)
                    return -1; /* Discard it */
                if (Exp[NbrFactors] != 0)
                    NbrFactors++;
                PD[NbrFactors] = BigInteger.valueOf(TestComp);
                Exp[NbrFactors] = 1;
                TestComp = 1;
                NbrFactors++;
                break;
            }
        } /* end while */
        return TestComp;
    }
    
    public void SortFactors(){
        for (int k = 0; k < NroFact - 1; k++){
          for (int j = k + 1; j < NroFact; j++){
            if (Factores[k].compareTo(Factores[j]) > 0){
              BigInteger Nro1 = Factores[k];
              Factores[k]     = Factores[j];
              Factores[j]     = Nro1;
            }
          }
        }
        for (int k = 0; k < NroFact; k++){
          PD[NbrFactors + k - 1]  = Factores[k];
          Exp[NbrFactors + k - 1] = 1;
        }
        NbrFactors += NroFact - 1;
    }
    
    public void SortFactorsPD(){
        for (int g = 0; g < NbrFactors - 1; g++){
            for (int j = g + 1; j < NbrFactors; j++){
                if (PD[g].compareTo(PD[j]) > 0){
                    BigInteger Nbr1 = PD[g];
                    PD[g] = PD[j];
                    PD[j] = Nbr1;
                    int i = Exp[g];
                    Exp[g] = Exp[j];
                    Exp[j] = i;
                    i = Typ[g];
                    Typ[g] = Typ[j];
                    Typ[j] = i;
                }
            }
        }
    }
  
    public void InsertFactor(BigInteger N)
    {
      for (int g = NroFact - 1; g >= 0; g--){
        Factores[NroFact] = Factores[g].gcd(N);
        if (!Factores[NroFact].equals(BigInteger.ONE) &&
            !Factores[NroFact].equals(Factores[g]))
        {
          Factores[g] = Factores[g].divide(Factores[NroFact]);
          NroFact++;
        }
      }
    }
    
  public void InsertNewFactor(final BigInteger InputFactor, int EC){
    int g, exp;

    /* Insert input factor */
    for (g = NbrFactors - 1; g >= 0; g--)
    {
      PD[NbrFactors] = PD[g].gcd(InputFactor);
      if (PD[NbrFactors].equals(BigInteger.ONE) || PD[NbrFactors].equals(PD[g]))
        continue;
      for (exp = 0; PD[g].remainder(PD[NbrFactors]).signum() == 0; exp++)
        PD[g] = PD[g].divide(PD[NbrFactors]);
      Exp[NbrFactors] = Exp[g] * exp;
      if (Typ[g] < 100000000)
      {
        Typ[g] = -EC;
        Typ[NbrFactors] = -TYP_EC - EC;
      }
      else if (Typ[g] < 150000000)
      {
        Typ[NbrFactors] = -Typ[g];
        Typ[g] = TYP_AURIF - Typ[g];
      }
      else if (Typ[g] < 200000000)
      {
        Typ[NbrFactors] = -Typ[g];
        Typ[g] = TYP_TABLE - Typ[g];
      }
      else if (Typ[g] < 250000000)
      {
        Typ[NbrFactors] = -Typ[g];
        Typ[g] = TYP_SIQS - Typ[g];
      }
      else if (Typ[g] < 300000000)
      {
        Typ[NbrFactors] = -Typ[g];
        Typ[g] = TYP_LEHMAN - Typ[g];
      } else {
        Typ[NbrFactors] = -Typ[g];
        Typ[g] = TYP_RABIN - Typ[g];
      }
      NbrFactors++;
    }
    SortFactorsPD();
}
  
    public FactorInteger(String val) {
        super(val);
    }
    
    public int get(int index){
        return digits[index];
    }
    
    public void set(int index, int x){
        digits[index] = x;
    }
    
    public void setFactor(int index, BigInteger x){
        Factores[index] = x;
    }
    
    public BigInteger getFactor(int index){
        return Factores[index];
    }
   
    public long getTestComp(){
        long TestComp = digits[0] + (digits[1] << 31);
           if (TestComp < 0){
                TestComp = 10000 * DosALa31;
           } else {
                for (int i = 2; i < length; i++){
                    if (digits[i] != 0){
                        TestComp = 10000 * DosALa31;
                        break;
                    }
                }
           } /* end while */
        return TestComp;
    }
    
    public void addSelf2PD(){
        length = getEnd(digits);
        PD[NbrFactors] = BigIntToBigNbr(digits, length);
        Exp[NbrFactors] = 1;
        Typ[NbrFactors] = -1; /* Unknown */
        NbrFactors++;
    }
    
    public int getSmallPrime(int i){
        return SmallPrime[i];
    }
    
    public void setSmallPrime(int i, int p){
        SmallPrime[i] = p;
    }
    
    public int getSmallPrimeLength(){
        return SmallPrime.length;
    }
    
    public int[] getSmallPrimes(){
        return SmallPrime;
    }
    
    public void setNbrPrimes(int n){
        nbrPrimes = n;
    }
}
