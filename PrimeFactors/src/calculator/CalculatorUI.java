/*
 * EDU.ksu.cis.calculator.CalculatorUI.java    3/17/2001
 *
 * Copyright (c) 2001 Rod Howell, All Rights Reserved.
 */

package calculator;

/**
 * An interface abstracting user interface for a calculator.
 *
 * @author Rod Howell
 *         (<a href="mailto:howell@cis.ksu.edu">howell@cis.ksu.edu</a>)
 */
public interface CalculatorUI {

  /**
   * Returns the current contents of the input buffer.
   */
  public String getInput();

  /**
   * Inserts the given String into the input buffer at a location
   * determined by the implementation.
   */
  public void insert(String s);

  /**
   * Displays the given String and radix.
   */
  public void setOutput(String s, int b);

  /**
   * Notifies the user interface whether work is being done.
   */
  public void setBusy(boolean b);

  /**
   * Displays an error message describing the given Throwable.
   */
  public void showError(Throwable e);
}
