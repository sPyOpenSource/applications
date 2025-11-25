
package ecm.Checks;

import BigIntegers.AprtCleInteger;
import java.math.BigInteger;

/**
 *
 * @author X. Wang
 */
public class FibonacciCheck extends LucasCheck{
      
    public FibonacciCheck(AprtCleInteger NumberToFactor){
        super(NumberToFactor);
        Prev = BigInteger.ONE;
        Act  = BigInteger.ZERO;
        addition = 0.80471895621705;
    }
      
    @Override
    public void Factor(int Index, BigInteger BigOriginal){
      NumberToFactor.NroFact = 1;
      NumberToFactor.setFactor(0, BigOriginal);
      int Index2 = Index;
      while (Index2 % 2 == 0)
        Index2 /= 2;
      int k = 1; // Factor F(Index2) 
      while (k * k <= Index2){
        if (Index2 % k == 0){
          BigInteger Nro1 = get(k, Prev, Act).gcd(BigOriginal);
          NumberToFactor.InsertFactor(Nro1);
          NumberToFactor.InsertFactor(BigOriginal.divide(Nro1));
          Nro1 = get(Index / k, Prev, Act).gcd(BigOriginal);
          NumberToFactor.InsertFactor(Nro1);
          NumberToFactor.InsertFactor(BigOriginal.divide(Nro1));
        }
        k += 2;
      }
      Index2 = Index;
      while (Index2 % 2 == 0){
        Index2 /= 2;
        InsertLucasFactor(Index2, BigOriginal);
      }
      NumberToFactor.SortFactors();
    }
    
}
