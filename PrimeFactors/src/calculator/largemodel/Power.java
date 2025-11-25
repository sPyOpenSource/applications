/*
 * EDU.ksu.cis.calculator.defaultmodel.Power.java    3/20/01
 *
 * Copyright (c) 2001 Rod Howell, All Rights Reserved.
 */

package calculator.largemodel;

import BigIntegers.LargeInteger;
import calculator.EncodedOperation;

/**
 * The general exponentiation operation.
 *
 * @author Rod Howell
 *         (<a href="mailto:howell@cis.ksu.edu">howell@cis.ksu.edu</a>)
 */
public class Power implements EncodedOperation {

  /**
   * @return 2.
   */
  @Override
  public int numOperands() {
    return 2;
  }

  /**
   * Returns the result of raising operand[0] to the power of operand[1].
   * @throws ArrayIndexOutOfBoundsException  If <tt>operands</tt> is not of 
   *                                         length at least 2.
   * @throws ClassCastException  If the elements of <tt>operands</tt> are not 
   *                             both <tt>LargeInteger</tt>s.
   * @throws ArithmeticException If operand[1] is negative.
   */
  @Override
  public Object doOperation(Object[] operands) 
    throws ArrayIndexOutOfBoundsException, ClassCastException, 
       ArithmeticException {
    return ((LargeInteger) operands[0]).pow((LargeInteger) operands[1]);
  }
}
