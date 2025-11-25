/*
 * EDU.ksu.cis.calculator.LargeInteger.java    3/20/01
 *
 * Copyright (c) 2001 Rod Howell, All Rights Reserved.
 */
package BigIntegers;

import static BigIntegers.StaticFunctions.BigIntToBigNbr;
import java.math.BigInteger;
import utilities.BitReverseCounter;
import utilities.FFT;
import static utilities.FFT.roots;
import utilities.NTT;

/**
 * A class which implements aribitrary-precision integer arithmetic.
 * <var>LargeIntegers</var> are stored in any radix within the range
 * 2-36, and radix conversions are done only when necessary; i.e.,
 * computations involving <var>LargeInteger</var>s of the same radix are
 * done within that radix. Thus, if inputs and outputs are all to be
 * in decimal, all of the computations are done in decimal, thereby
 * saving expensive radix conversions.
 *
 * @author Rod Howell
 *         (<a href="mailto:howell@cis.ksu.edu">howell@cis.ksu.edu</a>)
 */
public class LargeInteger extends BigInteger 
{
  /**
   * The radix positions of the number.
   * The low-order byte is <var>digits[offset]</var>, and the high-order byte is
   * <var>digits[offset + length - 1]</var>.
   */
  int[] digits;
  static byte[] temp;
  
  /**
   * The index of the low-order byte of the number.
   */
  private int offset;

  /**
   * The number of bytes comprising the number.
   */
  int length;

  /**
   * <tt>true</tt> if the number is negative.
   */
  private int negative;

  /**
   * The base to use when converting to a string.
   * 1 &lt; <tt>displayBase</tt> &lt; 37
   */
  int displayBase;

  /**
   * The base in which the number is stored.  This value is the largest
   * power of <tt>displayBase</tt> less than 257.
   * 16 &lt; <tt>base</tt> &lt; 257
   */
  int base = 0x10000;

  /**
   * Constructs a new LargeInteger in base 10 from the given String.
   * @throws NumberFormatException if <var>s</var> does not encode a 
   *                               decimal integer.
   */
  public LargeInteger(String s) throws NumberFormatException {
    this(s, 10);
  }

  /**
   * Constructs a new LargeInteger in the given base from the given String.
   * @throws NumberFormatException if <var>s</var> does not encode an
   *                               integer in the given base, or if the
   *                               given base is less than 2 or greater than
   *                               36.
   */
  public LargeInteger(String s, int b) throws NumberFormatException {
    super(s, b);
    if (b < 2 || b > 36) throw new NumberFormatException();
    displayBase = b;
    negative = super.signum();
    temp = super.abs().toByteArray();
    length = temp.length / 2 + 1;
    digits = new int[length];
    int j = 0;
    short mask = 1;
    int p = 0;
    for (int i = temp.length - 1; i >= 0; i--){
        p += mask * (int) (temp[i] >= 0 ? temp[i] : temp[i] + 256);
        mask <<= 8;
        if (mask == 0){                        // Overflow
            digits[j++] = p;
            mask = 1;
            p = 0;
        }
    }
    digits[j] = p;
    length = getEnd(digits);
  }

  /**
   * Constructs a new LargeInteger directly from the given parameters.
   * The array is not copied, and no error-checking is done.  If 
   * <tt>negative</tt> is true and the value encoded is zero, it is
   * made nonnegative.
   */
  private LargeInteger(int[] digits, int offset, int length, 
               int negative, int displayBase) {
    super(negative, temp);
    this.digits = digits;
    this.offset = offset;
    this.length = length;
    this.negative = negative;
    this.displayBase = displayBase;
  }

  /**
   * @return the sum of this LargeInteger and the given LargeInteger.
   */
  public LargeInteger add(LargeInteger other) {
    if (negative == -1) return negate().subtract(other).negate();
    if (other.negative == -1) return subtract(other.negate());
    if (length < other.length) return other.add(this);
    if (length == 0x7fffffff) throw new OutOfMemoryError();
    int[] sum = new int[length + 1];
    int carry = 0;
    int i = offset;
    int j = other.offset;
    int k = 0;
    for ( ; k < other.length; i++, j++, k++) 
      carry = addInts(digits[i], other.digits[j], carry, base, sum, k);
    for ( ; k < length; i++, k++)
      carry = addInts(digits[i], 0, carry, base, sum, k);
    if (carry > 0) {
      sum[length] = carry;
      return new LargeInteger(sum, 0, sum.length, 1, displayBase);
    }
    return new LargeInteger(sum, 0, length, 1, displayBase);
  }

