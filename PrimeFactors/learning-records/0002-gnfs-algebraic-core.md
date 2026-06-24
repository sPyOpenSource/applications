# Learning Record 0002: Algebraic Core of GNFS (Side Topic)

## Date
2026-06-22

## What was covered
- Lesson 2 as a side topic: the algebraic machinery that lifts the
  congruence-of-squares idea from QS (working in ℤ) to GNFS (working in ℤ[α])
- The three-part algebraic setup: irreducible polynomial f(x) with root α,
  integer m with f(m) ≡ 0 (mod n), and the evaluation homomorphism
  φ: ℤ[α] → ℤ_n sending α → m
- The norm formula N(a − bα) = b^d · f(a/b) as the bridge between the
  algebraic and rational sides
- Two-sided smoothness: a − bm (rational) and N(a − bα) (algebraic) must
  both be B-smooth
- The congruence of squares emerges from φ(β)² ≡ A² (mod n) when both sides
  are simultaneously squares
- Why GNFS beats QS asymptotically: smoothness candidates are ≈ n^{1/d}
  instead of ≈ √n

## Key insights
1. The norm formula N(a − bα) = b^d f(a/b) is the entire reason GNFS can
   find smaller smoothness candidates — the values shrink from O(√n) to
   O(n^{1/d}).
2. The evaluation homomorphism φ is well-defined precisely because
   f(m) ≡ 0 (mod n) — the same polynomial relation holds in both rings.
3. The two-sided smoothness problem is the conceptual core: both rational
   and algebraic sides need to produce squares simultaneously.
4. The algebraic square root problem (finding β ∈ ℤ[α]) is the hardest
   step in practice, requiring prime ideal factorization and CRT techniques.

## What's different from QS
- QS: one polynomial g(x) = x² − n, one side (ℤ), smoothness tested on ℤ
- GNFS: two polynomials (f(x) and x − m), two sides (ℤ[α] and ℤ),
  smoothness tested simultaneously on both
- QS: values tested are O(√n), bound by the size of n
- GNFS: values tested are O(n^{1/d}), tunable by choosing polynomial degree

## User background confirmed
- Strong abstract algebra made the ℤ[α], homomorphism, and norm discussion
  natural — no need to build up to rings of integers from scratch

## Zone of proximal development
Potential follow-ups (if user wants to continue the GNFS side topic):
- Polynomial selection methods (base-m, Montgomery-Murphy, Kleinjung)
- Sieving in GNFS: lattice sieving vs line sieving
- The algebraic square root step in detail
- Prime ideal factorization and the "factor base" in O_K

## Open questions
- How exactly is the polynomial f(x) chosen in practice? (Briefly touched on
  but not detailed.)
- The ideal factorization step: how does the norm's prime factorization map
  to prime ideal factorization in O_K?
