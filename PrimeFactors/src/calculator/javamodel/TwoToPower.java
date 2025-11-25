/*
 * EDU.ksu.cis.calculator.javamodel.TwoToPower.java    3/20/01
 *
 * Copyright (c) 2001 Rod Howell, All Rights Reserved.
 */

package calculator.javamodel;

import java.math.BigInteger;
import calculator.EncodedOperation;

/**
 * The operation of raising 2 to an arbitrary power.
 *
 * @author Rod Howell
 *         (<a href="mailto:howell@cis.ksu.edu">howell@cis.ksu.edu</a>)
 */
public class TwoToPower implements EncodedOperation {

  /**
   * Returns 1.
   */
  public int numOperands() {
    return 1;
  }

  /**
   * Returns the result of raising 2 to the power of <tt>operands[0]</tt>.
   * @throws ArrayIndexOutOfBoundsException  If <tt>operands</tt> is not of 
   *                                         length at least 1.
   * @throws ClassCastException  If <tt>operands[0]</tt> is not 
   *                             a <tt>BigInteger</tt>.
   * @throws ArithmeticException If <tt>operands[0]</tt> is negative.
   */
  public Object doOperation(Object[] operands) 
    throws ArrayIndexOutOfBoundsException, ClassCastException,
       ArithmeticException {
    if (((BigInteger)operands[0]).compareTo(BigInteger.valueOf(0)) < 0)
      throw new ArithmeticException();
    return 
      BigInteger.valueOf(1L).shiftLeft(((BigInteger) operands[0]).intValue());
  }
}
