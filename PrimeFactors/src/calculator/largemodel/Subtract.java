/*
 * EDU.ksu.cis.calculator.defaultmodel.Subtract.java    3/20/01
 *
 * Copyright (c) 2001 Rod Howell, All Rights Reserved.
 */

package calculator.largemodel;

import static BigIntegers.FactorInteger.MaxUInt;
import BigIntegers.LargeInteger;
import calculator.EncodedOperation;

/**
 * The subtract operation.
 *
 * @author Rod Howell
 *         (<a href="mailto:howell@cis.ksu.edu">howell@cis.ksu.edu</a>)
 */
public class Subtract implements EncodedOperation {

  /**
   * @return 2.
   */
  @Override
  public int numOperands() {
    return 2;
  }

  /**
   * Returns the result of subtracting operand[1] from operand[0].
   * @throws ArrayIndexOutOfBoundsException  If <tt>operands</tt> is not of 
   *                                         length at least 2.
   * @throws ClassCastException  If the elements of <tt>operands</tt> are not 
   *                             both <tt>LargeInteger</tt>s.
   */
  @Override
  public Object doOperation(Object[] operands) 
    throws ArrayIndexOutOfBoundsException, ClassCastException {
    return ((LargeInteger) operands[0]).subtract((LargeInteger) operands[1]);
  }
  
  public static void SubtractBigNbr(int Nbr1[], int Nbr2[], int Diff[], int NumberLength){
    long carry = 0;
    for (int i = 0; i < NumberLength; i++){
      carry = (carry >> 31) + Nbr1[i] - Nbr2[i];
      Diff[i] = (int)(carry & MaxUInt);
    }
  }
  
  public static void SubtractBigNbr32(long Nbr1[], long Nbr2[], long Diff[], int NumberLength){
        long carry = 0;
        for (int i = 0; i < NumberLength; i++)
        {
          carry = (carry >> 32) + Nbr1[i] - Nbr2[i];
          Diff[i] = carry & 0xFFFFFFFFL;
        }
    }
  
}