  /**
   * Places <tt>(a + b + c) mod m</tt> in <tt>sum[i]</tt>.
   * Both <tt>a</tt> and <tt>b</tt> must be less than <tt>m</tt>, and
   * <tt>c</tt> must be either 0 or 1.  No error checking is performed.
   * @return <tt>(a + b + c) / m</tt>.
   */
  private static int addInts(int a, int b, int c, int m, int[] sum, int i) {
    long result = ((long) a & 0xffff) + ((long) b & 0xffff) + c;
    if (result >= m) {
      sum[i] = (int) (result - m);
      return 1;
    } else {
      sum[i] = (int) result;
      return 0;
    }
  }

  /** 
   * @return the difference of this LargeInteger and the given LargeInteger.
   */
  public LargeInteger subtract(LargeInteger other) {
    if (negative < 0) return negate().add(other).negate();
    if (other.negative < 0) return add(other.negate());
    int comp = compareTo(other);
    if (comp == 0) return new LargeInteger("0", displayBase);
    if (comp < 0) return other.subtract(this).negate();
    int[] diff = new int[length];
    int borrow = 0;
    int i = offset;
    int j = other.offset;
    int k = 0;
    for ( ; k < other.length; i++, j++, k++){
      borrow = subtractInts(digits[i], other.digits[j], borrow, base, diff, k);
    }
    for ( ; k < length; i++, k++){
      borrow = subtractInts(digits[i], 0, borrow, base, diff, k);
    }
    return new LargeInteger(diff, 0, getEnd(diff), 1, displayBase);
  }

  /**
   * Places <tt>(a - b - c) mod m</tt> in <tt>diff[i]</tt>.
   * Both <tt>a</tt> and <tt>b</tt> must be less than <tt>m</tt>, and
   * <tt>c</tt> must be either 0 or 1.  No error checking is performed.
   * @return <tt>-((a - b - c) / m)</tt>.
   */
  private static byte subtractInts(int a, int b, int c, int m, int[] diff, int i) {
    long result = ((long) a & 0xffff) - ((long) b & 0xffff) - c;
    if (result < 0) {
      diff[i] = (int) (result + m);
      return 1;
    } else {
      diff[i] = (int) result;
      return 0;
    }
  }

  /**
   * Returns the LargeInteger obtained by changing the sign of this
   * LargeInteger.
   */
  @Override
  public LargeInteger negate() {
    return new LargeInteger(digits, offset, length, -1 * negative, displayBase);
  }
  
public LargeInteger multiply(LargeInteger other) {
    if (length >= 0x40000000 || other.length >= 0x40000000)
      throw new OutOfMemoryError();
    int len = length + other.length;
    if (len > 0x40000000) throw new OutOfMemoryError();

    // We must find the smallest power of 2 no smaller than <tt>len</tt>.
    int n = 1;
    int lgn = 0;
    while (n < len) {
      n = n << 1;
      lgn++;
    }

    // The FFT computation requires the bytes of the number to be permuted
    // in bit-reverse order.
    BitReverseCounter counter = new BitReverseCounter(n >>> 1);

    long[] aPermute = new long[n];
    long[] bPermute = new long[n];

    for (int i = offset; i < offset + length; i++) {
      aPermute[counter.next()] = ((long) digits[i]) & 0xffff;
    }
    counter.reset();
    for (int i = other.offset; i < other.offset + other.length; i++) {
      bPermute[counter.next()] = ((long) other.digits[i]) & 0xffff;
    }

    FFT.transform(aPermute, roots);
    FFT.transform(bPermute, roots);
    // ------------------------------------------------------------------
    
    // The product of two FFTs is computed by pointwise multiplication.
    for (int i = 0; i < n; i++) {
      aPermute[i] = FFT.modmult(aPermute[i], bPermute[i], FFT.mod);
    }
    // ----------------------------------------------------------------
    
    // We now permute the FFT and compute the inverse FFT.
    counter.reset();
    for (int i = 0; i < n; i++) {
      bPermute[counter.next()] = aPermute[i];
    }
    FFT.inverseTransform(bPermute, lgn);
    // ----------------------------------------------------
    
    // Finally, separate out the bytes of the result.
    long accum = 0L;
    int[] result = new int[len];
    for (int i = 0; i < len; i++) {
      accum += bPermute[i];
      result[i] = (int) (accum % base);
      accum = accum / base;
    }
    return new LargeInteger(result, 0, getEnd(result), 
                negative * other.negative, displayBase);
  }

