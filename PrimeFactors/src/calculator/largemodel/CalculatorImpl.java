/*
 * EDU.ksu.cis.calculator.defaultmodel.CalculatorImpl.java    3/20/01
 *
 * Copyright (c) 2001 Rod Howell, All Rights Reserved.
 */
package calculator.largemodel;

import calculator.Calculator;
import calculator.CalculatorUI;
import calculator.ConvertBase;
import calculator.EmptyDequeException;
import calculator.EncodedOperation;
import BigIntegers.LargeInteger;
import calculator.Operation;
import calculator.Deque;
import calculator.javamodel.ClearStack;
import calculator.javamodel.Pop;
import calculator.javamodel.RotateDown;
import calculator.javamodel.RotateUp;

/**
 * A model for an RPN calculator using the
 * {@link EDU.ksu.cis.calculator.LargeInteger} class.
 *
 * @author Rod Howell
 *         (<a href="mailto:howell@cis.ksu.edu">howell@cis.ksu.edu</a>)
 */
public class CalculatorImpl implements Calculator {

  /**
   * The user interface.
   */
  private CalculatorUI theView;

  /**
   * The computation stack.
   */
  private Deque theStack = new Deque();

  /**
   * The current base.
   */
  private int base;

  /**
   * true iff bases are sticky.
   */
  private boolean baseIsSticky;

  /**
   * Constructs a new CalculatorImpl using the given user interface, base 10,
   * and sticky bases.
   */
  public CalculatorImpl(CalculatorUI v) {
    this(v, 10, true);
  }

  /**
   * Constructs a new CalculatorImpl using the given user interface and base,
   * with sticky bases.
   */
  public CalculatorImpl(CalculatorUI v, int b) {
    this(v, b, true);
  }

  /**
   * Constructs a new CalculatorImpl using the given user interface and
   * the given setting for sticky bases, in base 10.
   */
  public CalculatorImpl(CalculatorUI v, boolean s) {
    this(v, 10, s);
  }

  /**
   * Constructs a new CalculatorImpl using the given user interface, base, and
   * setting for sticky bases.
   */
  public CalculatorImpl(CalculatorUI v, int b, boolean s) {
    theView = v;
    base = b;
    baseIsSticky = s;
  }

  /**
   * Performs the given operation.  If <var>input</var> is non-null, it
   * is treated as the top of the stack.  If <var>input</var> is "" or "-", it is
   * interpreted as "0".  Operands are taken from the top
   * of the stack so that the top element is the last operand.  The result
   * (if any) is pushed onto the stack.  When the operation is complete,
   * the element that then resides at the top of the stack (if any) is
   * reported to the user interface.  If an exception or error is thrown,
   * the stack will be restored to its original contents, if possible.
   * <p>
   * The following classes of operations are supported:
   * <ul>
   * <li> {@link Push} - If <var>input</var> is null, a copy of the top of the 
   *      stack is pushed.  Otherwise, <var>input</var> is simply accepted as
   *      the new top of the stack.  Unrecognized subclasses of
   *      {@link EDU.ksu.cis.calculator.Operation} are treated as Push.
   * <li> {@link Pop} - Removes the top from the stack.
   * <li> {@link ClearStack} - Empties the stack.
   * <li> {@link RotateUp} - Moves the element at the bottom of the stack to 
   *      the top.
   * <li> {@link RotateDown} - Moves the element at the top of the stack to the
   *      bottom.
   * <li> {@link EDU.ksu.cis.calculator.ConvertBase} - Converts the top of 
   *      the stack to the base specified by 
   *      {@link EDU.ksu.cis.calculator.ConvertBase#getBase() op.getBase()}.
   *      If the stack is empty, simply reports the new base to the user
   *      interface.
   * <li> {@link EDU.ksu.cis.calculator.EncodedOperation} - Performs the 
   *      operation specified by the given subclass of EncodedOperation.
   * </ul>
   * @throws EmptyDequeException  If the operation requires more operands
   *                              than are present on the stack.
   * @throws NumberFormatException  If <var>input</var> contains an invalid
   *                                character for the current base.
   * @throws Exception            If the given operation throws an
   *                              exception.
   */
  @Override
  public void doOperation(Operation op, String input) 
    throws EmptyDequeException, NumberFormatException, Exception {
    if (op instanceof ClearStack) {  // Don't need input.
      theStack = new Deque();
      theView.setOutput("", base);
    } else {
      // If input is present, push it onto the stack.
      if (input != null)
    theStack.addToFront(input.length() == 0 || input.equals("-") ? 
                new LargeInteger("0", base) :
                new LargeInteger(input, base));
      // Make sure enough operands are available.
      if (theStack.getSize() < op.numOperands()) { 
        theView.setOutput(theStack.isEmpty() ? "" : 
                  String.valueOf(theStack.getFront()),
                  base);
        throw new EmptyDequeException();
      }
      if (op instanceof ConvertBase) changeBase(((ConvertBase) op).getBase());
      else if (op instanceof EncodedOperation) 
        doEncodedOperation((EncodedOperation) op);
      else doStackOperation(op, input == null);
    }
  }

