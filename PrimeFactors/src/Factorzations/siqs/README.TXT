SIQS factorization (optimization of Dixon's method: x close to the square root of N such that x^2 module N is small)

Multithread procedure:                                               
   1) Main thread generates factor base and other parameters.           
   2) Start M threads where the number M is specified by the user.                                           
   3) For each polynomial:                                              
      3a) Main thread generates the data for the set of 2^n polynomials.
      3b) Each child thread computes a range of polynomials             
          (u*2^n/M to (u+1)*2^n/M exclusive).
          
Partial and full relation routines must be synchronized.