  /**
   * @return the product of this LargeInteger and the given LargeInteger.
   */
  public LargeInteger squareFFT() {
    if (length >= 0x40000000){
      throw new OutOfMemoryError();
    }
    int len = length * 2;
    if (len > 0x40000000) throw new OutOfMemoryError();

    // We must find the smallest power of 2 no smaller than <tt>len</tt>.
    int n = 1;
    int lgn = 0;
    while (n < len) {
      n = n << 1;
      lgn++;
    }
    
    // The FFT computation requires the bytes of the number to be permuted
    // in bit-reverse order.
    BitReverseCounter counter = new BitReverseCounter(n >>> 1);

    long[] aPermute = new long[n];
    long[] bPermute = new long[n];

    for (int i = offset; i < offset + length; i++) {
      aPermute[counter.next()] = ((long) digits[i]) & 0xffff;
    }

    FFT.transform(aPermute, roots);
    
    counter.reset();
    for (int i = 0; i < n; i++) {
      aPermute[i] = FFT.modmult(aPermute[i], aPermute[i], FFT.mod);
      bPermute[counter.next()] = aPermute[i];
    }
    
    FFT.inverseTransform(bPermute, lgn);

    // Finally, separate out the bytes of the result.
    long accum = 0L;
    int[] result = new int[len];
    for (int i = 0; i < len; i++) {
      accum += bPermute[i];
      result[i] = (int) (accum % base);
      accum = accum / base;
    }
    return new LargeInteger(result, 0, getEnd(result), 
                negative * negative, displayBase);
  }
  
  /**
   * @return the product of this LargeInteger and the given LargeInteger.
   */
  public LargeInteger squareNTT() {
    if (length >= 0x40000000){
      throw new OutOfMemoryError();
    }
    int len = length * 2;
    if (len > 0x40000000) throw new OutOfMemoryError();

    // We must find the smallest power of 2 no smaller than <tt>len</tt>.
    int n = 1;
    int lgn = 0;
    while (n < len) {
      n = n << 1;
      lgn++;
    }

    long[] aPermute = new long[n];

    for (int i = offset; i < offset + length; i++) {
      aPermute[i] = ((long) digits[i]) & 0xffff;
    }

    NTT.transformRadix2(aPermute, roots[lgn], FFT.mod);
    for (int i = 0; i < n; i++) {
      aPermute[i] = FFT.modmult(aPermute[i], aPermute[i], FFT.mod);
    }
    
    //aPermute = NTT.inverseTransform(aPermute, roots[lgn], mod);
    NTT.transformRadix2(aPermute, FFT.rootsInv[lgn], FFT.mod);
    for (int i = 0; i < n; i++){
        aPermute[i] = FFT.modmult(aPermute[i], FFT.lengthInv[lgn], FFT.mod);
    }
    
    // Finally, separate out the bytes of the result.
    long accum = 0L;
    int[] result = new int[len];
    for (int i = 0; i < len; i++) {
      accum += aPermute[i];
      result[i] = (int) (accum % base);
      accum = accum / base;
    }
    return new LargeInteger(result, 0, getEnd(result), 
                negative * negative, displayBase);
  }

  /**
   * @return the index following the last nonzero element in the given array.
   */
  public static int getEnd(int[] a) {
    int i = a.length - 1;
    while (i >= 0 && a[i] == 0) i--;
    return ++i;
  }

