/*
 * EDU.ksu.cis.calculator.EncodedOperation.java    3/20/2001
 *
 * Copyright (c) 2001 Rod Howell, All Rights Reserved.
 */

package calculator;

/**
 * An interface abstracting any operation explicitly encoded within the
 * implementing class.  Arithmetic operations should be encoded in this
 * fashion so as to decouple the encoding of the operation from the control
 * of evaluation order.
 */
public interface EncodedOperation extends Operation {

  /**
   * Performs the operation.
   * @param  operands   The operands for the operation.  The length of
   *                    this array should match the value returned by
   *                    {@link Operation#numOperands()}.
   * @return The result of the operation.
   * @throws ArrayIndexOutOfBoundsException  If <var>operands</var> has an
   *                                         incorrect length.
   * @throws ClassCastException        If <var>operands</var> contains an
   *                                   element of an unexptected type.
   * @throws Exception                 The operation may throw any Exception.
   */
  public Object doOperation(Object[] operands)
    throws IllegalArgumentException, ClassCastException, Exception;
}
