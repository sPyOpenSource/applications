Prime Factorization: ECM and SIQS

ECM stands for Elliptic Curve Method, which is a fast algorithm that employs elliptic curves to find small factors of a large number.
https://en.wikipedia.org/wiki/Lenstra_elliptic-curve_factorization 

It works by picking a random elliptic curve and a point on it, and then performing repeated additions of the point to itself until 
a division by zero occurs. This indicates that the curve is not smooth modulo some factor of the number, which can then be found 
by computing the greatest common divisor.

SIQS stands for Self-Initializing Quadratic Sieve, which is a variant of the quadratic sieve algorithm that uses polynomials with 
a common root modulo the number to be factored.

It works by generating a large number of smooth relations, which are pairs of numbers whose difference is a perfect square and whose 
product is smooth (has only small prime factors). 
These relations are then combined using linear algebra to find a congruence of squares, which leads to a non-trivial factor of the number.
https://www.rieselprime.de/ziki/Factorization