  /**
   * @return a 2-element array whose first element is <tt>this / other</tt> 
   * and whose second element is <tt>this % other</tt>.  Thus, if the two 
   * returned values are <tt>q</tt> and <tt>r</tt>, respectively, the following
   * hold:
   * <ul>
   * <li> <tt>q * other + r = this</tt>
   * <li> <tt>-other &lt; r &lt; other</tt>
   * <li> if <tt>this</tt> is nonnegative, then <tt>r</tt> is nonnegative
   * <li> if <tt>this</tt> is nonpositive, then <tt>r</tt> is nonpositive
   * </ul>
   * @throws ArithmeticException if <var>other</var> is 0.
   */
  public LargeInteger[] divide(LargeInteger other) throws ArithmeticException {
    if (negative < 0) {
      LargeInteger[] tmp = abs().divide(other);
      tmp[0] = tmp[0].negate();
      tmp[1] = tmp[1].negate();
      return tmp;
    }

    // precision is the number of bytes of accuracy needed in the 
    // reciprocal in order to obtain a result within 1 of the correct quotient.
    int precision = length - other.length + 1;
    LargeInteger[] result = new LargeInteger[2];
    if (precision <= 0) {
      result[0] = new LargeInteger("0", displayBase);
      result[1] = negative * other.negative < 0 ? negate() : this;
      return result;
    }
    LargeInteger absOther = other.abs();
    LargeInteger recip = absOther.getReciprocal(precision);
    result[0] = multiply(recip);
    result[0] = new LargeInteger(result[0].digits, 
                 result[0].offset + length + 1, 
                 result[0].length - length - 1,
                 result[0].negative, displayBase);

    // At this point, the result may be off by 1, so we must check it.
    LargeInteger prod = absOther.multiply(result[0]);
    result[1] = subtract(prod);
    if (result[1].compareTo(absOther) >= 0) {
      result[1] = result[1].subtract(absOther);
      result[0] = result[0].add(new LargeInteger("1", displayBase));
    }
    else if (result[1].negative < 0) {
      result[1] = result[1].add(absOther);
      result[0] = result[0].subtract(new LargeInteger("1", displayBase));
    }
    if (other.negative < 0) result[0] = result[0].negate();
    return result;
  }

  @Override
  public LargeInteger abs() {
    return new LargeInteger(digits, offset, length, 1, displayBase);
  }
    
  /**
   * Returns an approximation of this LargeInteger's reciprocal, scaled
   * to an integer.
   * @return <tt>(1/this) * base<sup>precision-length</sup> + u</tt>,
   *         where <tt>|u|</tt> is no more than <tt>base</tt>.
   * @throws ArithmeticException if this LargeInteger is 0.
   */
  private LargeInteger getReciprocal(int precision) {
    if (precision >= 0x40000000) throw new OutOfMemoryError();
    if (precision <= 2) {
      // Compute an approximation using the first 3 bytes.
      long baseSq = base * base;
      long val = baseSq * baseSq * base / 
        (baseSq * getDigit(0) + base * getDigit(1) + getDigit(2));
      int[] u = new int[4];
      for (int i = 0; i < 4; i++) {
        u[i] = (int) (val % base);
        val = val / base;
      }
      int start = 2 - precision;
      int len = 2 + precision;
      while (len >= 0 && u[start + len - 1] == 0) len--;
      return new LargeInteger(u, start, len, 1, displayBase);
    } else {
      LargeInteger z = getReciprocal(precision / 2 + 1);
      LargeInteger t = trunc(precision + 2);
      LargeInteger prod = t.multiply(z).multiply(z);
      int diff = 2 * (precision / 2 + 2);
      LargeInteger prodTrunc = 
        new LargeInteger(prod.digits, prod.offset + diff, prod.length - diff, 
             1, displayBase);
      LargeInteger zPad = z.pad(precision - precision / 2 - 1);
      return zPad.add(zPad.subtract(prodTrunc));
    }
  }

  /**
   * Returns the p most significant digits of this LargeInteger. If
   * there are fewer than p digits, trailing 0s are added.
   */
  private LargeInteger trunc(int p) {
    return p <= length ?
      new LargeInteger(digits, offset + length - p, p, 1, displayBase) :
      pad(p - length);
  }

  /**
   * Returns this LargeInteger padded with p trailing 0s.
   */
  private LargeInteger pad(int p) {
    int[] bytes = new int[length + p];
    System.arraycopy(digits, offset, bytes, p, length);
    return new LargeInteger(bytes, 0, length + p, 1, displayBase);
  }

  /**
   * Returns the i-th most significant digit.
   */
  private int getDigit(int i) {
    return i >= length ? 0 :
      (digits[offset + length - i - 1]) & 0xffff;
  }

