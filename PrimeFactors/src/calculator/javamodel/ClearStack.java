/*
 * EDU.ksu.cis.calculator.javamodel.ClearStack.java    3/20/01
 *
 * Copyright (c) 2001 Rod Howell, All Rights Reserved.
 */

package calculator.javamodel;

import calculator.Operation;

/**
 * The clear stack operation.  Objects of this class are used to
 * encapsulate a clear stack request.
 *
 * @author Rod Howell
 *         (<a href="mailto:howell@cis.ksu.edu">howell@cis.ksu.edu</a>)
 */
public class ClearStack implements Operation {

  /**
   * @return 0.
   */
  @Override
  public int numOperands() {
    return 0;
  }
  
}
