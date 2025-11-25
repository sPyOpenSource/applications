
package ecm.Checks;

import BigIntegers.ECMBigInteger;
import java.math.BigInteger;
import java.util.HashMap;

/**
 *
 * @author X. Wang
 */
public class PowerPM1Check {
    private final ECMBigInteger NumberToFactor;
    private final HashMap<Integer, String> M = new HashMap<>(), F = new HashMap<>();
    
    public PowerPM1Check(ECMBigInteger NumberToFactor){
        this.NumberToFactor = NumberToFactor;
        M.put(11, "23*89");
        M.put(23, "47*178481");
        M.put(29, "233*1103*2089");
        M.put(37, "223*616318177");
        M.put(41, "13367*164511353");
        M.put(43, "431*9719*2099863");
        M.put(47, "2351*4513*13264529");
        M.put(53, "6361*69431*20394401");
        M.put(59, "179951*3203431780337");
        M.put(67, "193707721*761838257287");
        M.put(71, "228479*48544121*212885833");
        M.put(73, "439*2298041*9361973132609");
        M.put(79, "2687*202029703*1113491139767");
        M.put(83, "167*57912614113275649087721");
        M.put(97, "11447*13842607235828485645766393");
        M.put(101, "7432339208719*341117531003194129");
        M.put(103, "2550183799*3976656429941438590393");
        M.put(109, "745988807*870035986098720987332873");
        M.put(113, "3391*23279*65993*1868569*1066818132868207");
        M.put(131, "263*10350794431055162386718619237468234569");
        
        F.put(32, "641*6700417");
        F.put(64, "274177*67280421310721");
        F.put(128, "59649589127497217*5704689200685129054721");
        F.put(256, "1238926361552897*93461639715357977769163558199606896584051237541638188580280321");
        F.put(512, "2424833*7455602825647884208337395736200454918783366342657*741640062627530801524787141901937474059940781097519023905821316144415759504705008092818711693940737");
        F.put(1024, "45592577*6487031809*4659775785220018543264560743076778192897");
        F.put(2048, "319489*974849*167988556341760475137*3560841906445833920513");
    }
    
    public boolean Check(){
        boolean plus1  = false;
        boolean minus1 = false;
        int Exponent   = 0;
        final int mod9     = NumberToFactor.mod(BigInteger.valueOf(9L)).intValue();
        final int maxExpon = NumberToFactor.bitLength();
        final double logar = (maxExpon - 32) * Math.log(2) + Math.log(NumberToFactor.shiftRight(maxExpon - 32).longValue());
        boolean ProcessExpon[] = new boolean[maxExpon + 1];
        boolean primes[]       = new boolean[2 * maxExpon + 3];
        for (int i = 2; i <= maxExpon; i++)
            ProcessExpon[i] = true;
        for (int i = 2; i < primes.length; i++)
            primes[i] = true;
        for (int i = 2; i * i < primes.length; i++){ // Generation of primes
            for (int j = i * i; j < primes.length; j += i)
                primes[j] = false;
        }
        // If the number +/- 1 is multiple of a prime but not a multiple
        // of its square then the number +/- 1 cannot be a perfect power.
        for (int i = 2; i < primes.length; i++){
            if (primes[i]){
                long i2 = (long) i * (long) i;
                int modulus = NumberToFactor.mod(BigInteger.valueOf(i)).intValue();
                if (modulus == 1 && NumberToFactor.mod(BigInteger.valueOf(i2)).longValue() != 1L)
                    plus1 = true;  // NumberFactor cannot be a power + 1
                if (modulus == i - 1 && NumberToFactor.mod(BigInteger.valueOf(i2)).longValue() != i2 - 1L)
                    minus1 = true; // NumberFactor cannot be a power - 1
                if (minus1 && plus1)
                    return false;
                if (!ProcessExpon[i / 2])
                    continue;
                if (modulus > (plus1 ? 1 : 2) && modulus < (minus1 ? i - 1 : i - 2)){
                    for (int j = i / 2; j <= maxExpon; j += i / 2)
                        ProcessExpon[j] = false;
                } else {
                    if (modulus == i - 2){
                        for (int j = i - 1; j <= maxExpon; j += i - 1)
                            ProcessExpon[j] = false;
                    }
                }
            }
        }
        for (int j = 2; j < 100; j++){
            double u = logar / Math.log(j) + 0.000005;
            Exponent = (int) Math.floor(u);
            if (u - Exponent > 0.00001)
                continue;
            if (Exponent % 3 == 0 && mod9 > 2 && mod9 < 7)
                continue;
            if (!ProcessExpon[Exponent])
                continue;
            if (ProcessExponent(Exponent))
                return true;
        }
        for (; Exponent >= 2; Exponent--){
            if (Exponent % 3 == 0 && mod9 > 2 && mod9 < 7)
                continue;
            if (!ProcessExpon[Exponent])
                continue;
            if (ProcessExponent(Exponent))
                return true;
        }
        return false;
    }
    
