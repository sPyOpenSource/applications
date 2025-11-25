/*
 * EDU.ksu.cis.calculator.javamodel.Add.java    3/20/01
 *
 * Copyright (c) 2001 Rod Howell, All Rights Reserved.
 */

package calculator.javamodel;

import java.math.BigInteger;
import calculator.EncodedOperation;

/**
 * The add operation.
 *
 * @author Rod Howell
 *         (<a href="mailto:howell@cis.ksu.edu">howell@cis.ksu.edu</a>)
 */
public class Add implements EncodedOperation {

  /**
   * Returns 2.
   */
  @Override
  public int numOperands() {
    return 2;
  }

  /**
   * Returns the result of adding the given operands.
   * @throws ArrayIndexOutOfBoundsException  If <var>operands</var> is not of 
   *                                         length at least 2.
   * @throws ClassCastException  If the elements of <var>operands</var> are not 
   *                             both <tt>BigInteger</tt>s.
   */
  @Override
  public Object doOperation(Object[] operands) 
    throws ArrayIndexOutOfBoundsException, ClassCastException {
    return ((BigInteger) operands[0]).add((BigInteger) operands[1]);
  }
}
