# Resources

## Primary Sources

### Books

- **Crandall, R. & Pomerance, C. (2005).** *Prime Numbers: A Computational
  Perspective* (2nd ed.). Springer.
  The canonical reference. Chapters 5–6 cover the quadratic sieve and ECM in
  full mathematical detail.

- **Bressoud, D. M. (1989).** *Factorization and Primality Testing*.
  Springer (Undergraduate Texts in Mathematics).
  Chapter 8: The Quadratic Sieve. Excellent for building intuition before the
  heavy analysis.
  https://ndl.ethernet.edu.et/bitstream/123456789/23097/1/David%20M.%20Bressoud.pdf

- **Wagstaff, S. S. Jr. (2013).** *The Joy of Factoring*. American
  Mathematical Society (Student Mathematical Library, Vol. 68).
  Clear, accessible treatment of both QS and ECM with historical context.

- **Riesel, H. (1994).** *Prime Numbers and Computer Methods for
  Factorization* (2nd ed.). Birkhäuser.
  Classic reference with detailed algorithm descriptions.

### Theses & Papers

- **Pomerance, C. (1981).** The Quadratic Sieve Factoring Algorithm.
  *Advances in Cryptology: Proceedings of EUROCRYPT 84*, pp. 169–182.
  The original QS paper.

- **Contini, S. P. (1997).** *Factoring Integers with the
  Self-Initializing Quadratic Sieve*. M.A. thesis, University of Georgia.
  https://web.archive.org/web/20150405063620/http://www.crypto-world.com/documents/contini_siqs.pdf
  The definitive reference for SIQS — the variant used in this codebase.

- **Lenstra, H. W. Jr. (1987).** Factoring Integers with Elliptic Curves.
  *Annals of Mathematics*, 126(3), 649–673.
  The original ECM paper.

- **Zimmermann, P. & Dodson, B. (2006).** 20 Years of ECM.
  *Proceedings of the 7th Algorithmic Number Theory Symposium*.
  Comprehensive survey of ECM improvements (stage 2, parameter choices).

### Wikipedia Articles

- [Congruence of squares](https://en.wikipedia.org/wiki/Congruence_of_squares)
- [Quadratic sieve](https://en.wikipedia.org/wiki/Quadratic_sieve)
- [Lenstra elliptic-curve factorization](https://en.wikipedia.org/wiki/Lenstra_elliptic-curve_factorization)
- [Tonelli–Shanks algorithm](https://en.wikipedia.org/wiki/Tonelli%E2%80%93Shanks_algorithm)
  (used in this codebase for solving x² ≡ n (mod p))
