/*
 * EDU.ksu.cis.calculator.javamodel.Push.java    3/20/2001
 *
 * Copyright (c) 2001 Rod Howell, All Rights Reserved.
 */

package calculator.javamodel;

import java.math.BigInteger;
import calculator.Operation;

/**
 * The push operation.  Objects of this class are used to
 * encapsulate a push request.
 *
 * @author Rod Howell
 *         (<a href="mailto:howell@cis.ksu.edu">howell@cis.ksu.edu</a>)
 */
public class Push implements Operation {

  /**
   * Returns 1.
   */
  public int numOperands() {
    return 1;
  }
}
