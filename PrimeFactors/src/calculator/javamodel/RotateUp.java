/*
 * EDU.ksu.cis.calculator.javamodel.RotateUp.java    3/20/01
 *
 * Copyright (c) 2001 Rod Howell, All Rights Reserved.
 */

package calculator.javamodel;

import java.math.BigInteger;
import calculator.Operation;

/**
 * The rotate up operation.  Objects of this class are used to
 * encapsulate a rotate up request.
 *
 * @author Rod Howell
 *         (<a href="mailto:howell@cis.ksu.edu">howell@cis.ksu.edu</a>)
 */
public class RotateUp implements Operation {

  /**
   * Returns 1.
   */
  public int numOperands() {
    return 1;
  }
}
