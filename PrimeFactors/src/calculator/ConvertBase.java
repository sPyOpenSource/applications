/*
 * EDU.ksu.cis.calculator.defaultmodel.ConvertBase.java    3/20/2001
 *
 * Copyright (c) 2001 Rod Howell, All Rights Reserved.
 */

package calculator;

/**
 * The convert base operation.  Objects of this class are used to
 * encapsulate a base change request.
 *
 * @author Rod Howell
 *         (<a href="mailto:howell@cis.ksu.edu">howell@cis.ksu.edu</a>)
 */
public class ConvertBase implements Operation {

  /**
   * The new base.
   */
  private int base;

  /**
   * Constructs a new ConvertBase.
   * @param  b  The new base.
   * @throws IllegalArgumentException  If b &lt; 2 or b &gt; 36.
   */
  public ConvertBase(int b) {
    if (b < 2 || b > 36) throw new IllegalArgumentException();
    base = b;
  }

  /**
   * Returns 0.
   */
  public int numOperands() {
    return 0;
  }

  /**
   * Returns the new base.
   */
  public int getBase() {
    return base;
  }
}
