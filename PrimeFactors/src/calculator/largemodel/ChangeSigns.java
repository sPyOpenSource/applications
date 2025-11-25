/*
 * EDU.ksu.cis.calculator.defaultmodel.ChangeSigns.java    3/20/01
 *
 * Copyright (c) 2001 Rod Howell, All Rights Reserved.
 */

package calculator.largemodel;

import static BigIntegers.FactorInteger.MaxUInt;
import BigIntegers.LargeInteger;
import calculator.EncodedOperation;

/**
 * The change signs operation.
 *
 * @author Rod Howell
 *         (<a href="mailto:howell@cis.ksu.edu">howell@cis.ksu.edu</a>)
 */
public class ChangeSigns implements EncodedOperation {

  /**
   * @return 1.
   */
  @Override
  public int numOperands() {
    return 1;
  }

  /**
   * Returns negation of <tt>operands[0]</tt>.
   * @throws ArrayIndexOutOfBoundsException  If <tt>operands</tt> is not of 
   *                                         length at least 1.
   * @throws ClassCastException  If <tt>operands[0]</tt> is not 
   *                             a <tt>LargeInteger</tt>.
   */
  @Override
  public Object doOperation(Object[] operands) 
    throws ArrayIndexOutOfBoundsException, ClassCastException {
    return ((LargeInteger) operands[0]).negate();
  }
  
  public static void ChSignBigNbr(int[] TestNbr, int NumberLength){
        int carry = 0;
        for (int i = 0; i < NumberLength; i++){
            carry = (carry >> 31) - TestNbr[i];
            TestNbr[i] = (int)(carry & MaxUInt);
        }
    }
}