  /**
   * @return this LargeInteger raised to the power <var>other</var>.
   * Note that it is very easy to generate an OutOfMemoryError.
   * @throws  ArithmeticException  If <var>other</var> is negative.
   */
  public LargeInteger pow(LargeInteger other) throws ArithmeticException {
    if (other.negative < 0) throw new ArithmeticException();
    LargeInteger x = this;
    LargeInteger z = new LargeInteger("1", displayBase);
    for (int i = other.offset; i < other.offset + other.length; i++) {
      z = z.multiply(x.pow((other.digits[i]) & 0xffff));
      x = x.pow(base);
    }
    return z;
  }

  /**
   * @return this LargeInteger raised to the power <var>exp</var>.
   * @throws  ArithmeticException  If <var>exp</var> is negative.
   */
  @Override
  public LargeInteger pow(int exp) throws ArithmeticException {
    if (exp < 0) throw new ArithmeticException();
    if (exp == 0) return new LargeInteger("1", displayBase);
    LargeInteger z = pow(exp / 2);
    z = z.multiply(z);
    if ((exp & 1) == 1) return multiply(z);
    else return z;
  }

  /**
   * @return <var>a</var> raised to the power <var>exp</var>.
   * @throws  ArithmeticException  If <var>exp</var> is negative.
   */
  public static LargeInteger pow(int a, LargeInteger exp) 
    throws ArithmeticException {
    if (exp.negative < 0) throw new ArithmeticException();
    LargeInteger z = new LargeInteger("1", exp.displayBase);
    for (int i = exp.offset + exp.length - 1; i >= exp.offset; i--) {
      z = z.pow(exp.base).multiply(pow(a, (exp.digits[i]) & 0xffff, 
                       exp.displayBase));
    }
    return z;
  }

  /**
   * @return the LargeInteger <var>a<sup>exp</sup></var>, stored in base
   * <var>db</var>.
   * @throws ArithmeticException   If <var>exp</var> is negative.
   * @throws NumberFormatException If <var>db &lt; 2</var> or <var>db &gt; 36</var>
   */
  public static LargeInteger pow(int a, int exp, int db) 
    throws ArithmeticException {
    if (exp < 0) throw new ArithmeticException();
    if (exp == 0) return new LargeInteger("1", db);
    LargeInteger z = pow(a, exp / 2, db);
    z = z.multiply(z);
    if ((exp & 1) == 1) return z.multiply(a);
    else return z;
  }

  /**
   * @return the product of this LargeInteger and <var>a</var>.
   */
  public LargeInteger multiply(int a) {
    int[] bytes = new int[length + 8];
    int absa = Math.abs(a);
    long accum = 0;
    int j = 0;
    for (int i = offset; i < offset + length; i++, j++) {
      accum += (((long) digits[i]) & 0xffff) * absa;
      bytes[j] = (int) (accum % base);
      accum = accum / base;
    }
    for ( ; accum > 0; accum = accum / base, j++) {
      bytes[j] = (int) (accum % base);
    }
    return new LargeInteger(bytes, 0, getEnd(bytes), negative * a, displayBase);
  }

  /**
   * @return an array whose first element is <tt>this / a</tt> and whose
   * second element is <tt>this % a</tt>.
   * @throws ArithmeticException  If <tt>a = 0</tt>.
   */
  public LargeInteger[] divide(int a) {
    int[] bytes = new int[length];
    int absa = Math.abs(a);
    long accum = 0;
    int j = bytes.length - 1;
    for (int i = offset + length - 1; i >= offset; i--) {
      accum += digits[i] & 0xffff;
      bytes[j] = (int) (accum / absa);
      accum = (accum % absa) * base;
    }
    LargeInteger[] result = new LargeInteger[2];
    result[0] = new LargeInteger(bytes, 0, getEnd(bytes), negative * a, displayBase);
    if (negative < 0) accum = -accum;
    result[1] = new LargeInteger(Long.toString(accum, displayBase), 
                 displayBase);
    return result;
  }

  /**
   * Returns the String representation of this LargeInteger.  The format
   * is the same as that of {@link java.lang.Integer#toString(int)}, where the
   * given <tt>int</tt> is the radix associated with this LargeInteger.
   */
  @Override
  public String toString() {
      if(base != 0x10000){
          BigInteger m = BigIntToBigNbr(digits, length);
          return m.toString(displayBase);
      }
    int NL = length * 2;
    temp = new byte[NL];
    for (int i = 0; i < length; i++){
        int digit = digits[i];
        temp[NL - 1 - 2 * i] = (byte) (digit             & 0xFF);
        temp[NL - 2 - 2 * i] = (byte) (digit / 0x100     & 0xFF);
    }
    return new BigInteger(negative, temp).toString(displayBase);
  }

