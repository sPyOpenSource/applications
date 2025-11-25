/*
 * EDU.ksu.cis.calculator.defaultmodel.TwoToPower.java    3/20/01
 *
 * Copyright (c) 2001 Rod Howell, All Rights Reserved.
 */

package calculator.largemodel;

import BigIntegers.LargeInteger;
import calculator.EncodedOperation;

/**
 * The operation of raising 2 to an arbitrary power.
 *
 * @author Rod Howell
 *         (<a href="mailto:howell@cis.ksu.edu">howell@cis.ksu.edu</a>)
 */
public class TwoToPower implements EncodedOperation {

  /**
   * @return 1.
   */
  @Override
  public int numOperands() {
    return 1;
  }

  /**
   * Returns the result of raising 2 to the power of <tt>operands[0]</tt>.
   * @throws ArrayIndexOutOfBoundsException  If <tt>operands</tt> is not of 
   *                                         length at least 1.
   * @throws ClassCastException  If <tt>operands[0]</tt> is not 
   *                             a <tt>LargeInteger</tt>.
   * @throws ArithmeticException If <tt>operands[0]</tt> is negative.
   */
  @Override
  public Object doOperation(Object[] operands) 
    throws ArrayIndexOutOfBoundsException, ClassCastException,
       ArithmeticException {
    return LargeInteger.pow(2, (LargeInteger) operands[0]);
  }
}
