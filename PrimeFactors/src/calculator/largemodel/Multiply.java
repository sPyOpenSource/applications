/*
 * EDU.ksu.cis.calculator.defaultmodel.Multiply.java    3/20/01
 *
 * Copyright (c) 2001 Rod Howell, All Rights Reserved.
 */

package calculator.largemodel;

import static BigIntegers.FactorInteger.MaxUInt;
import BigIntegers.LargeInteger;
import calculator.EncodedOperation;

/**
 * The multiply operation.
 *
 * @author Rod Howell
 *         (<a href="mailto:howell@cis.ksu.edu">howell@cis.ksu.edu</a>)
 */
public class Multiply implements EncodedOperation {

  /**
   * @return 2.
   */
  @Override
  public int numOperands() {
    return 2;
  }

  /**
   * Returns the result of multiplyinging the given operands.
   * @throws ArrayIndexOutOfBoundsException  If <tt>operands</tt> is not of 
   *                                         length at least 2.
   * @throws ClassCastException  If the elements of <tt>operands</tt> are not 
   *                             both <tt>LargeInteger</tt>s.
   */
  @Override
  public Object doOperation(Object[] operands) 
    throws ArrayIndexOutOfBoundsException, ClassCastException {
    return ((LargeInteger) operands[0]).multiply((LargeInteger) operands[1]);
  }
  
  public static void MultBigNbrByLong(int Nbr1[], long Nbr2, int Prod[], int NumberLength){
    long Pr = 0;
    for (int i = 0; i < NumberLength; i++){
      Pr = (Pr >> 31) + Nbr2 * Nbr1[i];
      Prod[i] = (int)(Pr & MaxUInt);
    }
  }
  
  public static void MultBigNbr(int Nbr1[], int Nbr2[], int Prod[], int NumberLength){
    long carry, Pr;
    carry = Pr = 0;
    for (int i = 0; i < NumberLength; i++){
      Pr = carry & MaxUInt;
      carry >>>= 31;
      for (int j = 0; j <= i; j++){
        Pr += (long)Nbr1[j] * Nbr2[i - j];
        carry += (Pr >>> 31);
        Pr &= MaxUInt;
      }
      Prod[i] = (int)Pr;
    }
  }
}
