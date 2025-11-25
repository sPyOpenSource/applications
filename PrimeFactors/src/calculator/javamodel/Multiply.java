/*
 * EDU.ksu.cis.calculator.javamodel.Multiply.java    3/20/01
 *
 * Copyright (c) 2001 Rod Howell, All Rights Reserved.
 */

package calculator.javamodel;

import java.math.BigInteger;
import calculator.EncodedOperation;

/**
 * The multiply operation.
 *
 * @author Rod Howell
 *         (<a href="mailto:howell@cis.ksu.edu">howell@cis.ksu.edu</a>)
 */
public class Multiply implements EncodedOperation {

  /**
   * Returns 2.
   */
  public int numOperands() {
    return 2;
  }

  /**
   * Returns the result of multiplyinging the given operands.
   * @throws ArrayIndexOutOfBoundsException  If <tt>operands</tt> is not of 
   *                                         length at least 2.
   * @throws ClassCastException  If the elements of <tt>operands</tt> are not 
   *                             both <tt>BigInteger</tt>s.
   */
  public Object doOperation(Object[] operands) 
    throws ArrayIndexOutOfBoundsException, ClassCastException {
    return ((BigInteger) operands[0]).multiply((BigInteger) operands[1]);
  }
}
