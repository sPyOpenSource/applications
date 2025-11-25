
package utilities;

/**
 *
 * @author xuyi
 */
public class FFT {
    /** The modulus for all of the FFT calculations.
     *  This is a 47-bit prime, 65550 * 2<sup>30</sup> + 1.
     */
    public static final long mod = 70383776563201L;

    /** Element i is a principal 2**i-th root of unity in the FFT field.
     */
    public static final long[] roots = new long[] {
1L, 70383776563200L, 53584494145874L, 16491100154340L,  8251729855569L, 
    54141260506933L, 30827116249773L, 11841623934447L, 38938666398922L, 
    40119572768059L, 13382088273171L,  7259789800794L,   245247012133L, 
    48959122674900L,  2057783853341L, 24137967250171L, 17560868529348L, 
    54236099443817L, 31613430409055L, 28568927026433L, 11496597192615L, 
    33252378055270L, 23600998947234L, 22435795378610L, 52431065818252L, 
    30551680809588L, 26713297315242L, 22918355618248L, 15869278321465L, 
     7423219505696L, 31696988370702L};

    /** Element i is the multiplicative inverse of roots[i] in the FFT field.
     */
    public static final long[] rootsInv = new long[] {
    1L, 70383776563200L, 16799282417327L, 61472623878972L, 39229782493121L, 
    50936372324210L, 9362651625339L, 10452472889196L, 9025625771819L, 
    31859598355635L, 39040032295658L, 65913589151130L, 24822682898295L, 
    23045662539716L, 59786918773100L, 39400950333183L, 42896716809001L, 
    4828709384135L, 15241522851548L, 25994160944760L, 41535456258163L, 
    41075902423707L, 19684530485833L, 32566673512488L, 48139603138128L, 
    55660679838838L, 416502740110L, 62354506855284L, 16247553611790L, 
    70223437700930L, 32091152699528L};

    /** Element i is the multiplicative inverse of 2**i in the FFT field.
     */
    public static final long[] lengthInv = new long[] {
    1L, 35191888281601L, 52787832422401L, 61585804492801L, 65984790528001L, 
    68184283545601L, 69284030054401L, 69833903308801L, 70108839936001L, 
    70246308249601L, 70315042406401L, 70349409484801L, 70366593024001L, 
    70375184793601L, 70379480678401L, 70381628620801L, 70382702592001L, 
    70383239577601L, 70383508070401L, 70383642316801L, 70383709440001L, 
    70383743001601L, 70383759782401L, 70383768172801L, 70383772368001L, 
    70383774465601L, 70383775514401L, 70383776038801L, 70383776301001L, 
    70383776432101L, 70383776497651L};
  
    /** This method may be used to compute an FFT or an inverse FFT.
     *  It uses modmult to compute products in the underlying ring.
     *  The first parameter must contain n elements, where n is a power
     *  of two.  
     *  <p>
     *  Computing an FFT:
     *  <p>
     *  The first parameter gives the coefficients of a polynomial
     *  in bit-reverse order.  The second parameter must be an array 
     *  of at least <tt>lg n</tt> elements such that element i is a 
     *  principal 2**i-th root of unity. The second parameter will be
     *  modified to contain the values of the polynomial at successive
     *  powers of the principal n-th root of unity.
     *  <p>
     *  Computing an inverse FFT:
     *  <p>
     *  The first parameter gives the values of the polynomial at successive
     *  powers of a principal n-th root of unity, given in bit-reverse order.
     *  The second parameter must contain the multiplicative inverses of the 
     *  roots used in the FFT computation. The second parameter will be
     *  modified to contain values which, when multiplied by the inverse
     *  of n, are the coefficients of the polynomial from low-order term to
     *  high-order term.
     *  <p>
     *  The code is based on the algorithm ITERATIVE-FFT given on p. 794 of
     *  "Introduction to Algorithm", by Cormen, Leiserson, and Rivest
     *  (McGraw Hill, 1990).
     */
    public static void transform(long[] a, long[] ws) {
        for (int m = 1, s = 1; m < a.length; m <<= 1, s++) {
          long w = 1L;
          for (int j = 0; j < m; j++) {
            for (int k = j; k < a.length; k += (m << 1)) {
              long t = modmult(w, a[k + m], mod);
              long u = a[k];
              long sum = u + t;
              a[k] = sum >= mod ? sum - mod : sum;
              long diff = u - t;
              a[k + m] = diff < 0L ? diff + mod : diff;
            }
            w = modmult(w, ws[s], mod);
          }
        }
    }
  
    public static void inverseTransform(long[] a, int lgn){
        transform(a, rootsInv);
        for (int i = 0; i < a.length; i++) {
          a[i] = modmult(a[i], lengthInv[lgn], mod);
        }
    }

    /** Computes the product of its parameters mod 70383776563201.
     *  The parameters must each be nonnegative and contain at most
     *  47 significant bits.
     */
    public static long modmult(long a, long b, long mod) {
        long loa = a & 0xffffL;  // The low-order 16 bits.
        long lob = b & 0xffffL;  //
        long hia = (a & 0xffffffffffff0000L) >>> 16;  // The high-order 31 bits.
        long hib = (b & 0xffffffffffff0000L) >>> 16;  //
        long lo = loa * lob;  // 32 bits.
        long mid = loa * hib + lob * hia;  // 48 bits.
        lo += (mid << 16) & 0xffffffffL;   // 33 bits.
        long hi = hia * hib + (mid >>> 16) + (lo >>> 32); // 63 bits.
        lo = lo & 0xffffffffL;  // 32 bits.
        long result = hi % mod;  // 47 bits.
        result = ((result << 16) | (lo >>> 16)) % mod;  // 47 bits.
        result = ((result << 16) | (lo & 0xffffL)) % mod;
        return result;
    }
}
