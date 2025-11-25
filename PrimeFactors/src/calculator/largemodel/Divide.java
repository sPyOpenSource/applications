/*
 * EDU.ksu.cis.calculator.defaultmodel.Divide.java    3/20/01
 *
 * Copyright (c) 2001 Rod Howell, All Rights Reserved.
 */

package calculator.largemodel;

import BigIntegers.LargeInteger;
import calculator.EncodedOperation;
import static calculator.largemodel.ChangeSigns.ChSignBigNbr;

/**
 * The divide operation.
 *
 * @author Rod Howell
 *         (<a href="mailto:howell@cis.ksu.edu">howell@cis.ksu.edu</a>)
 */
public class Divide implements EncodedOperation {

  /**
   * @return 2.
   */
  @Override
  public int numOperands() {
    return 2;
  }

  /**
   * Returns the result of dividing operand[0] by operand[1].
   * @throws ArrayIndexOutOfBoundsException  If <tt>operands</tt> is not of 
   *                                         length at least 2.
   * @throws ClassCastException  If the elements of <tt>operands</tt> are not 
   *                             both <tt>LargeInteger</tt>s.
   * @throws ArithmeticException If operand[1] is zero.
   */
  @Override
  public Object doOperation(Object[] operands) 
    throws ArrayIndexOutOfBoundsException, ClassCastException, 
       ArithmeticException {
    return ((LargeInteger) operands[0]).divide((LargeInteger) operands[1])[0];
  }
  
  public static void DivBigNbrByLong(int Dividend[], long Divisor, int Quotient[], int NumberLength)
    {
      int i;
      boolean ChSignDivisor = false;
      long Divid, Rem = 0;

      if (Divisor < 0)
      {                            // If divisor is negative...
        ChSignDivisor = true;      // Indicate to change sign at the end and
        Divisor = -Divisor;        // convert divisor to positive.
      }
      if ((Dividend[i = NumberLength - 1] & 0x40000000) != 0)
      {                            // If dividend is negative...
        Rem = Divisor - 1;
      }
      for ( ; i >= 0; i--)
      {
        Divid = Dividend[i] + (Rem << 31);
        Rem = Divid - (Quotient[i] = (int)(Divid / Divisor)) * Divisor;
      }
      if (ChSignDivisor)
      {                            // Change sign if divisor is negative.
                                   // Convert divisor to positive.
        ChSignBigNbr(Quotient, NumberLength);
      }
    }
}
