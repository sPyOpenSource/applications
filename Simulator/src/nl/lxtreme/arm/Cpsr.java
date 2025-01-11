
package nl.lxtreme.arm;

/**
 *
 * @author xuyi
 */
public 
  /**
   * Denotes the "Current Program Status Register" (CPSR).
   */
  class Cpsr
  {
    // VARIABLES

    boolean n; // 31
    boolean z; // 30
    boolean c; // 29
    boolean v; // 28
    boolean q; // 27

    int it; // 26, 25 & 15..10

    boolean j; // 24

    // padding 23..20

    int ge; // 19..16

    boolean E; // 9
    boolean A; // 8
    boolean I; // 7
    boolean F; // 6
    boolean t; // 5

    // 5-bits mode

    int mode; // 4..0

    // METHODS

    /**
     * Returns the value of the CPSR as 32-bit value.
     * 
     * @return a 32-bit value representation of the CPSR.
     */
    int getValue()
    {
      long result = 0;
      result |= (this.n ? (1L << 31) : 0);
      result |= (this.z ? (1L << 30) : 0);
      result |= (this.c ? (1L << 29) : 0);
      result |= (this.v ? (1L << 28) : 0);
      result |= (this.q ? (1L << 27) : 0);
      result |= ((this.it & 0x03) << 25);
      result |= (this.j ? (1L << 24) : 0);
      result |= ((this.ge & 0x0F) << 16);
      result |= ((this.it & 0xFC) << 8);
      result |= (this.E ? (1L << 9) : 0);
      result |= (this.A ? (1L << 8) : 0);
      result |= (this.I ? (1L << 7) : 0);
      result |= (this.F ? (1L << 6) : 0);
      result |= (this.t ? (1L << 5) : 0);
      result |= (this.mode & 0x1F);
      return (int) (result & 0xFFFFFFFF);
    }

    /**
     * Sets this CPSR by means of a 32-bit value.
     * 
     * @param aValue
     *          the 32-bit representation of the CPSR to set.
     */
    void setValue(int aValue)
    {
      long value = aValue;
      this.n = (value & (1L << 31)) != 0;
      this.z = (value & (1L << 30)) != 0;
      this.c = (value & (1L << 29)) != 0;
      this.v = (value & (1L << 28)) != 0;
      this.q = (value & (1L << 27)) != 0;
      this.it = (int) (((value >> 8) & 0xFC) | ((value >> 25) & 0x03));
      this.j = (value & (1L << 24)) != 0;
      this.ge = (int) ((value >> 16) & 0x0F);
      this.E = (value & (1L << 9)) != 0;
      this.A = (value & (1L << 8)) != 0;
      this.I = (value & (1L << 7)) != 0;
      this.F = (value & (1L << 6)) != 0;
      this.t = (value & (1L << 5)) != 0;
      this.mode = (int) (value & 0x1f);
    }
  }