  public int getBase() {
    return displayBase;
  }
  
  /**
   * Compares this LargeInteger with the given Object.
   * @return a negative integer, zero, or a positive integer as this
   * LargeInteger is less than, equal to, or greater than the specified
   * object.
   * @throws ClassCastException if the specified object is not a LargeInteger.
   */
  public int compareTo(LargeInteger other) 
  {
    if (negative < 0) return -negate().compareTo(other.negate());
    if (other.negative < 0) return 1;
    if (length > other.length) return 1;
    if (length < other.length) return -1;
    int i = offset + length - 1, j = other.offset + other.length - 1;
    while (i >= offset && digits[i] == other.digits[j]){
        i--; j--;
    }
    if (i < offset) return 0;
    if (((digits[i]) & 0xffff) > ((other.digits[j]) & 0xffff)) 
      return 1;
    return -1;
  }
  
  public LargeInteger and(LargeInteger other){
      int len;
      if(length < other.length){
          len = length;
      } else {
          len = other.length;
      }
      int[] result = new int[len];
      for(int i = 0; i < len; i++){
        result[i] = digits[i + offset] & other.digits[i + other.offset];
      }
      return new LargeInteger(result, 0, getEnd(result), 1, displayBase);
  }
  
  @Override
  public LargeInteger shiftLeft(int n){
      int rem = n % 16;
      int len = n / 16;
      int[] result = new int[length + len + 1];
      for(int i = length - 1; i >= 0; i--){
          result[i + len] = (digits[i + offset] << rem) & 0xffff;
          result[i + len + 1] |= digits[i + offset] >> (16 - rem);
      }
      return new LargeInteger(result, 0, getEnd(result), 1, displayBase);
  }
  
  @Override
  public LargeInteger shiftRight(int n){
      if(base != 0x10000) return new LargeInteger(super.shiftRight(n).toString());
      if(n < 0) return shiftLeft(Math.abs(n));
      int rem = n % 16;
      int len = n / 16;
      if(length <= len) return new LargeInteger(digits, offset, 0, 0, displayBase);
      int[] result = new int[length - len];
      result[0] = digits[offset + len] >> rem;
      for(int i = 1; i < length - len; i++){
          result[i] = digits[i + offset + len] >> rem;
          result[i - 1] |= (digits[i + offset + len] << (16 - rem)) & 0xffff;
      }
      return new LargeInteger(result, 0, getEnd(result), 1, displayBase);
  }
  
  // p here is the same p as in 2^p-1
    public static boolean isPrime(int prime){
        //JProgressBar progressBar = new JProgressBar(0, prime - 2);
        //newContentPane.add(progressBar);
        //progressBar.setStringPainted(true);
        //progressBar.setString("" + prime);
        //progressBar.setValue(0);
        LargeInteger s = new LargeInteger("4");
        LargeInteger m = new LargeInteger("1").shiftLeft(prime).subtract(new LargeInteger("1")); // = (1 << p) - 1
        for (int i = 1; i <= prime - 2; i++) {
            // This is the slower but straightforward way
            // s.Mod(s.Sub(s.Mul(s, s), two), m)
            // Or, use this faster way
            // s here is the same as k in this part of the wikipedia page:
            // https://en.wikipedia.org/wiki/Lucas%E2%80%93Lehmer_primality_test#Time_complexity
            s = s.squareNTT().subtract(new LargeInteger("2"));
            while (s.compareTo(m) == 1) {
                // And is big's logical and, Rsh is right shift
                LargeInteger x = s.shiftRight(prime);
                LargeInteger y = s.and(m);
                s = y.add(x);
            }
            //progressBar.setValue(i);
            if (s.compareTo(m) == 0) {
                //progressBar.setForeground(Color.GREEN);
                //progressBar.setString("p = " + p + " is prime");
                return true;
            }
        }
        boolean ret = s.compareTo(new LargeInteger("0")) == 0;
        if(ret){
            //progressBar.setForeground(Color.GREEN);
            //progressBar.setString("p = " + p + " is prime");
        } else {
            //progressBar.setForeground(Color.LIGHT_GRAY);
        }
        return ret;
    }
}
