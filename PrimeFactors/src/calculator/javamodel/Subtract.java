/*
 * EDU.ksu.cis.calculator.javamodel.Subtract.java    3/20/01
 *
 * Copyright (c) 2001 Rod Howell, All Rights Reserved.
 */

package calculator.javamodel;

import java.math.BigInteger;
import calculator.EncodedOperation;

/**
 * The subtract operation.
 *
 * @author Rod Howell
 *         (<a href="mailto:howell@cis.ksu.edu">howell@cis.ksu.edu</a>)
 */
public class Subtract implements EncodedOperation {

  /**
   * Returns 2.
   */
  public int numOperands() {
    return 2;
  }

  /**
   * Returns the result of subtracting operand[1] from operand[0].
   * @throws ArrayIndexOutOfBoundsException  If <tt>operands</tt> is not of 
   *                                         length at least 2.
   * @throws ClassCastException  If the elements of <tt>operands</tt> are not 
   *                             both <tt>BigInteger</tt>s.
   */
  public Object doOperation(Object[] operands) 
    throws ArrayIndexOutOfBoundsException, ClassCastException {
    return ((BigInteger) operands[0]).subtract((BigInteger) operands[1]);
  }
}
