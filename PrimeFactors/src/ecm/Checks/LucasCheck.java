
package ecm.Checks;

import BigIntegers.AprtCleInteger;
import java.math.BigInteger;

/**
 *
 * @author X. Wang
 */
public class LucasCheck {
    
    final AprtCleInteger NumberToFactor;
    BigInteger Prev = BigInteger.valueOf(-1);
    BigInteger Act = BigInteger.TWO;
    double addition = 0;
            
    public LucasCheck(AprtCleInteger NumberToFactor){
        this.NumberToFactor = NumberToFactor;
    }
    
    public int Check(){
      if (NumberToFactor.bitLength() > 32){
        int maxExpon = NumberToFactor.bitLength();
        double logar =
          (maxExpon - 32) * Math.log(2)
            + Math.log(NumberToFactor.shiftRight(maxExpon - 32).longValue());
        logar = (logar + addition) / 0.481211825059603; // index of L
        if (logar + 0.000005 - Math.floor(logar + 0.000005) > 0.00001)
          return -1;
      }
      BigInteger prev = Prev;
      BigInteger act  = Act;
      BigInteger next;
      int i = 0;
      while (true){
        int j = act.compareTo(NumberToFactor);
        if (j == 0){
          return i;
        }
        if (j > 0)
          return -1;
        next = prev.add(act);
        prev = act;
        act  = next;
        i++;
      }
  }
  
    public void Factor(int Index, BigInteger BigOriginal){
        NumberToFactor.NroFact = 1;
        NumberToFactor.setFactor(0, BigOriginal);
        InsertLucasFactor(Index, BigOriginal);
        NumberToFactor.SortFactors();
    }
  
  void InsertLucasFactor(int Index, BigInteger BigOriginal){
      BigInteger Fibo;
      BigInteger BigInt5 = BigInteger.valueOf(5);
      int k = 1; // Factor L(Index) 
      while (k * k <= Index){
        if (Index % k == 0){
          BigInteger Nro1 = get(k, Prev, Act).gcd(BigOriginal);
          NumberToFactor.InsertFactor(Nro1);
          if (k % 5 == 0){
            Fibo = get(k, BigInteger.ONE, BigInteger.ZERO);
            NumberToFactor.InsertFactor(
              BigInt5.multiply(Fibo).subtract(BigInt5).multiply(Fibo).add(BigInteger.ONE)
            );
            NumberToFactor.InsertFactor(
              BigInt5.multiply(Fibo).add(BigInt5).multiply(Fibo).add(BigInteger.ONE)
            );
          } else {
            NumberToFactor.InsertFactor(Nro1);
            NumberToFactor.InsertFactor(BigOriginal.divide(Nro1));
          }
          Nro1 = get(Index / k, Prev, Act).gcd(BigOriginal);
          NumberToFactor.InsertFactor(Nro1);
          if ((Index / k) % 5 == 0){
            Fibo = get(Index / k, BigInteger.ONE, BigInteger.ZERO);
            NumberToFactor.InsertFactor(
              BigInt5.multiply(Fibo).subtract(BigInt5).multiply(Fibo).add(BigInteger.ONE)
            );
            NumberToFactor.InsertFactor(
              BigInt5.multiply(Fibo).add(BigInt5).multiply(Fibo).add(BigInteger.ONE)
            );
          } else {
            NumberToFactor.InsertFactor(Nro1);
            NumberToFactor.InsertFactor(BigOriginal.divide(Nro1));
          }
        }
        k++;
      }
  }
  
  public static BigInteger get(int Index, BigInteger prev, BigInteger act){
      BigInteger next;
      for (int i = 1; i <= Index; i++){
        next = prev.add(act);
        prev = act;
        act  = next;
      }
      return act;
  }

}
