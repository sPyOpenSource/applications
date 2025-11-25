/*
 * EDU.ksu.cis.calculator.Calculator.java    3/18/2001
 *
 * Copyright (c) 2001 Rod Howell, All Rights Reserved.
 */

package calculator;

/**
 * An interface abstracting a calculator.
 *
 * @author Rod Howell
 *         (<a href="mailto:howell@cis.ksu.edu">howell@cis.ksu.edu</a>)
 */
public interface Calculator {

  /**
   * Performs operation <var>op</var>, using <var>input</var> as a possible
   * source of operand(s).
   * @throws   Exception  May throw any Exception.
   */
  public void doOperation(Operation op, String input) throws Exception;

  /**
   * This method may be implemented to change any settings of the calculator.
   * @param change   Encodes the change to the settings.
   * @param input    May contain input affected by the change.
   * @throws    Exception  May throw any Exception.
   */
  public void changeSettings(Object change, String input) throws Exception;

}
