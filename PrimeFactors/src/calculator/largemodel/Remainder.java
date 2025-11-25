/*
 * EDU.ksu.cis.calculator.defaultmodel.Remainder.java    3/20/01
 *
 * Copyright (c) 2001 Rod Howell, All Rights Reserved.
 */

package calculator.largemodel;

import BigIntegers.LargeInteger;
import calculator.EncodedOperation;

/**
 * The remainder operation.
 *
 * @author Rod Howell
 *         (<a href="mailto:howell@cis.ksu.edu">howell@cis.ksu.edu</a>)
 */
public class Remainder implements EncodedOperation {

  /**
   * @return 2.
   */
  @Override
  public int numOperands() {
    return 2;
  }

  /**
   * Returns the remainder obtained when dividing operand[0] by operand[1].
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
    return ((LargeInteger) operands[0]).divide((LargeInteger) operands[1])[1];
  }
  
  public static long RemDivBigNbrByLong(int Dividend[], long Divisor, int NumberLength)
    {
      int i;
      long Rem = 0;
      long Mod2_31;
      int divis = (int)(Divisor < 0?-Divisor:Divisor);
      if (Divisor < 0)
      {                            // If divisor is negative...
        Divisor = -Divisor;        // Convert divisor to positive.
      }
      Mod2_31 = ((-2147483648) - divis) % divis;  // 2^31 mod divis.
      if (Dividend[i = NumberLength - 1] >= 0x40000000)
      {                            // If dividend is negative...
        Rem = Divisor - 1;
      }
      for ( ; i >= 0; i--)
      {
        Rem = Rem * Mod2_31 + Dividend[i];
        do
        {
          Rem = (Rem >> 31) * Mod2_31 + (Rem & 0x7FFFFFFF);
        } while (Rem > 0x1FFFFFFFFL);
      }
      return Rem % divis;
    }
}
