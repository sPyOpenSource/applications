/*
 * EDU.ksu.cis.calculator.javamodel.Pop.java    3/20/01
 *
 * Copyright (c) 2001 Rod Howell, All Rights Reserved.
 */

package calculator.javamodel;

import calculator.Operation;

/**
 * The pop operation.  Objects of this class are used to
 * encapsulate a pop request.
 *
 * @author Rod Howell
 *         (<a href="mailto:howell@cis.ksu.edu">howell@cis.ksu.edu</a>)
 */
public class Pop implements Operation {

  /**
   * @return 1.
   */
  @Override
  public int numOperands() {
    return 1;
  }
  
}
