# Mission: Understand the Mathematics of Prime Factorization Algorithms

## Why
I want to understand how the Elliptic Curve Method (ECM) and
Self-Initializing Quadratic Sieve (SIQS) actually work at the
mathematical level — not just the high-level story, but the
concrete algebra and number theory that makes them tick.

## What success looks like
- I can explain how a congruence of squares is found by each
  algorithm, in my own words
- I can trace through the key mathematical steps in the source
  code and map them to the theoretical description
- I can reason about why certain parameters (smoothness bounds,
  factor base sizes, curve counts) are chosen as they are
- I could, in principle, re-implement a simplified version from
  the math alone

## Constraints
- Keep lessons tightly focused — one concept at a time
- Assume I'm comfortable with abstract algebra (groups, rings,
  fields, finite fields) and basic number theory
- Don't skip the algebra I can handle; do spell out the parts
  that are easy to get wrong (smoothness probability, matrix
  dependencies, group law edge cases)
- Reference external high-trust sources so I can follow up
