/*
 * EDU.ksu.cis.calculator.defaultmodel.Add.java    3/20/01
 *
 * Copyright (c) 2001 Rod Howell, All Rights Reserved.
 */

package calculator.largemodel;

import static BigIntegers.FactorInteger.MaxUInt;
import BigIntegers.LargeInteger;
import calculator.EncodedOperation;

/**
 * The add operation.
 *
 * @author Rod Howell
 *         (<a href="mailto:howell@cis.ksu.edu">howell@cis.ksu.edu</a>)
 */
public class Add implements EncodedOperation {

  /**
   * @return 2.
   */
  @Override
  public int numOperands() {
    return 2;
  }

  /**
   * Returns the result of adding the given operands.
   * @throws ArrayIndexOutOfBoundsException  If <tt>operands</tt> is not of 
   *                                         length at least 2.
   * @throws ClassCastException  If the elements of <tt>operands</tt> are not 
   *                             both <tt>LargeInteger</tt>s.
   */
  @Override
  public Object doOperation(Object[] operands) 
    throws ArrayIndexOutOfBoundsException, ClassCastException {
    return ((LargeInteger) operands[0]).add((LargeInteger) operands[1]);
  }
  
  public static void AddBigNbr(int Nbr1[], int Nbr2[], int Sum[], int NumberLength){
    long carry = 0;
    for (int i = 0; i < NumberLength; i++){
      carry = (carry >> 31) + Nbr1[i] + Nbr2[i];
      Sum[i] = (int)(carry & MaxUInt);
    }
  }
  
  public static void AddBigNbr32(long Nbr1[], long Nbr2[], long Sum[], int NumberLength){
        long carry = 0;
        for (int i = 0; i < NumberLength; i++)
        {
          carry = (carry >> 32) + Nbr1[i] + Nbr2[i];
          Sum[i] = carry & 0xFFFFFFFFL;
        }
    }
}