  /**
   * Changes the current base to b, performs any base conversion necessary to
   * reflect this change, and reports the result to the user interface.
   */
  private void changeBase(int b) {
    base = b;
    try {
      theStack.addToFront(((LargeInteger) 
               theStack.removeFromFront()).toString(base));
      theView.setOutput(String.valueOf(theStack.getFront()), base);
    }
    catch (EmptyDequeException e) {  // Just report the new base.
      theView.setOutput("", base);
    }
  }

  /**
   * Performs the given EncodedOperation using the value(s) at the top
   * of the stack for operand(s), and reports the results to the user
   * interface.  Assumes sufficiently many values
   * exist on the stack.  If an Exception or Error is thrown, an
   * attempt will be made to restore the stack to its original contents.
   * @throws Exception  If the given operation throws an Exception.
   */
  private void doEncodedOperation(EncodedOperation op) 
    throws Exception {
    int numOps = op.numOperands();
    LargeInteger[] ops = new LargeInteger[numOps];
    for (int i = numOps; i > 0; ) {
      ops[--i] = (LargeInteger) theStack.removeFromFront();
    }
    try {
      theStack.addToFront(op.doOperation(ops));
      theView.setOutput(String.valueOf(theStack.getFront()), base);
    }
    // Try to restore the stack after any Exception or Error.
    catch (Exception | Error e) {
      for (int i = 0; i < numOps; i++) theStack.addToFront(ops[i]);
      theView.setOutput(String.valueOf(theStack.getFront()), base);
      throw e;
    }
  }

  /**
   * Performs the given stack operation ({@link Push}, {@link Pop},
   * {@link RotateUp}, or {@link RotateDown}) and reports the results to
   * user interface.  Other subclasses of {@link Operation} are treated
   * as Push.
   * @param  op               The operation to perform.
   * @param  noNewInput  true iff no new input has been received.
   * @throws EmptyDequeException  If the stack is empty.
   */
  private void doStackOperation(Operation op, boolean noNewInput) 
    throws EmptyDequeException {
    if (op instanceof Pop) theStack.removeFromFront();
    else if (op instanceof RotateUp) 
      theStack.addToFront(theStack.removeFromBack());
    else if (op instanceof RotateDown)
      theStack.addToBack(theStack.removeFromFront());
    else if (noNewInput)   // Treat anything else as Push
      theStack.addToFront(theStack.getFront());
    if (theStack.isEmpty()) theView.setOutput("", base);
    else {
      // Make sure base is the same as the base of the top element.
      if (baseIsSticky)
    theStack.addToFront(((LargeInteger) 
                 theStack.removeFromFront()).toString(base));
      else base = ((LargeInteger) theStack.getFront()).getBase();
      theView.setOutput(String.valueOf(theStack.getFront()), base);
    }
  }

  /**
   * Changes the settings of the calculator.  The only setting that can
   * be changed in this model is sticky bases.  If <var>change</var> is of
   * type {@link java.lang.Boolean}, then bases are set to sticky iff the
   * value of <var>change</var> is true.  If <var>change</var> is of a different 
   * type, this method has no effect.  The second parameter is ignored.
   */
  @Override
  public void changeSettings(Object change, String input) {
    if (change instanceof Boolean) {
      baseIsSticky = ((Boolean) change);
    }
  }
}
