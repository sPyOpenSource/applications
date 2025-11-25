/*
 * EDU.ksu.cis.calculator.Operation.java    3/20/01
 *
 * Copyright (c) 2001 Rod Howell, All Rights Reserved.
 */

package calculator;

/**
 * An interface abstracting any operation.  The operation may be
 * explicitly encoded in an implementing subclass, or may defer the
 * operation to some method receiving the Operation as a parameter.
 * Arithmetic operations should be encoded explicitly; in this case,
 * subclasses should implement {@link EncodedOperation}.  In this way,
 * the encoding of an arithmetic operation can be decoupled from the
 * control of evaluation order.
 *
 * @author Rod Howell
 *         (<a href="mailto:howell@cis.ksu.edu">howell@cis.ksu.edu</a>)
 */
public interface Operation {

  /**
   * @return the number of operands required by this Operation.
   */
  public int numOperands();
}
