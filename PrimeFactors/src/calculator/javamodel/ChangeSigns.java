/*
 * EDU.ksu.cis.calculator.javamodel.ChangeSigns.java    3/20/01
 *
 * Copyright (c) 2001 Rod Howell, All Rights Reserved.
 */

package calculator.javamodel;

import java.math.BigInteger;
import calculator.EncodedOperation;

/**
 * The change signs operation.
 *
 * @author Rod Howell
 *         (<a href="mailto:howell@cis.ksu.edu">howell@cis.ksu.edu</a>)
 */
public class ChangeSigns implements EncodedOperation {

  /**
   * Returns 1.
   */
  public int numOperands() {
    return 1;
  }

  /**
   * Returns negation of <tt>operands[0]</tt>.
   * @throws ArrayIndexOutOfBoundsException  If <tt>operands</tt> is not of 
   *                                         length at least 1.
   * @throws ClassCastException  If <tt>operands[0]</tt> is not 
   *                             a <tt>BigInteger</tt>.
   */
  public Object doOperation(Object[] operands) 
    throws ArrayIndexOutOfBoundsException, ClassCastException {
    return ((BigInteger) operands[0]).negate();
  }
}
