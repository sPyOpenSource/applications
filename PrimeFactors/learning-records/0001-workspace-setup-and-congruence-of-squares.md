# Learning Record 0001: Workspace Setup and Congruence of Squares

## Date
2026-06-22

## What was covered
- Teaching workspace initialised (MISSION.md, RESOURCES.md, assets/style.css,
  reference/glossary.html)
- Lesson 1: The Congruence of Squares — the algebraic foundation that
  x² ≡ y² (mod n) with x ≠ ±y yields a factor via gcd(x − y, n)
- Worked example: n = 1649 → 17 × 97
- Mapped the four steps (factor base → collect relations → linear algebra →
  extract factor) to the QuadraticSieve.java code
- Observed that ECM shares the same goal (congruence of squares) but achieves
  it through elliptic curve group operations

## Key insights
1. The core insight is that a *product* of non-square residues can be a
   square — not that any individual residue must be one. This is the engine
   behind all modern factoring.
2. The factor base includes only primes where n is a quadratic residue
   (Legendre symbol = 1). Non-residue primes never divide g(x) and are
   excluded (line 281 of QuadraticSieve.java).
3. Gaussian elimination over 𝔽₂ is guaranteed to find a dependency when the
   number of relations exceeds the size of the factor base.
4. The QS uses log-approximation sieving (adding log(p) instead of dividing
   by p) as a fast filter for smoothness — only promising candidates are
   checked properly.

## User background confirmed
- Strong abstract algebra (groups, rings, fields, finite fields)
- High-level understanding of ECM and QS, first detailed look at the
  underlying algebra and its code-level representation

## Zone of proximal development
Next logical topics (in order of dependency):
- Lesson 2: Legendre symbol and factor-base construction — why
  (n/p) = 1 is required, how the Tonelli–Shanks algorithm finds the
  sieving offsets, the trade-off in choosing B_SMOOTH
- Lesson 3: The sieving wheel — how the Wheel class tracks arithmetic
  progressions for each prime and accumulates log contributions
- Lesson 4: The linear algebra step — Gaussian elimination over 𝔽₂ with
  BitSets, handling big primes via VectorsShrinker

## Open questions / unclear points
- The SIQS polynomial re-initialisation strategy is not yet examined
- The big prime variation (VectorsShrinker) is understood at a high level
  but would benefit from a dedicated lesson
- ECM group law and its connection to congruence of squares still to cover
