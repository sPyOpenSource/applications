/*
 * EDU.ksu.cis.calculator.javamodel.Remainder.java    3/20/01
 *
 * Copyright (c) 2001 Rod Howell, All Rights Reserved.
 */

package calculator.javamodel;

import java.math.BigInteger;
import calculator.EncodedOperation;

/**
 * The remainder operation.
 *
 * @author Rod Howell
 *         (<a href="mailto:howell@cis.ksu.edu">howell@cis.ksu.edu</a>)
 */
public class Remainder implements EncodedOperation {

  /**
   * Returns 2.
   */
  public int numOperands() {
    return 2;
  }

  /**
   * Returns the remainder obtained when dividing operand[0] by operand[1].
   * @throws ArrayIndexOutOfBoundsException  If <tt>operands</tt> is not of 
   *                                         length at least 2.
   * @throws ClassCastException  If the elements of <tt>operands</tt> are not 
   *                             both <tt>BigInteger</tt>s.
   * @throws ArithmeticException If operand[1] is zero.
   */
  public Object doOperation(Object[] operands) 
    throws ArrayIndexOutOfBoundsException, ClassCastException, 
       ArithmeticException {
    return ((BigInteger) operands[0]).remainder((BigInteger) operands[1]);
  }
}