    private boolean ProcessExponent(int Exponent){
        BigInteger NFp1, NFm1, root, rootN1, rootN, rootbak, nextroot, dif;
        NFp1 = NumberToFactor.add(BigInteger.ONE);
        NFm1 = NumberToFactor.subtract(BigInteger.ONE);
        int intLog2N = NFp1.bitLength() - 1;
        double log2N =
          intLog2N
            + Math.log(NFp1.shiftRight(intLog2N - 32).add(BigInteger.ONE).doubleValue())
              / Math.log(2)
            - 32;
        log2N /= Exponent;
        if (log2N < 32){
          root = BigInteger.valueOf((long) Math.exp(log2N * Math.log(2)));
        } else {
          intLog2N = (int) Math.floor(log2N) - 32;
          root =
            BigInteger
              .valueOf((long) Math.exp((log2N - intLog2N) * Math.log(2)) + 10)
              .shiftLeft(intLog2N);
        }
        rootbak = root;
        while (true){
          rootN1 = root.pow(Exponent - 1);
          rootN = root.multiply(rootN1);
          dif = NFp1.subtract(rootN);
          if (dif.signum() == 0){ // Perfect power
            Cunningham(root, Exponent, BigInteger.ONE.negate(), NumberToFactor.getPD(NumberToFactor.NbrFactors - 1));
            return true;
          }
          nextroot =
            dif
              .add(BigInteger.ONE)
              .divide(BigInteger.valueOf(Exponent).multiply(rootN1))
              .add(root)
              .subtract(BigInteger.ONE);
          if (root.compareTo(nextroot) <= 0)
            break; // Not a perfect power
          root = nextroot;
        }
        root = rootbak;
        while (true){
          rootN1 = root.pow(Exponent - 1);
          rootN = root.multiply(rootN1);
          dif = NFm1.subtract(rootN);
          if (dif.signum() == 0){ // Perfect power
            Cunningham(root, Exponent, BigInteger.ONE, NumberToFactor.getPD(NumberToFactor.NbrFactors - 1));
            return true;
          }
          nextroot =
            dif
              .add(BigInteger.ONE)
              .divide(BigInteger.valueOf(Exponent).multiply(rootN1))
              .add(root)
              .subtract(BigInteger.ONE);
          if (root.compareTo(nextroot) <= 0)
            break; // Not a perfect power
          root = nextroot;
        }
        return false;
    }
    
    private void Cunningham(
    BigInteger BigBase,
    int Expon,
    BigInteger BigIncre,
    BigInteger BigOriginal)
  {
      int Incre = BigIncre.intValue();
      NumberToFactor.Base = BigBase.intValue();
      NumberToFactor.exp = Incre * Expon;
      NumberToFactor.NroFact = 1;
      NumberToFactor.setFactor(0, BigOriginal);
      int Expon2      = Expon;
      BigInteger Nro1 = BigBase.pow(Expon2).add(BigIncre);
      NumberToFactor.InsertFactor(Nro1);
        // Get known primitive factors. 
        String factorsAscii = null;
        if(Incre == 1){
            if(BigBase == BigInteger.TWO) factorsAscii = F.get(Expon);
        } else {
            if(BigBase == BigInteger.TWO) factorsAscii = M.get(Expon);
        }
        if (factorsAscii != null && factorsAscii.length() > 0){ // Factors found in server.
            int indexFactors = 0;
            do { // Loop through factors.
              int newIndexFactors = factorsAscii.indexOf('*', indexFactors);
              if (newIndexFactors > 0){
                NumberToFactor.InsertFactor(
                  new BigInteger(
                    factorsAscii.substring(indexFactors, newIndexFactors)));
              } else {
                NumberToFactor.InsertFactor(
                  new BigInteger(factorsAscii.substring(indexFactors)));
              }
              indexFactors = newIndexFactors + 1;
            } while (indexFactors > 0);
        }
      while (Expon2 % 2 == 0 && Incre == -1){
        Expon2 /= 2;
        NumberToFactor.InsertFactor(BigBase.pow(Expon2).add(BigInteger.ONE));
        NumberToFactor.InsertAurifFactors(BigBase, Expon2, 1);
      }
      int k = 1;
      while (k * k <= Expon){
        if (Expon % k == 0){
          if (k % 2 != 0){ /* Only for odd exponent */
            Nro1 = BigBase.pow(Expon / k).add(BigIncre).gcd(BigOriginal);
            NumberToFactor.InsertFactor(Nro1);
            NumberToFactor.InsertFactor(BigOriginal.divide(Nro1));
            NumberToFactor.InsertAurifFactors(BigBase, Expon / k, Incre);
          }
          if ((Expon / k) % 2 != 0){ /* Only for odd exponent */
            Nro1 = BigBase.pow(k).add(BigIncre).gcd(BigOriginal);
            NumberToFactor.InsertFactor(Nro1);
            NumberToFactor.InsertFactor(BigOriginal.divide(Nro1));
            NumberToFactor.InsertAurifFactors(BigBase, k, Incre);
          }
        }
        k++;
      }
      NumberToFactor.SortFactors();
  }
}
