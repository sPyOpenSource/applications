
package utilities;

/** This class implements a counter in which the bits are reversed.
 *  It is constructed with a mask, which should contain a single
 *  1 bit.  This bit marks the position at which the value should be
 *  "incremented", with carries propogating to the right.  For example,
 *  <pre>
 *  new BitReverseCounter(4);
 *  </pre>
 *  constructs a counter which produces the sequence (shown in binary):
 *  <pre>
 *  000
 *  100
 *  010
 *  110
 *  001
 *  101
 *  011
 *  111
 *  </pre>
 *  The next value is returned in constant amortized time.
 */
public class BitReverseCounter {

  /** The mask for this counter.
   */
  private final int mask;

  /** The next value to be returned.
   */
  private int count;

  /** Constructs a new BitReverseCounter with the given mask.
   */
  public BitReverseCounter(int m) {
    mask = m;
  }

  /** @return the next value.
   */
  public int next() {
    int ret = count;
    count = count ^ mask;
    int m = mask;
    while (m != 0 && (count & m) == 0) {
      m = m >>> 1;
      count = count ^ m;
    }
    return ret;
  }

  /** Resets the counter to 0.
   */
  public void reset() {
    count = 0;
  }
}
