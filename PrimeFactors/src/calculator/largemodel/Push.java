/*
 * EDU.ksu.cis.calculator.defaultmodel.Push.java    3/20/01
 *
 * Copyright (c) 2001 Rod Howell, All Rights Reserved.
 */

package calculator.largemodel;

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
   * @return 1.
   */
  @Override
  public int numOperands() {
    return 1;
  }
}
